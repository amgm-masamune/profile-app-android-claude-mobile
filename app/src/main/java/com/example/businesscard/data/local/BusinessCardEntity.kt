package com.example.businesscard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.businesscard.data.model.BusinessCard

/** Roomに保存するための名刺テーブル。 */
@Entity(tableName = "business_cards")
data class BusinessCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val company: String,
    val title: String,
    val phone: String,
    val email: String,
)

fun BusinessCardEntity.asExternalModel() = BusinessCard(
    id = id,
    name = name,
    company = company,
    title = title,
    phone = phone,
    email = email,
)

fun BusinessCard.asEntity() = BusinessCardEntity(
    id = id,
    name = name,
    company = company,
    title = title,
    phone = phone,
    email = email,
)
