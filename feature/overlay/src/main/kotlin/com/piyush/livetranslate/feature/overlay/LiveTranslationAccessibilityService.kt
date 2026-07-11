package com.piyush.livetranslate.feature.overlay

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.piyush.livetranslate.core.model.TranslationOrigin
import com.piyush.livetranslate.core.model.TranslationRequest
import com.piyush.livetranslate.core.model.UserSettings
import com.piyush.livetranslate.domain.repository.SettingsRepository
import com.piyush.livetranslate.domain.usecase.TranslateTextUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class LiveTranslationAccessibilityService : AccessibilityService() {
    @Inject lateinit var translateText: TranslateTextUseCase
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var runtime: OverlayRuntimeController

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val ocr = OcrFallbackEngine()
    private lateinit var overlay: TranslationOverlayWindow
    private var settings = UserSettings()
    private var paused = false
    private var scanJob: Job? = null
    private var translateJob: Job? = null
    private var pending: DetectedText? = null
    private var lastPackage: String? = null
    private var lastVisible = emptySet<String>()
    private var lastQueued = ""
    private var lastOcrText = ""
    private var lastOcrAt = 0L
    private var lastTranslationAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        overlay = TranslationOverlayWindow(
            this,
            onPause = ::togglePause,
            onOcr = { requestOcr(force = true) },
            onStop = { disableSelf() },
        ).also { it.attach() }
        runtime.update { it.copy(serviceConnected = true, paused = false, message = "Accessibility service connected. Enable consent to begin.") }

        serviceScope.launch {
            settingsRepository.settings.collectLatest { current ->
                settings = current
                if (!current.overlayConsentGranted) {
                    overlay.showMessage("Open LiveTranslate Pro to review and allow screen translation")
                    runtime.update { it.copy(message = "Consent is required before screen text is read.", mode = DetectionMode.IDLE) }
                } else if (!paused) {
                    overlay.showMessage("Watching for new visible text")
                    runtime.update { it.copy(message = "Watching other apps for new visible text.") }
                    scheduleScan(lastPackage, immediate = true)
                }
            }
        }
        serviceScope.launch {
            runtime.commands.collect { command ->
                when (command) {
                    OverlayCommand.TogglePause -> togglePause()
                    OverlayCommand.ScanWithOcr -> requestOcr(force = true)
                    OverlayCommand.StopService -> disableSelf()
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (!settings.overlayConsentGranted || paused) return
        val eventPackage = event.packageName?.toString()?.takeIf(String::isNotBlank) ?: return
        if (shouldIgnore(eventPackage)) return
        if (eventPackage != lastPackage || event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            lastPackage = eventPackage
            lastVisible = emptySet()
            lastQueued = ""
        }
        runtime.update { it.copy(activePackage = eventPackage) }
        val immediate = event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
        scheduleScan(eventPackage, immediate)
    }

    private fun scheduleScan(expectedPackage: String?, immediate: Boolean) {
        if (expectedPackage == null || shouldIgnore(expectedPackage)) return
        scanJob?.cancel()
        scanJob = serviceScope.launch {
            delay(if (immediate) 180 else 420)
            if (!settings.overlayConsentGranted || paused || expectedPackage != lastPackage) return@launch
            val root = rootInActiveWindow
            if (root == null) {
                requestOcr(force = false)
                return@launch
            }
            val snapshot = try { ScreenTextExtractor.extract(root) } finally {
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT < 33) root.recycle()
            }
            if (snapshot.containsSensitiveField) {
                overlay.showMessage("Paused on a screen containing a protected input field")
                runtime.update { it.copy(mode = DetectionMode.IDLE, message = "Protected input detected; this screen is not translated.") }
                return@launch
            }
            val current = snapshot.texts
            val newlyVisible = if (lastVisible.isEmpty()) current else current.filterTo(linkedSetOf()) { it !in lastVisible }
            lastVisible = current
            if (newlyVisible.isEmpty()) {
                if (current.isEmpty()) requestOcr(force = false)
                return@launch
            }
            val candidate = newlyVisible.toList().takeLast(8).joinToString("\n").take(1_500)
            submit(candidate, DetectionMode.ACCESSIBILITY)
        }
    }

    private fun submit(text: String, mode: DetectionMode) {
        val cleaned = text.trim()
        if (cleaned.length < 2 || cleaned == lastQueued) return
        lastQueued = cleaned
        pending = DetectedText(cleaned, mode)
        if (translateJob?.isActive == true) return
        translateJob = serviceScope.launch {
            while (pending != null && isActive) {
                val item = pending ?: break
                pending = null
                val wait = 850L - (System.currentTimeMillis() - lastTranslationAt)
                if (wait > 0) delay(wait)
                runtime.update { it.copy(translating = true, mode = item.mode, sourcePreview = item.text.take(180), message = "Translating new screen text…") }
                val request = TranslationRequest(
                    text = item.text,
                    sourceLanguage = "auto",
                    targetLanguage = settings.overlayTargetLanguage,
                    origin = if (item.mode == DetectionMode.OCR) TranslationOrigin.SCREEN_OCR else TranslationOrigin.SCREEN_ACCESSIBILITY,
                    saveToHistory = settings.overlaySaveHistory,
                )
                translateText(request).fold(
                    onSuccess = { result ->
                        lastTranslationAt = System.currentTimeMillis()
                        overlay.showTranslation(item.text, result.translatedText, item.mode)
                        runtime.update {
                            it.copy(
                                translating = false, mode = item.mode, sourcePreview = item.text.take(180),
                                translation = result.translatedText, message = if (item.mode == DetectionMode.OCR) "OCR fallback" else "Accessibility text",
                                translatedAt = lastTranslationAt,
                            )
                        }
                    },
                    onFailure = { error ->
                        overlay.showMessage(error.message ?: "Translation unavailable")
                        runtime.update { it.copy(translating = false, message = error.message ?: "Translation unavailable") }
                    },
                )
            }
        }
    }

    private fun requestOcr(force: Boolean) {
        if (!settings.overlayConsentGranted || paused) return
        if (!settings.overlayOcrFallbackEnabled && !force) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (force) overlay.showMessage("OCR screen fallback requires Android 11 or newer")
            return
        }
        val now = System.currentTimeMillis()
        if (!force && now - lastOcrAt < 3_000) return
        lastOcrAt = now
        overlay.setCaptureVisible(false)
        serviceScope.launch {
            delay(120)
            takeScreenshot(Display.DEFAULT_DISPLAY, ContextCompat.getMainExecutor(this@LiveTranslationAccessibilityService), object : TakeScreenshotCallback {
                override fun onSuccess(result: ScreenshotResult) {
                    val hardwareBitmap = Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace)
                    val bitmap = hardwareBitmap?.copy(Bitmap.Config.ARGB_8888, false)
                    result.hardwareBuffer.close()
                    overlay.setCaptureVisible(true)
                    if (bitmap == null) {
                        overlay.showMessage("OCR could not read this screen")
                        return
                    }
                    serviceScope.launch {
                        try {
                            val text = ocr.recognize(bitmap).trim()
                            if (text.isNotBlank() && text != lastOcrText) {
                                lastOcrText = text
                                submit(text, DetectionMode.OCR)
                            } else if (force) overlay.showMessage("No new readable text found with OCR")
                        } catch (error: Exception) {
                            overlay.showMessage(error.message ?: "OCR failed")
                        } finally { bitmap.recycle() }
                    }
                }

                override fun onFailure(errorCode: Int) {
                    overlay.setCaptureVisible(true)
                    val message = if (errorCode == ERROR_TAKE_SCREENSHOT_SECURE_WINDOW) "Protected screens cannot be captured" else "OCR screenshot unavailable"
                    if (force) overlay.showMessage(message)
                    runtime.update { it.copy(message = message) }
                }
            })
        }
    }

    private fun togglePause() {
        paused = !paused
        if (paused) { scanJob?.cancel(); pending = null }
        overlay.setPaused(paused)
        runtime.update { it.copy(paused = paused, translating = false, mode = DetectionMode.IDLE, message = if (paused) "Screen translation paused." else "Watching for new visible text.") }
        if (!paused) { lastVisible = emptySet(); scheduleScan(lastPackage, immediate = true) }
    }

    private fun shouldIgnore(packageName: String): Boolean = packageName == this.packageName || packageName in PRIVATE_PACKAGES

    override fun onInterrupt() {
        if (!paused) togglePause()
    }

    override fun onDestroy() {
        scanJob?.cancel(); translateJob?.cancel(); serviceScope.cancel(); ocr.close()
        if (::overlay.isInitialized) overlay.detach()
        runtime.disconnected()
        super.onDestroy()
    }

    private data class DetectedText(val text: String, val mode: DetectionMode)

    private companion object {
        val PRIVATE_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.settings",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller",
            "com.google.android.inputmethod.latin",
            "com.samsung.android.honeyboard",
        )
    }
}
