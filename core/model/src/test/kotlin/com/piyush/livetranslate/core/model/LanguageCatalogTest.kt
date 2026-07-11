package com.piyush.livetranslate.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageCatalogTest {
    @Test fun `catalog has unique tags and dedicated Hinglish support`() {
        val tags = LanguageCatalog.supported.map { it.tag }
        assertEquals(tags.size, tags.toSet().size)
        assertTrue("hinglish" in tags)
        assertTrue(tags.size >= 20)
    }

    @Test fun `automatic and Hinglish speech use Hindi recognizer locale`() {
        assertEquals("hi-IN", LanguageCatalog.speechLocale("auto"))
        assertEquals("hi-IN", LanguageCatalog.speechLocale("hinglish"))
        assertNotEquals("auto", LanguageCatalog.speechLocale("auto"))
    }
}
