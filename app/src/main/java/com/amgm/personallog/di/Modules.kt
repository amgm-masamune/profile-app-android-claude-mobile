package com.amgm.personallog.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.amgm.personallog.data.db.AppDatabase
import com.amgm.personallog.data.db.DiaryDao
import com.amgm.personallog.data.db.ExpenseDao
import com.amgm.personallog.data.db.Migrations
import com.amgm.personallog.data.db.ReceiptDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            // fallbackToDestructiveMigration は使わない。version を上げたら Migrations.kt に追加する。
            .addMigrations(*Migrations.ALL)
            .build()

    @Provides fun provideDiaryDao(db: AppDatabase): DiaryDao = db.diaryDao()
    @Provides fun provideReceiptDao(db: AppDatabase): ReceiptDao = db.receiptDao()
    @Provides fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.settingsDataStore
}
