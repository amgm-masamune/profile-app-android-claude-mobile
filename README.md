# BusinessCard

Android公式の推奨アーキテクチャを学ぶための、いちばんシンプルな名刺アプリ。

- UI: Jetpack Compose + Navigation Compose(3画面: 一覧 / 表示 / 編集)
- 表示画面は横画面固定
- 永続化: Room
- 複数の名刺を保持。レイアウトは固定で、MVPでは文字のみ編集可能(背景色・文字色の変更は今後)

## アーキテクチャ

Android公式の推奨アーキテクチャ(UI層 / ドメイン層 / データ層)に沿って、単一モジュール内をパッケージで明示的に分ける。依存の向きは `ui → domain ← data`(domainは何にも依存しない)。

```
ui (Composable / ViewModel)
        │ 呼ぶ
        ▼
domain (model / Repositoryインタフェース / UseCase)   ← 業務ルール(氏名必須、空白除去)
        ▲ 実装する
        │
data (Room: Dao・Entity / mapper / Repository実装)
```

```
com.example.businesscard
├─ BusinessCardApplication.kt   @HiltAndroidApp
├─ MainActivity.kt              @AndroidEntryPoint
├─ di/                          Hiltモジュール(DatabaseModule, RepositoryModule)
├─ domain/                      純粋なKotlin(Android非依存)
│  ├─ model/                    BusinessCard
│  ├─ repository/               BusinessCardRepository(インタフェース)
│  └─ usecase/                  Observe(s) / Save / Delete
├─ data/
│  ├─ local/                    AppDatabase, BusinessCardDao, BusinessCardEntity
│  ├─ mapper/                   Entity <-> Domain変換
│  └─ repository/               OfflineFirstBusinessCardRepository(実装)
└─ ui/
   ├─ navigation/               型安全ナビゲーション(Destinations, AppNavHost)
   ├─ list/ detail/ edit/       画面ごとに *Screen.kt と *ViewModel.kt
   ├─ component/ theme/         共通部品・デザイントークン
```

- 単方向データフロー: 状態(UiState)は下へ、イベントは上へ
- DI: Hilt。ViewModelは `@HiltViewModel`、RepositoryはインタフェースをBindsで実装に結びつける
- ナビゲーション: Navigation Compose の型安全API(`@Serializable` な遷移先 + `toRoute`)

## デザイン

デザインシステム「Soft Glass」(グレージュ版)。グレージュの壁に、下端が光るすりガラスの部品が浮き、右下に影を落とす。共有されたUIキットの画像を忠実に再現したもの。トークンは `ui/theme/`、共通部品は `ui/component/`。詳細とボタン配置のルールは [DESIGN.md](DESIGN.md)。

最新の見た目は [`screenshots` ブランチ](../../tree/screenshots) で確認できる(pushごとにCIが自動生成)。

## ビルドとテスト

```
./gradlew testDebugUnitTest   # ユニットテスト
./gradlew assembleDebug       # APK作成
./gradlew testDebugUnitTest --tests '*ScreenshotTest' -PrecordScreenshots=true   # 画面のPNGを書き出す
```

push時に GitHub Actions(`.github/workflows/ci.yml`)でも同じものを実行し、画面のPNGを `screenshots` ブランチに置く。

## 今後

- 名刺ごとの背景色・文字色の変更
