package com.navidrometv.di

import android.content.Context
import androidx.room.Room
import com.navidrometv.data.local.MusicCacheDao
import com.navidrometv.data.local.NavidromeDatabase
import com.navidrometv.data.local.PlayHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NavidromeDatabase =
        Room.databaseBuilder(context, NavidromeDatabase::class.java, "navidrome_tv.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun providePlayHistoryDao(database: NavidromeDatabase): PlayHistoryDao = database.playHistoryDao()

    @Provides
    fun provideMusicCacheDao(database: NavidromeDatabase): MusicCacheDao = database.musicCacheDao()
}
