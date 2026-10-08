package com.example.businesscard.domain.repository

import com.example.businesscard.domain.model.ThemeStyle
import kotlinx.coroutines.flow.Flow

/**
 * 利用者の設定へのアクセス契約。domain層が定義し、data層が実装する(依存関係逆転)。
 */
interface UserPreferencesRepository {
    /** 選んでいる見た目。まだ選んでいなければ [ThemeStyle.DEFAULT]。 */
    val themeStyle: Flow<ThemeStyle>

    suspend fun setThemeStyle(style: ThemeStyle)
}
