package com.piyush.livetranslate.feature.overlay

import android.view.accessibility.AccessibilityNodeInfo

internal data class ScreenTextSnapshot(val texts: LinkedHashSet<String>, val containsSensitiveField: Boolean)

internal object ScreenTextExtractor {
    private const val MAX_NODES = 700
    private const val MAX_DEPTH = 40
    private const val MAX_ITEM_LENGTH = 600

    fun extract(root: AccessibilityNodeInfo): ScreenTextSnapshot {
        val texts = linkedSetOf<String>()
        var visited = 0
        var sensitive = false

        fun walk(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > MAX_DEPTH || visited++ > MAX_NODES || !node.isVisibleToUser) return
            if (node.isPassword) {
                sensitive = true
                return
            }
            // Never capture what a user is currently typing, even if an app forgot to mark it as a password.
            if (!node.isEditable) {
                sanitize(node.text)?.let(texts::add)
                if (node.text.isNullOrBlank()) sanitize(node.contentDescription)?.let(texts::add)
            }
            for (index in 0 until node.childCount) {
                val child = node.getChild(index) ?: continue
                try { walk(child, depth + 1) } finally {
                    @Suppress("DEPRECATION")
                    if (android.os.Build.VERSION.SDK_INT < 33) child.recycle()
                }
            }
        }
        walk(root, 0)
        return ScreenTextSnapshot(texts, sensitive)
    }

    private fun sanitize(value: CharSequence?): String? {
        val text = value?.toString()?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
        if (text.length !in 2..MAX_ITEM_LENGTH || text.none(Char::isLetter)) return null
        if (looksSensitive(text)) return null
        return text
    }

    private fun looksSensitive(text: String): Boolean {
        val lowered = text.lowercase()
        val secretLabel = listOf("password", "passcode", "one-time password", "verification code", "security code", "cvv", "otp").any(lowered::contains)
        val hasShortCode = Regex("(?<!\\d)\\d{4,8}(?!\\d)").containsMatchIn(text)
        val cardLike = Regex("(?:\\d[ -]?){13,19}").containsMatchIn(text)
        return cardLike || (secretLabel && hasShortCode)
    }
}
