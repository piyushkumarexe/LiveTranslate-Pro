package com.piyush.livetranslate.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.piyush.livetranslate.core.database.dao.CacheDao
import com.piyush.livetranslate.core.database.dao.FavoriteDao
import com.piyush.livetranslate.core.database.dao.MetadataDao
import com.piyush.livetranslate.core.database.dao.RecentLanguageDao
import com.piyush.livetranslate.core.database.dao.SettingsDao
import com.piyush.livetranslate.core.database.dao.TranslationDao
import com.piyush.livetranslate.core.database.entity.CachedResultEntity
import com.piyush.livetranslate.core.database.entity.FavoriteEntity
import com.piyush.livetranslate.core.database.entity.OfflineMetadataEntity
import com.piyush.livetranslate.core.database.entity.RecentLanguageEntity
import com.piyush.livetranslate.core.database.entity.TranslationEntity
import com.piyush.livetranslate.core.database.entity.UserSettingsEntity

@Database(
    entities = [
        TranslationEntity::class,
        FavoriteEntity::class,
        RecentLanguageEntity::class,
        UserSettingsEntity::class,
        OfflineMetadataEntity::class,
        CachedResultEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class LiveTranslateDatabase : RoomDatabase() {
    abstract fun translationDao(): TranslationDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun settingsDao(): SettingsDao
    abstract fun cacheDao(): CacheDao
    abstract fun metadataDao(): MetadataDao
    abstract fun recentLanguageDao(): RecentLanguageDao
}
