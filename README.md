# 個人記録 (Personal Log)

写真 → 日記 → レシート読取 → 家計簿 をひとつにまとめた個人用アプリ。データは端末内(Room / 内部ストレージ / DataStore)にのみ保存します。Android 先行(minSdk 28)。

- 技術: Kotlin / Jetpack Compose / Material3 / Navigation Compose / Room / Hilt / DataStore / kotlinx-serialization
- applicationId: `com.amgm.personallog`(debug/release 共通)
- タブ: 日記 / レシート / 家計簿 / 設定(Wave1 時点で実装済みは設定画面のみ。他は空の画面)

## ビルドとインストール

ローカルにAndroid SDKがなくても、GitHub Actions(`personal-log` ワークフロー)でビルドされます。`personal-log-app` ブランチへ push すると、テストと `assembleDebug` を実行し、最新のAPKをプレリリースに公開します。

スマホのブラウザから常に最新版を取得できます:

https://github.com/amgm-masamune/profile-app-android-claude-mobile/releases/download/personal-log-debug-latest/personal-log-debug.apk

手元でビルドする場合: `./gradlew testDebugUnitTest assembleDebug`(JDK 17 + Android SDK 35)。

## 更新エラー対策(上書き更新できない問題)

| 対策 | 内容 |
|---|---|
| a. 署名の固定 | `app/debug.keystore` をリポジトリにコミットし、`app/build.gradle.kts` で debug 署名をこのファイルに固定。CIの使い捨てランナーでも毎回同じ署名になる(`INSTALL_FAILED_UPDATE_INCOMPATIBLE` の原因対策)。 |
| b. versionCode | `(System.currentTimeMillis() / 60000L).toInt()`(エポックからの分)で単調増加。ダウングレードにならない。versionName は `0.1.<GITHUB_RUN_NUMBER or local>`。 |
| c. Room | `exportSchema = true`、スキーマは `app/schemas` にコミット、DB version=1。`fallbackToDestructiveMigration` は使わない。version を上げるときは `data/db/Migrations.kt` に Migration を必ず追加する。 |
| d. バックアップ無効 | `android:allowBackup="false"`。自動バックアップの復元でDBだけ古く戻る事故を避ける。 |
| e. 失敗時の診断 | 起動時にDBを開き、例外が出ても落ちずに原因を画面へ表示する。 |

CIは `apksigner verify --print-certs` で署名証明書の SHA-256 をログとReleaseの本文に出します。設定画面でも端末にインストール済みアプリの署名SHA-256を確認できます。

## 更新エラーが出たとき

- 署名が同じなら、APKをそのまま開けば上書きインストールできます(データは残ります)。PCからは `adb install -r personal-log-debug.apk`。端末の「アプリを更新」でも可。
- 署名の違う旧版(固定前のCIビルドなど)が入っている場合のみ、一度アンインストールが必要です(その端末の記録は消えます)。固定後のビルド同士では不要です。
- 起動時にDBエラー画面が出たら、表示された原因を控えてください。
