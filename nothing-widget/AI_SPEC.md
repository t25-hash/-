# AI_SPEC — nothing-widget

このファイルはAIコーディングエージェント(Claude Code / Codex)がこのプロジェクトを変更する前に読む前提のスペックです。

## 現状
骨組みのみ。ホーム画面に置ける最小のAndroidウィジェット(`NothingWidgetProvider`)が1つあり、固定文言を表示するだけで実際の機能は未実装。ウィジェットの中身(何を表示・操作するか)はまだ決まっていない。

## 制約・規約
- 言語: Kotlin。Java不可。
- モジュール構成: `nothing-widget/app` の単一モジュール。新しい機能もこの中に追加する(モジュール分割はまだ不要)。
- `applicationId` / `namespace`: `com.t25hash.nothingwidget`。変更しない。
- `minSdk 26` / `compileSdk 34` / `targetSdk 34`。上げる場合は `.github/workflows/build.yml` のAndroid SDKセットアップとの整合を確認すること。
- Gradle wrapperバイナリ(`gradlew` / `gradle-wrapper.jar`)はリポジトリに含めていない。CIは `gradle/actions/setup-gradle` で直接 `gradle` コマンドを使ってビルドしている。ローカルでも同様に素の `gradle` (8.7系)を使うか、必要なら `gradle wrapper` を自分の環境で生成すること。
- ウィジェットのレイアウトは `app/src/main/res/layout/widget_nothing.xml`、更新ロジックは `NothingWidgetProvider.onUpdate` に置く。

## CI
`git push` すると `.github/workflows/build.yml` が `nothing-widget/` 配下の変更を検知して `gradle assembleDebug` を実行し、`app-debug.apk` をワークフローのartifactとしてアップロードする。ビルドが通ることを変更の完了条件にする。

## 次にやること(未着手)
ウィジェットが実際に何を表示/操作するかはまだ未定。要件が決まったらこのファイルに追記してから実装に入ること。
