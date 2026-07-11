package com.piyush.livetranslate.domain

import com.piyush.livetranslate.core.model.Translation
import com.piyush.livetranslate.core.model.TranslationRequest
import com.piyush.livetranslate.core.model.TranslationResult
import com.piyush.livetranslate.domain.repository.TranslationRepository
import com.piyush.livetranslate.domain.usecase.TranslateTextUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TranslateTextUseCaseTest {
    @Test fun `request is delegated without mutation`() = runBlocking {
        val fake = FakeRepository()
        val request = TranslationRequest("kya kar rahe ho", "hinglish", "en-US")
        val result = TranslateTextUseCase(fake)(request).getOrThrow()
        assertEquals(request, fake.lastRequest)
        assertEquals("What are you doing?", result.translatedText)
    }
}

private class FakeRepository : TranslationRepository {
    var lastRequest: TranslationRequest? = null
    override fun observeHistory(query: String): Flow<List<Translation>> = flowOf(emptyList())
    override fun observeFavorites(): Flow<List<Translation>> = flowOf(emptyList())
    override suspend fun translate(request: TranslationRequest): Result<TranslationResult> {
        lastRequest = request
        return Result.success(TranslationResult("What are you doing?", "hi", .98f, "fake"))
    }
    override suspend fun delete(id: String) = Unit
    override suspend fun clearHistory() = Unit
    override suspend fun setFavorite(id: String, favorite: Boolean) = Unit
    override suspend fun exportCsv(): String = ""
    override suspend fun syncNow(): Result<Unit> = Result.success(Unit)
    override suspend fun clearCache() = Unit
}
