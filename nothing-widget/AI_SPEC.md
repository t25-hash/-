# AI_SPEC — nothing-widget

このファイルはAIコーディングエージェント(Claude Code / Codex)がこのプロジェクトを変更する前に読む前提のスペックです。

## 現状
「パパっとメモして共有」ウィジェット。**Jetpack Glance**でウィジェット本体(`NothingWidget`, 2x2)をタップすると、上部に黒帯だけがすっと出る`QuickMemoActivity`が開き、短い文章を書いて「共有」を押すとOS標準のシェアシート(`ACTION_SEND`)に渡す。Essential SpaceやAIアプリなど、テキスト共有を受け取れる宛先はすべてシェアシートに自動で並ぶため、宛先ごとの個別対応はしていない。書いたメモは`MemoStore`に保存され、同じメモを複数の宛先へ続けて共有できる(共有してもActivityは閉じない)。

## アーキテクチャ
- ウィジェット本体: `NothingWidget : GlanceAppWidget()`。`provideGlance` → `provideContent { Content() }` の現行API(古い`Content()`オーバーライド方式ではない)。黒背景・中央寄せの「M E M O / + 書いて共有」表示で、ウィジェット全体がタップ可能(`clickable(actionStartActivity<QuickMemoActivity>())`)。サイズは2x2(`minWidth`/`minHeight` 110dp + `targetCellWidth`/`targetCellHeight` 2)。
- レシーバ: `NothingWidgetReceiver : GlanceAppWidgetReceiver()`。Manifestの `<receiver>` はこちらを指す。
- `QuickMemoActivity`: `AppCompatActivity`。カスタムテーマ`Theme.QuickMemo`(`windowNoTitle` + `windowIsTranslucent` + `backgroundDimEnabled=false`)で、タイトルバーも背景の暗いオーバーレイも出さない。`onCreate`で`window.setLayout(MATCH_PARENT, WRAP_CONTENT)` + `setGravity(Gravity.TOP)`にして、画面上部にだけ黒帯が乗るようにしている。`windowSoftInputMode="stateVisible"`でキーボードを自動表示。
  - 「共有」を押すと: (1)テキストが前回保存時から変わっていれば`MemoStore.addMemo`で保存 (2)`Intent.ACTION_SEND`(`type="text/plain"`)を`Intent.createChooser`で開く (3)**`finish()`しない**。同じメモをEssential Space・AIアプリなど複数の宛先へ続けて共有する運用のため、閉じるのは「閉じる」ボタンを押した時だけ。
  - **ホーム画面ウィジェット自体はテキスト入力欄を持てない**(RemoteViews/Glance共通の制約)ため、この画面が唇一の入力経路。
- `MemoStore`: 書いたメモをSharedPreferences(JSON配列、`text` + `created_at`)に保存するだけの単純なストア。閲覧用のUIはまだ無い(保存のみ)。
- `res/layout/widget_nothing.xml` は Glance の描画が読み込まれる前の一瞬だけ表示されるプレースホルダ(`widget_info.xml`の`initialLayout`)。実際の表示は全て `Content()` 側で書く。
- `res/layout/activity_quick_memo.xml` は`QuickMemoActivity`のレイアウト(EditText + 共有/閉じるボタン)。上部パディングを多めに取り、ステータスバー直下に張り付かないようにしている。

## 意図的にやらないこと
- 宛先(Essential Space・特定のAIアプリなど)をハードコードしたIntent送信は**しない**。OS標準の`ACTION_SEND` + `createChooser`だけを使い、宛先選択はユーザーとOSに委ねる。これにより端末やインストール状況が変わっても動き続ける。
- ウィジェット内にテキスト入力欄を持たせようとしない(技術的に不可能)。
- 「共有」のたびに保存が重複しないよう、同一テキストの連続共有では2回目以降`MemoStore.addMemo`を呼ばない(直前保存済みテキストと比較)。

## 制約・規約
- 言語: Kotlin 2.0.21。AGP 8.7.2。Compose CompilerはKotlinプラグイン経由(`org.jetbrains.kotlin.plugin.compose`)。
- `androidx.glance:glance-appwidget` / `androidx.glance:glance` は 1.1.1 で固定。フルのCompose UI(compose-ui/material)は使わないので追加しないこと(GlanceはRemoteViewsに変換される独自のComposable群を使う)。`Alignment`は`androidx.glance.layout.Alignment`(`androidx.glance.Alignment`ではない)。
- `androidx.appcompat:appcompat:1.7.0` を`QuickMemoActivity`用に追加済み。
- モジュール構成: `nothing-widget/app` の単一モジュール。
- `applicationId` / `namespace`: `com.t25hash.nothingwidget`。変更しない。
- `minSdk 26` / `compileSdk 34` / `targetSdk 34`。
- Gradle wrapperバイナリはリポジトリに含めていない。CIは `gradle/actions/setup-gradle` で直接 `gradle` コマンドを使う(8.7系)。
- CIはビルド前にリポジトリ直下の`debug.keystore.base64`を`~/.android/debug.keystore`へ復元してから`assembleDebug`する(全ウィジェット共通の固定 debug鍵)。これにより毎回同じ署名のAPKが出るので、実機側でアンインストールせずに上書きインストールできる。

## CI
`git push` すると `.github/workflows/build.yml` が `nothing-widget/` 配下の変更を検知して `gradle assembleDebug` を実行し、`app-debug.apk` をartifactとしてアップロードする。ビルドが通ることを変更の完了条件にする。

## 次にやること(未着手)
- 実機での動作確認(Essential SpaceがシェアシートにきちんとText宛先として出るかは、Nothing Phone実機でしか確認できない)
- 保存したメモの閲覧・削除UI(今は保存するだけで、見返す手段が無い)
