package com.example.businesscard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Roomに保存するための名刺テーブル。data層の内部表現で、UI/domainには公開しない。 */
@Entity(tableName = "business_cards")
data class BusinessCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val company: String,
    val title: String,
    val phone: String,
    val email: String,
)
