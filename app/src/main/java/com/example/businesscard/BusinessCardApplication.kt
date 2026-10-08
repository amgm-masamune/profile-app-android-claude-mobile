package com.example.businesscard

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.example.businesscard.data.BusinessCardRepository
import com.example.businesscard.data.OfflineFirstBusinessCardRepository
import com.example.businesscard.data.local.AppDatabase

/** 依存関係の置き場。手動DI(Hilt等を使わない最小構成)。 */
interface AppContainer {
    val businessCardRepository: BusinessCardRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(context, AppDatabase::class.java, "business_card.db").build()
    }

    override val businessCardRepository: BusinessCardRepository by lazy {
        OfflineFirstBusinessCardRepository(database.businessCardDao())
    }
}

class BusinessCardApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
