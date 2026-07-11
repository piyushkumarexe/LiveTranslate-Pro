package com.piyush.livetranslate.feature.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

internal class TranslationOverlayWindow(
    private val context: Context,
    private val onPause: () -> Unit,
    private val onOcr: () -> Unit,
    private val onStop: () -> Unit,
) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val root = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private val panel = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(13), dp(16), dp(12))
        background = rounded(0xF21A1A22.toInt(), 22f)
        visibility = View.GONE
        elevation = dp(12).toFloat()
    }
    private val status = label("Live screen translation", 12f, 0xFFB8B7C8.toInt())
    private val source = label("Waiting for visible text…", 12f, 0xFFB8B7C8.toInt())
    private val translated = label("", 18f, Color.WHITE)
    private val pause = action("Pause", onPause)
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.END
        x = dp(10)
        y = dp(160)
    }
    private var attached = false

    init {
        val controls = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            addView(pause)
            addView(action("Scan", onOcr))
            addView(action("Stop", onStop))
        }
        panel.addView(status, matchWrap())
        panel.addView(source, matchWrap(top = 7))
        panel.addView(translated, matchWrap(top = 5))
        panel.addView(controls, matchWrap(top = 9))
        val bubble = TextView(context).apply {
            text = "文"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(0xFF625BDB.toInt(), 30f)
            elevation = dp(14).toFloat()
            contentDescription = "LiveTranslate overlay. Tap to expand, drag to move."
            setOnTouchListener(DragTouchListener { panel.visibility = if (panel.visibility == View.VISIBLE) View.GONE else View.VISIBLE })
        }
        root.addView(panel, LinearLayout.LayoutParams(dp(296), WindowManager.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(8) })
        root.addView(bubble, LinearLayout.LayoutParams(dp(58), dp(58)))
    }

    fun attach() {
        if (attached) return
        windowManager.addView(root, params)
        attached = true
    }

    fun showTranslation(sourceText: String, translatedText: String, mode: DetectionMode) {
        status.text = if (mode == DetectionMode.OCR) "Translated with OCR fallback" else "Translated from accessible text"
        source.text = sourceText.take(180)
        translated.text = translatedText.take(800)
        panel.visibility = View.VISIBLE
    }

    fun showMessage(message: String) {
        status.text = message
        source.text = ""
        translated.text = ""
    }

    fun setPaused(value: Boolean) {
        pause.text = if (value) "Resume" else "Pause"
        status.text = if (value) "Screen translation paused" else "Watching for new text"
    }

    fun setCaptureVisible(visible: Boolean) { root.visibility = if (visible) View.VISIBLE else View.INVISIBLE }

    fun detach() {
        if (!attached) return
        runCatching { windowManager.removeView(root) }
        attached = false
    }

    private fun label(value: String, size: Float, color: Int) = TextView(context).apply {
        text = value; textSize = size; setTextColor(color); setLineSpacing(0f, 1.08f)
    }
    private fun action(value: String, click: () -> Unit) = TextView(context).apply {
        text = value; textSize = 13f; setTextColor(0xFFCBC8FF.toInt()); gravity = Gravity.CENTER
        setPadding(dp(12), dp(8), dp(12), dp(8)); setOnClickListener { click() }
    }
    private fun rounded(color: Int, radiusDp: Float) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radiusDp.toInt()).toFloat() }
    private fun matchWrap(top: Int = 0) = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(top) }
    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()

    private inner class DragTouchListener(private val click: () -> Unit) : View.OnTouchListener {
        private var downRawX = 0f; private var downRawY = 0f; private var startX = 0; private var startY = 0
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downRawX = event.rawX; downRawY = event.rawY; startX = params.x; startY = params.y; return true }
                MotionEvent.ACTION_MOVE -> {
                    params.x = (startX - (event.rawX - downRawX)).toInt().coerceAtLeast(0)
                    params.y = (startY + (event.rawY - downRawY)).toInt().coerceAtLeast(0)
                    if (attached) windowManager.updateViewLayout(root, params)
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (abs(event.rawX - downRawX) < dp(6) && abs(event.rawY - downRawY) < dp(6)) click()
                    return true
                }
            }
            return false
        }
    }
}
