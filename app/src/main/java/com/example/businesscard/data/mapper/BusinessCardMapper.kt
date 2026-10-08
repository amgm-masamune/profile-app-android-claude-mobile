package com.example.businesscard.data.mapper

import com.example.businesscard.data.local.BusinessCardEntity
import com.example.businesscard.domain.model.BusinessCard

fun BusinessCardEntity.toDomain() = BusinessCard(
    id = id,
    name = name,
    company = company,
    title = title,
    phone = phone,
    email = email,
)

fun BusinessCard.toEntity() = BusinessCardEntity(
    id = id,
    name = name,
    company = company,
    title = title,
    phone = phone,
    email = email,
)
