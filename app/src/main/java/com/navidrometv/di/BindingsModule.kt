package com.navidrometv.di

import com.navidrometv.core.common.AppDispatchers
import com.navidrometv.core.common.DefaultAppDispatchers
import com.navidrometv.data.repository.AuthRepositoryImpl
import com.navidrometv.data.repository.HistoryRepositoryImpl
import com.navidrometv.data.repository.MusicRepositoryImpl
import com.navidrometv.data.repository.PlaylistRepositoryImpl
import com.navidrometv.data.repository.SettingsRepositoryImpl
import com.navidrometv.domain.repository.AuthRepository
import com.navidrometv.domain.repository.HistoryRepository
import com.navidrometv.domain.repository.MusicRepository
import com.navidrometv.domain.repository.PlaylistRepository
import com.navidrometv.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {

    @Binds
    @Singleton
    abstract fun bindDispatchers(impl: DefaultAppDispatchers): AppDispatchers

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindMusicRepository(impl: MusicRepositoryImpl): MusicRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
