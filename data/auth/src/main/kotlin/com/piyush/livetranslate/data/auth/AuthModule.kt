package com.piyush.livetranslate.data.auth

import com.piyush.livetranslate.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds @Singleton
    abstract fun authRepository(implementation: FirebaseAuthRepository): AuthRepository
}
