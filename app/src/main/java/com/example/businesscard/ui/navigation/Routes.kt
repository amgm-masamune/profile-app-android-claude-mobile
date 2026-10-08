package com.example.businesscard.ui.navigation

/** ナビゲーション引数のキー。SavedStateHandleからも同じキーで読む。 */
const val CARD_ID_ARG = "cardId"

/** 新規作成を表すid。 */
const val NEW_CARD_ID = -1L

object Routes {
    const val LIST = "list"
    const val DETAIL = "detail/{$CARD_ID_ARG}"
    const val EDIT = "edit?$CARD_ID_ARG={$CARD_ID_ARG}"

    fun detail(id: Long) = "detail/$id"

    /** idがnullなら新規作成画面。 */
    fun edit(id: Long? = null) = if (id == null) "edit" else "edit?$CARD_ID_ARG=$id"
}
