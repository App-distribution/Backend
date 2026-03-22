package com.appdist.core.database

import android.content.Context
import androidx.room.Room
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "appdist.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideBuildDao(db: AppDatabase) = db.buildDao()
    @Provides fun provideInstallHistoryDao(db: AppDatabase) = db.installHistoryDao()
    @Provides fun provideDownloadDao(db: AppDatabase) = db.downloadDao()
}
