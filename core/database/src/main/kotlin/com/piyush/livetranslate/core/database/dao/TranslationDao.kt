package com.piyush.livetranslate.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.piyush.livetranslate.core.database.entity.FavoriteEntity
import com.piyush.livetranslate.core.database.entity.TranslationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationDao {
    @Query("SELECT * FROM translations ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TranslationEntity>>

    @Query("SELECT * FROM translations WHERE sourceText LIKE '%' || :query || '%' OR translatedText LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun search(query: String): Flow<List<TranslationEntity>>

    @Query("SELECT * FROM translations WHERE id = :id LIMIT 1")
    suspend fun get(id: String): TranslationEntity?

    @Query("SELECT * FROM translations WHERE syncState != 'SYNCED' ORDER BY updatedAt LIMIT :limit")
    suspend fun pending(limit: Int = 100): List<TranslationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TranslationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<TranslationEntity>)

    @Query("UPDATE translations SET syncState = :state WHERE id = :id")
    suspend fun setSyncState(id: String, state: String)

    @Query("DELETE FROM translations WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM translations")
    suspend fun clear()

    @Query("SELECT * FROM translations ORDER BY createdAt DESC")
    suspend fun snapshot(): List<TranslationEntity>
}

@Dao
interface FavoriteDao {
    @Query("SELECT translationId FROM favorites")
    fun observeIds(): Flow<List<String>>

    @Query("SELECT t.* FROM translations t INNER JOIN favorites f ON f.translationId = t.id ORDER BY f.createdAt DESC")
    fun observeTranslations(): Flow<List<TranslationEntity>>

    @Query("SELECT * FROM favorites WHERE syncState != 'SYNCED' LIMIT :limit")
    suspend fun pending(limit: Int = 100): List<FavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE translationId = :id")
    suspend fun delete(id: String)

    @Query("UPDATE favorites SET syncState = :state WHERE translationId = :id")
    suspend fun setSyncState(id: String, state: String)
}
