package com.piyush.livetranslate.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_settings ADD COLUMN overlayConsentGranted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN overlayTargetLanguage TEXT NOT NULL DEFAULT 'en-US'")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN overlayOcrFallbackEnabled INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN overlaySaveHistory INTEGER NOT NULL DEFAULT 0")
    }
}
