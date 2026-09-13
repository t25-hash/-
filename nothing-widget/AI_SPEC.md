# AI_SPEC — nothing-widget

このファイルはAIコーディングエージェント(Claude Code / Codex)がこのプロジェクトを変更する前に読む前提のスペックです。

## 現状
「パパっとメモして共有」ウィジェット。**Jetpack Glance**でウィジェット本体(`NothingWidget`)をタップすると、ダイアログ風の`QuickMemoActivity`が開き、短い文章を書いて「共有」を押すとOS標準のシェアシート(`ACTION_SEND`)に渡す。Essential SpaceやAIアプリなど、テキスト共有を受け取れる宛先はすべてシェアシートに自動で並ぶため、宛先ごとの個別対応はしていない。

## アーキテクチャ
- ウィジェット本体: `NothingWidget : GlanceAppWidget()`。`provideGlance` → `provideContent { Content() }` の現行API(古い`Content()`オーバーライド方式ではない)。黒背景・中央寄せの「M E M O / + 書いて共有」表示で、ウィジェット全体がタップ可能(`clickable(actionStartActivity<QuickMemoActivity>())`)。
- レシーバ: `NothingWidgetReceiver : GlanceAppWidgetReceiver()`。Manifestの `<receiver>` はこちらを指す。
- `QuickMemoActivity`: `AppCompatActivity`。`Theme.AppCompat.Dialog` + `windowSoftInputMode="stateVisible"` でダイアログ風に一瞬だけ開き、キーボードを自動表示する。EditTextに書いた内容を`Intent.ACTION_SEND`(`type="text/plain"`)で`Intent.createChooser`に渡すだけ。**ホーム画面ウィジェット自体はテキスト入力欄を持てない**(RemoteViews/Glance共通の制約)ため、この一瞬だけ開くActivityが唇一の入力経路。
- `res/layout/widget_nothing.xml` は Glance の描画が読み込まれる前の一瞬だけ表示されるプレースホルダ(`widget_info.xml`の`initialLayout`)。実際の表示は全て `Content()` 側で書く。
- `res/layout/activity_quick_memo.xml` は`QuickMemoActivity`のレイアウト(EditText + 共有/閉じるボタン)。

## 意図的にやらないこと
- 宛先(Essential Space・特定のAIアプリなど)をハードコードしたIntent送信は**しない**。OS標準の`ACTION_SEND` + `createChooser`だけを使い、宛先選択はユーザーとOSに委ねる。これにより端末やインストール状況が変わっても動き続ける。
- ウィジェット内にテキスト入力欄を持たせようとしない(技術的に不可能)。

## 制約・規約
- 言語: Kotlin 2.0.21。AGP 8.7.2。Compose CompilerはKotlinプラグイン経由(`org.jetbrains.kotlin.plugin.compose`)。
- `androidx.glance:glance-appwidget` / `androidx.glance:glance` は 1.1.1 で固定。フルのCompose UI(compose-ui/material)は使わないので追加しないこと(GlanceはRemoteViewsに変換される独自のComposable群を使う)。
- `androidx.appcompat:appcompat:1.7.0` を`QuickMemoActivity`用に追加済み。
- モジュール構成: `nothing-widget/app` の単一モジュール。
- `applicationId` / `namespace`: `com.t25hash.nothingwidget`。変更しない。
- `minSdk 26` / `compileSdk 34` / `targetSdk 34`。
- Gradle wrapperバイナリはリポジトリに含めていない。CIは `gradle/actions/setup-gradle` で直接 `gradle` コマンドを使う(8.7系)。

## CI
`git push` すると `.github/workflows/build.yml` が `nothing-widget/` 配下の変更を検知して `gradle assembleDebug` を実行し、`app-debug.apk` をartifactとしてアップロードする。ビルドが通ることを変更の完了条件にする。

## 次にやること(未着手)
- 実機での動作確認(Essential SpaceがシェアシートにきちんとText宛先として出るかは、Nothing Phone実機でしか確認できない)
