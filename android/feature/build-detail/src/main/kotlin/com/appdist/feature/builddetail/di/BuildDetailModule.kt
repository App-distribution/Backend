package com.appdist.feature.builddetail.di

import android.content.Context
import android.content.pm.PackageManager
import com.appdist.feature.builddetail.data.BuildDetailRepositoryImpl
import com.appdist.feature.builddetail.domain.BuildDetailRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BuildDetailModule {
    @Binds @Singleton
    abstract fun bindBuildDetailRepository(impl: BuildDetailRepositoryImpl): BuildDetailRepository
}

@Module
@InstallIn(SingletonComponent::class)
object BuildDetailProviders {
    @Provides @Singleton
    fun providePackageManager(@ApplicationContext context: Context): PackageManager =
        context.packageManager
}
