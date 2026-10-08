package com.example.businesscard.di

import android.content.Context
import androidx.room.Room
import com.example.businesscard.data.local.AppDatabase
import com.example.businesscard.data.local.BusinessCardDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Room(DB本体とDAO)の提供。 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "business_card.db").build()

    @Provides
    fun provideBusinessCardDao(database: AppDatabase): BusinessCardDao = database.businessCardDao()
}
