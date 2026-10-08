package com.example.businesscard.data

import com.example.businesscard.data.model.BusinessCard
import kotlinx.coroutines.flow.Flow

/** データ層の入口。名刺データの唯一の情報源(Single Source of Truth)。 */
interface BusinessCardRepository {
    /** 全名刺(新しい順)。 */
    val cards: Flow<List<BusinessCard>>

    /** 指定した名刺。存在しなければnullを流す。 */
    fun observeCard(id: Long): Flow<BusinessCard?>

    /** idが0なら新規作成、それ以外は更新。 */
    suspend fun save(card: BusinessCard)

    suspend fun delete(id: Long)
}
