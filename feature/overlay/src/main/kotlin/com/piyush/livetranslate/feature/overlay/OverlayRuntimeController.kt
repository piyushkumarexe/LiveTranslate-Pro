package com.piyush.livetranslate.feature.overlay

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

enum class DetectionMode { IDLE, ACCESSIBILITY, OCR }
sealed interface OverlayCommand {
    data object TogglePause : OverlayCommand
    data object ScanWithOcr : OverlayCommand
    data object StopService : OverlayCommand
}

data class OverlayRuntimeStatus(
    val serviceConnected: Boolean = false,
    val paused: Boolean = false,
    val translating: Boolean = false,
    val mode: DetectionMode = DetectionMode.IDLE,
    val activePackage: String? = null,
    val sourcePreview: String = "",
    val translation: String = "",
    val message: String = "Enable the accessibility service to start.",
    val translatedAt: Long? = null,
)

@Singleton
class OverlayRuntimeController @Inject constructor() {
    private val _status = MutableStateFlow(OverlayRuntimeStatus())
    val status = _status.asStateFlow()
    private val _commands = MutableSharedFlow<OverlayCommand>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val commands = _commands.asSharedFlow()

    fun command(command: OverlayCommand) { _commands.tryEmit(command) }
    fun update(block: (OverlayRuntimeStatus) -> OverlayRuntimeStatus) { _status.update(block) }
    fun disconnected() { _status.value = OverlayRuntimeStatus(message = "Accessibility service is off.") }
}

fun isOverlayAccessibilityEnabled(context: Context): Boolean {
    val manager = context.getSystemService(AccessibilityManager::class.java)
    return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any { info ->
        info.resolveInfo.serviceInfo.packageName == context.packageName &&
            info.resolveInfo.serviceInfo.name == LiveTranslationAccessibilityService::class.java.name
    }
}
