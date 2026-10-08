package com.example.businesscard.data.model

/** アプリ内で扱う名刺のモデル。UI層はこのクラスだけを知っていればよい。 */
data class BusinessCard(
    val id: Long = 0,
    val name: String,
    val company: String = "",
    val title: String = "",
    val phone: String = "",
    val email: String = "",
)
