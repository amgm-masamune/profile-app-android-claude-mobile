package com.example.businesscard.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessCardDao {
    @Query("SELECT * FROM business_cards ORDER BY id DESC")
    fun observeAll(): Flow<List<BusinessCardEntity>>

    @Query("SELECT * FROM business_cards WHERE id = :id")
    fun observeById(id: Long): Flow<BusinessCardEntity?>

    /** idが0なら新規挿入、既存idなら更新。 */
    @Upsert
    suspend fun upsert(entity: BusinessCardEntity)

    @Query("DELETE FROM business_cards WHERE id = :id")
    suspend fun deleteById(id: Long)
}
