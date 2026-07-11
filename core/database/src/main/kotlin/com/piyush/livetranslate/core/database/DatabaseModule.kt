package com.piyush.livetranslate.core.database

import android.content.Context
import androidx.room.Room
import com.piyush.livetranslate.core.database.dao.CacheDao
import com.piyush.livetranslate.core.database.dao.FavoriteDao
import com.piyush.livetranslate.core.database.dao.MetadataDao
import com.piyush.livetranslate.core.database.dao.RecentLanguageDao
import com.piyush.livetranslate.core.database.dao.SettingsDao
import com.piyush.livetranslate.core.database.dao.TranslationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): LiveTranslateDatabase =
        Room.databaseBuilder(context, LiveTranslateDatabase::class.java, "live_translate.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides fun translations(db: LiveTranslateDatabase): TranslationDao = db.translationDao()
    @Provides fun favorites(db: LiveTranslateDatabase): FavoriteDao = db.favoriteDao()
    @Provides fun settings(db: LiveTranslateDatabase): SettingsDao = db.settingsDao()
    @Provides fun cache(db: LiveTranslateDatabase): CacheDao = db.cacheDao()
    @Provides fun metadata(db: LiveTranslateDatabase): MetadataDao = db.metadataDao()
    @Provides fun recentLanguages(db: LiveTranslateDatabase): RecentLanguageDao = db.recentLanguageDao()
}
