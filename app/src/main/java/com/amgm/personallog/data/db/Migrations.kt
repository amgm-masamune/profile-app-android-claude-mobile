package com.amgm.personallog.data.db

import androidx.room.migration.Migration

/**
 * ルール(更新エラー/データ消失の防止):
 *  - AppDatabase の version を上げるたびに、ここへ Migration(旧, 新) を必ず追加し ALL に登録する。
 *  - fallbackToDestructiveMigration は絶対に使わない。
 *  - version を上げたら app/schemas/<version>.json もコミットする。
 *
 * 例: val MIGRATION_1_2 = object : Migration(1, 2) {
 *         override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE ...") }
 *     }
 * 現在は version = 1 のため Migration はまだ無い。
 */
object Migrations {
    val ALL: Array<Migration> = arrayOf()
}
