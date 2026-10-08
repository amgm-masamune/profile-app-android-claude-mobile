# デザインシステム「Soft Glass」(グレージュ版)

## 見本

ユーザーが共有したUIキットの画像(2026-10-09)を忠実に再現している。
グレージュの壁に、すりガラスの部品(Launch / Secondary / Icon button + Text field / Dropdown / Toggle / Toggle(スイッチ) / Tabs / Premium plan)が浮いている写真風の一枚。

この画像の特徴:

- **壁**: グレージュ(灰色がかったベージュ)。左上から斜めに光が差し込み、左下と下中央は陰になっている。
- **すりガラス**: 白の薄い膜(約18%)。角丸は控えめ(pillではない)。縁は1dpの白。
- **下端の光**: 部品の下端の中央がいちばん白く光り、その光が壁にも漏れる。主ボタンほど強い。
- **右下の影**: どの部品も同じだけ右下に影を落とす(壁から同じ距離に浮いているため)。
- **文字**: すべて白。太字(Launch / Premium plan)と細字(Secondary / Tabs)で階層を作る。

一般的な呼び名は **Glassmorphism(グラスモーフィズム)** + **Soft UI**。本プロジェクトでは引き続き「Soft Glass」と呼ぶ。

## 原則

1. **面はすりガラス**: 白の薄い膜 + 上端のつや + 白い縁。
2. **光は下から**: 部品の下端が光る。強さは3段階(Strong / Medium / Soft)。主操作だけ Strong。
3. **影は右下**: 大きさに関係なく同じずれ(14dp, 14dp)とぼかし(12dp)。影は部品の外側だけに描き、ガラス越しに透けさせない。
4. **すりガラスを重ねない**: 実ブラーが無いので、ガラスの上にガラスを浮かせると濁る。浮かせるボタンは使わず、リストの外に並べる。
5. **直接色を書かない**: 画面のコードに `Color(0x...)` を書かず、トークン(`SoftGlassTheme.colors`)から使う。
6. **ライト/ダークの切り替えは持たない**: 見本が一枚の写真なので、端末設定に関係なく同じ見た目。

## トークン

実体は `ui/theme/`。

| 区分 | ファイル | 内容 |
|---|---|---|
| 色 | `SoftGlassColors.kt` | 壁、ガラス面、光、影、文字、危険色 |
| 形・大きさ・光 | `SoftGlassShapes.kt` | `SoftGlassShapes`(control 14dp / tile 12dp / card 22dp / dialog 26dp / pill)、`SoftGlassSize`(部品の高さ56dp)、`SoftGlassLight`(影のずれ・ぼかし) |
| 文字 | `Type.kt` | `SoftGlassType`。書体は端末標準。全ての文字にごく弱い影 |
| テーマ | `Theme.kt` | トークンを配り、Material3 の ColorScheme にも対応づける |

主な色(見本の画像から拾った値):

| トークン | 値 | 用途 |
|---|---|---|
| wallTop → wallBottom | #AA9B8E → #968677 | 壁の縦グラデーション |
| wallLight / wallShade | #E2D7CB / #6E6054 | 斜めの光の帯 / 左下・下中央の陰 |
| glass | 白18% | 部品の膜 |
| glassPressed / glassSelected / glassTile | 白30% / 白32% / 白45% | 押下・フォーカス / 選択中のタブ / アイコンのタイル |
| glassBorder / focusBorder | 白55% / 白95% | 縁 / フォーカス中の縁 |
| glow | #FFFCF6 | 下端の光 |
| castShadow | #281C12 の33% | 右下の影 |
| ink / inkMuted / inkFaint | 白 / 白78% / 白60% | 文字 / 補助 / プレースホルダ・非選択 |
| danger | #FFC7BD | 削除・エラー |

## コンポーネント

実体は `ui/component/`。`ComponentPreviews.kt` の `SoftGlassCatalog` が見本と同じ並び(2列 x 4段)のカタログ。

| 見本の部品 | コンポーネント | アプリでの使い道 |
|---|---|---|
| (全部品の土台) | `Modifier.glassSurface` | 影・漏れる光・膜・内側の光・縁・光る縁の6層を描く |
| 壁 | `SoftGlassBackground` / `SoftGlassScaffold` | 全画面の背景 |
| Launch / Premium plan | `GlassButton`(Primary) | 名刺を追加、保存 |
| Secondary | `GlassButton`(Secondary) | ダイアログのキャンセル |
| Icon button | `GlassIconButton` | 戻る、編集、削除 |
| Icon button + Text field | `GlassTextField`(`leadingIcon`) | 編集画面の各欄 |
| Dropdown | `GlassDropdown` | (カタログのみ) |
| Toggle(横並び) | `GlassSegmentedControl` | (カタログのみ) |
| Toggle(スイッチ) | `GlassSwitch` | (カタログのみ) |
| Tabs | `GlassTabs` | (カタログのみ) |
| — | `GlassLabel` | 部品の上の小さな見出し |
| — | `GlassConfirmDialog` | 削除の確認 |
| — | `BusinessCardView` | 名刺(ボタンと同じ質感を名刺の大きさに) |

## ボタン配置のルール

- 主操作は**画面の下**(縦画面)または**右下**(横画面)。親指が届く場所に置く。
- 主操作は1画面に1つだけ強く光らせる。
- **戻る**は左上の四角いアイコンボタンに統一。
- 破壊的な操作(削除)は主操作から離し、危険色のアイコンボタンにして、**確認ダイアログ**を挟む。

| 画面 | 配置 |
|---|---|
| 一覧 | 下に横いっぱいの「名刺を追加」(Primary)。名刺の上には浮かせない |
| 表示(横画面) | 左上に戻る、右下に編集(強く光るアイコンボタン)。名刺は中央いっぱい |
| 編集 | 上は戻るのみ。下に[削除(編集時のみ・左)][保存(右・広い)]。キーボードの真上に追従 |

## 見た目の確認(スクリーンショット)

`app/src/test/.../ui/ScreenshotTest.kt` が、カタログと各画面を Robolectric のネイティブ描画でPNGにする。

```
./gradlew testDebugUnitTest --tests '*ScreenshotTest' -PrecordScreenshots=true
# → app/build/outputs/screenshots/*.png
```

mainへのpushごとに GitHub Actions がこれを実行し、画像を **`screenshots` ブランチ**に置き直す(ブランチのREADMEに全画像が並ぶ)。

## 実装上の制約(2026-10-09 時点)

- **実ブラーは使っていない**。すりガラスは「白の薄い膜 + つや + 縁」で近似。壁がなめらかなグラデーションなので見た目の差は小さい。背面のぼかしはAndroidでは `RenderEffect`(Android 12以上)が必要で、Composeの標準では面の背面だけをぼかせないため見送った。
- 右下の影は `Paint.setShadowLayer` で描いている。ハードウェア描画では **Android 9(API 28)以上**で表示され、Android 8系(minSdk 26)では影が出ない(光と膜はそのまま)。
- **白い文字のコントラストは低い**(明るいガラスの上で約2:1)。見本に忠実にした結果で、WCAGの基準(4.5:1)は満たさない。弱い文字の影で読みやすさを補っている。
- フォントは端末標準。見本の書体(SF Pro 風)とは異なる。
