# BusinessCard

Android公式の推奨アーキテクチャを学ぶための、いちばんシンプルな名刺アプリ。

- UI: Jetpack Compose + Navigation Compose(3画面: 一覧 / 表示 / 編集)
- 表示画面は横画面固定
- 永続化: Room
- 複数の名刺を保持。レイアウトは固定で、MVPでは文字のみ編集可能(背景色・文字色の変更は今後)

## アーキテクチャ

```
UI層   (Composable) ──events──▶ ViewModel (StateFlowでUiStateを公開)
                                   │
データ層                      Repository (interface / OfflineFirst実装)
                                   │
                               Room (Dao / Entity)
```

- 単方向データフロー: 状態は下へ、イベントは上へ
- `data/` : モデル、Room、Repository
- `ui/`   : 画面ごとに `*Screen.kt`(状態を受け取るだけ)と `*ViewModel.kt`
- DIは `BusinessCardApplication` の `AppContainer` による手動DI(Hiltなし)
- ViewModelの生成は `AppViewModelProvider` に集約

## ビルドとテスト

```
./gradlew testDebugUnitTest   # ユニットテスト
./gradlew assembleDebug       # APK作成
```

push時に GitHub Actions(`.github/workflows/ci.yml`)でも同じものを実行する。

## 今後

- 名刺ごとの背景色・文字色の変更
