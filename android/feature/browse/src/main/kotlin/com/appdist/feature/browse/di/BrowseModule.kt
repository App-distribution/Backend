package com.appdist.feature.browse.di

import com.appdist.feature.browse.data.BuildRepositoryImpl
import com.appdist.feature.browse.data.ProjectRepositoryImpl
import com.appdist.feature.browse.domain.BuildRepository
import com.appdist.feature.browse.domain.ProjectRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BrowseModule {
    @Binds @Singleton
    abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository

    @Binds @Singleton
    abstract fun bindBuildRepository(impl: BuildRepositoryImpl): BuildRepository
}
