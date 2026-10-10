package com.amgm.personallog.backup

/** ZIPエントリ名・DB内パスの検証(ZIPスリップ対策)。純粋関数でJVMテスト可能。 */
object BackupPaths {
    /** バックアップ対象の filesDir 直下ディレクトリ。 */
    val ROOT_DIRS = listOf("diary", "receipts")

    /**
     * filesDir からの相対パスとして安全なら正規化したパス("diary/a.jpg")を返し、危険なら null。
     * 拒否: 空, 絶対パス, 逆スラッシュ, ドライブ指定, NUL, ".." や "." やの空セグメント, 対象外ディレクトリ配下以外。
     */
    fun safeRelativePath(path: String): String? {
        if (path.isEmpty() || path.contains('\u0000') || path.contains('\\')) return null
        if (path.startsWith("/") || path.contains(':')) return null
        val segments = path.split('/')
        if (segments.any { it.isEmpty() || it == "." || it == ".." }) return null
        if (segments.size < 2) return null
        if (segments[0] !in ROOT_DIRS) return null
        return segments.joinToString("/")
    }

    /** ZIPエントリ名 "files/diary/a.jpg" -> "diary/a.jpg"。安全でなければ null。 */
    fun entryNameToRelative(entryName: String): String? {
        if (!entryName.startsWith(BACKUP_FILES_PREFIX)) return null
        return safeRelativePath(entryName.removePrefix(BACKUP_FILES_PREFIX))
    }

    /** DB内の filePath / imagePath 用。空は「ファイル無し」として許容。トラバーサル(絶対パス, "..")だけを拒否する。 */
    fun isSafeStoredPath(path: String): Boolean {
        if (path.isBlank()) return true
        if (path.contains('\u0000') || path.contains('\\') || path.startsWith("/") || path.contains(':')) return false
        return path.split('/').none { it == ".." }
    }
}
