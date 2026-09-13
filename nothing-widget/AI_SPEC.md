# AI_SPEC — nothing-widget

このファイルはAIコーディングエージェント(Claude Code / Codex)がこのプロジェクトを変更する前に読む前提のスペックです。

## 現状
「パパっとメモして共有」ウィジェット。**Jetpack Glance**でウィジェット本体(`NothingWidget`, 2x2)をタップすると、上部に黒帯だけがすっと出る`QuickMemoActivity`が開く。メモ帳アプリのように、テキスト入力欄がほぼ全面を占め、右上に共有アイコンだけが乗るミニマルな見た目。閉じる専用ボタンは無く、システムの戻る操作で閉じる。

Discord Webhook直接投稿は試作したが「どうでもいい」とのことで削除済み。メモと共有(OS標準シェアシート)だけのシンプル構成。

## アーキテクチャ
- ウィジェット本体: `NothingWidget : GlanceAppWidget()`。`provideGlance` → `provideContent { Content() }` の現行API。黒背景・中央寄せの「M E M O / + 書いて共有」表示で、ウィジェット全体がタップ可能(`clickable(actionStartActivity<QuickMemoActivity>())`)。サイズは2x2(`minWidth`/`minHeight` 110dp + `targetCellWidth`/`targetCellHeight` 2)。
- レシーバ: `NothingWidgetReceiver : GlanceAppWidgetReceiver()`。
- `QuickMemoActivity`: `AppCompatActivity`。カスタムテーマ`Theme.QuickMemo`(`windowNoTitle` + `windowIsTranslucent` + `backgroundDimEnabled=false`)で、タイトルバーも背景の暗いオーバーレイも出さない。`onCreate`で`window.setLayout(MATCH_PARENT, WRAP_CONTENT)` + `setGravity(Gravity.TOP)`にして、画面上部にだけ黒帯が乗るようにしている。`windowSoftInputMode="stateVisible"`でキーボードを自動表示。
  - レイアウトは`FrameLayout`。`EditText`(`memo_input`)が全面を占め、右上に共有用`ImageButton`(`share_button`、システム標準アイコン`ic_menu_share`+白 tint)だけが乗る。
  - `share_button`タップ: (1)テキストが前回保存時から変わっていれば`MemoStore.addMemo`で保存 (2)`Intent.ACTION_SEND`(`type="text/plain"`)を`Intent.createChooser`で開く (3)**`finish()`しない**(複数宛先へ続けて共有できるように)。
  - **ホーム画面ウィジェット自体はテキスト入力欄を持てない**(RemoteViews/Glance共通の制約)ため、この画面が唇一の入力経路。
- `MemoStore`: 書いたメモをSharedPreferences(JSON配列、`text` + `created_at`)に保存するだけの単純なストア。閲覧用のUIはまだ無い(保存のみ)。
- `res/layout/widget_nothing.xml` は Glance の描画が読み込まれる前の一瞬だけ表示されるプレースホルダ(`widget_info.xml`の`initialLayout`)。実際の表示は全て `Content()` 側で書く。

## 意図的にやらないこと
- 宛先(Essential Space・特定のAIアプリなど)をハードコードしたIntent送信は**しない**。OS標準の`ACTION_SEND` + `createChooser`だけを使い、宛先選択はユーザーとOSに委ねる。
- ウィジェット内にテキスト入力欄を持たせようとしない(技術的に不可能。Nothing Playground(playground.nothing.tech)のウィジェットはNothing独自の非公開プラットフォームで動いており、標準AppWidget/RemoteViewsの制約を受けないため同じ見た目を再現できない)。
- 「共有」のたびに保存が重複しないよう、同一テキストの連続共有では2回目以降`MemoStore.addMemo`を呼ばない(直前保存済みテキストと比較)。
- 画面を常時ボタンだらけにしない。閉じるボタンは常設せず、戻る操作に任せる。
- Discord Webhook直接投稿は一度実装したが不要と判断され削除済み。`WebhookStore`/`DiscordWebhookPoster`は存在しない。**再度追加する場合は明示的な指示を待つこと。**

## 制約・規約
- 言語: Kotlin 2.0.21。AGP 8.7.2。Compose CompilerはKotlinプラグイン経由(`org.jetbrains.kotlin.plugin.compose`)。
- `androidx.glance:glance-appwidget` / `androidx.glance:glance` は 1.1.1 で固定。フルのCompose UI(compose-ui/material)は使わない。`Alignment`は`androidx.glance.layout.Alignment`(`androidx.glance.Alignment`ではない)。
- `androidx.appcompat:appcompat:1.7.0` を`QuickMemoActivity`用に追加済み。
- モジュール構成: `nothing-widget/app` の単一モジュール。
- `applicationId` / `namespace`: `com.t25hash.nothingwidget`。変更しない。
- `minSdk 26` / `compileSdk 34` / `targetSdk 34`。
- ネットワーク権限は不要(`INTERNET`permissionは削除済み)。`allowBackup="true"`(生きた認証情報を保持しなくなったため)。
- Gradle wrapperバイナリはリポジトリに含めていない。CIは `gradle/actions/setup-gradle` で直接 `gradle` コマンドを使う(8.7系)。
- CIはビルド前にリポジトリ直下の`debug.keystore.base64`を`~/.android/debug.keystore`へ復元してから`assembleDebug`する(全ウィジェット共通の固定 debug鍵)。これにより毎回同じ署名のAPKが出るので、実機側でアンインストールせずに上書きインストールできる。

## CI
`git push` すると `.github/workflows/build.yml` が `nothing-widget/` 配下の変更を検知して `gradle assembleDebug` を実行し、`app-debug.apk` をartifactとしてアップロードする。ビルドが通ることを変更の完了条件にする。

## 次にやること(未着手)
- 実機での動作確認(Essential SpaceがシェアシートにきちんとText宛先として出るか、システム標準アイコン`ic_menu_share`がNothing端末上でも見た目に問題ないか)
- 保存したメモの閲覧・削除UI(今は保存するだけで、見返す手段が無い)
