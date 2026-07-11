package com.piyush.livetranslate.data.translation

import com.piyush.livetranslate.domain.repository.SettingsRepository
import com.piyush.livetranslate.domain.repository.SpeechRecognizerEngine
import com.piyush.livetranslate.domain.repository.SpeechSynthesizer
import com.piyush.livetranslate.domain.repository.TranslationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TranslationModule {
    @Binds @Singleton abstract fun provider(implementation: FirebaseGroqTranslationProvider): AiTranslationProvider
    @Binds @Singleton abstract fun repository(implementation: TranslationRepositoryImpl): TranslationRepository
    @Binds @Singleton abstract fun settings(implementation: SettingsRepositoryImpl): SettingsRepository
    @Binds @Singleton abstract fun speechRecognizer(implementation: AndroidSpeechRecognizerEngine): SpeechRecognizerEngine
    @Binds @Singleton abstract fun speechSynthesizer(implementation: AndroidSpeechSynthesizer): SpeechSynthesizer
}
