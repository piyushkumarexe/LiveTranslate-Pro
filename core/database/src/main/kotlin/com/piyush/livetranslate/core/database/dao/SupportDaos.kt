package com.piyush.livetranslate.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.piyush.livetranslate.core.database.entity.CachedResultEntity
import com.piyush.livetranslate.core.database.entity.OfflineMetadataEntity
import com.piyush.livetranslate.core.database.entity.RecentLanguageEntity
import com.piyush.livetranslate.core.database.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM user_settings WHERE profile = 'default' LIMIT 1")
    fun observe(): Flow<UserSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserSettingsEntity)
}

@Dao
interface CacheDao {
    @Query("SELECT * FROM cached_results WHERE cacheKey = :key AND expiresAt > :now LIMIT 1")
    suspend fun getValid(key: String, now: Long = System.currentTimeMillis()): CachedResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedResultEntity)

    @Query("DELETE FROM cached_results WHERE expiresAt <= :now")
    suspend fun prune(now: Long = System.currentTimeMillis())

    @Query("DELETE FROM cached_results")
    suspend fun clear()
}

@Dao
interface MetadataDao {
    @Query("SELECT * FROM offline_metadata WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): OfflineMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OfflineMetadataEntity)
}

@Dao
interface RecentLanguageDao {
    @Query("SELECT * FROM recent_languages ORDER BY lastUsedAt DESC LIMIT :limit")
    fun observe(limit: Int = 8): Flow<List<RecentLanguageEntity>>

    @Query("SELECT * FROM recent_languages WHERE languageTag = :tag LIMIT 1")
    suspend fun get(tag: String): RecentLanguageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecentLanguageEntity)
}
