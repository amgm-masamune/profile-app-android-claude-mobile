package com.example.businesscard.di

import com.example.businesscard.data.repository.OfflineFirstBusinessCardRepository
import com.example.businesscard.domain.repository.BusinessCardRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** domainのRepositoryインタフェースに、data層の実装を結びつける。 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBusinessCardRepository(impl: OfflineFirstBusinessCardRepository): BusinessCardRepository
}
