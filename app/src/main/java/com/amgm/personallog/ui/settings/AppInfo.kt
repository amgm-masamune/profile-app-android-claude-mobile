package com.amgm.personallog.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import java.security.MessageDigest

data class AppInfo(
    val versionName: String,
    val versionCode: Long,
    val signatureSha256: String,
) {
    companion object {
        fun read(context: Context): AppInfo = try {
            val info = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES,
            )
            val signer = info.signingInfo?.apkContentsSigners?.firstOrNull()
            val sha = signer?.let {
                MessageDigest.getInstance("SHA-256").digest(it.toByteArray())
                    .joinToString(":") { b -> "%02X".format(b) }
            } ?: "不明"
            AppInfo(info.versionName ?: "?", info.longVersionCode, sha)
        } catch (t: Throwable) {
            AppInfo("?", 0L, "取得失敗: ${t.message}")
        }
    }
}
