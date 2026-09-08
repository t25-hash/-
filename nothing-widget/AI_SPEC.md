# AI_SPEC — nothing-widget

このファイルはAIコーディングエージェント(Claude Code / Codex)がこのプロジェクトを変更する前に読む前提のスペックです。

## 現状
骨組みのみ。**Jetpack Glance**(ComposeベースのウィジェットAPI)で `NothingWidget`(`GlanceAppWidget`)を1つ定義し、固定文言を表示するだけ。実際の表示内容・操作は未実装。

## アーキテクチャ
- ウィジェット本体: `NothingWidget : GlanceAppWidget()`。`@Composable override fun Content()` にレイアウトを書く(XMLは使わない)。
- レシーバ: `NothingWidgetReceiver : GlanceAppWidgetReceiver()`。Manifestの `<receiver>` はこちらを指す。
- `res/layout/widget_nothing.xml` は Glance の描画が読み込まれる前の一瞬だけ表示されるプレースホルダ(`widget_info.xml`の`initialLayout`)。実際の表示は全て `Content()` 側で書く。

## 制約・規約
- 言語: Kotlin 2.0.21。AGP 8.7.2。Compose CompilerはKotlinプラグイン経由(`org.jetbrains.kotlin.plugin.compose`)。
- `androidx.glance:glance-appwidget` / `androidx.glance:glance` は 1.1.1 で固定。フルのCompose UI(compose-ui/material)は使わないので追加しないこと(GlanceはRemoteViewsに変換される独自のComposable群を使う)。
- モジュール構成: `nothing-widget/app` の単一モジュール。
- `applicationId` / `namespace`: `com.t25hash.nothingwidget`。変更しない。
- `minSdk 26` / `compileSdk 34` / `targetSdk 34`。
- Gradle wrapperバイナリはリポジトリに含めていない。CIは `gradle/actions/setup-gradle` で直接 `gradle` コマンドを使う(8.7系)。

## CI
`git push` すると `.github/workflows/build.yml` が `nothing-widget/` 配下の変更を検知して `gradle assembleDebug` を実行し、`app-debug.apk` をartifactとしてアップロードする。ビルドが通ることを変更の完了条件にする。

## 次にやること(未着手)
ウィジェットが実際に何を表示/操作するかはまだ未定。要件が決まったら `Content()` の中身を書き換えること。
