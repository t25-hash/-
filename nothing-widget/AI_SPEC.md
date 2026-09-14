# AI_SPEC — nothing-widget

このファイルはAIコーディングエージェント(Claude Code / Codex)がこのプロジェクトを変更する前に読む前提のスペックです。

## 現状
「パパっとメモして共有」ウィジェット。**Jetpack Glance**でウィジェット本体(`NothingWidget`)をタップすると、上部に黒帯だけがすっと出る`QuickMemoActivity`が開く。メモ帳アプリのように、テキスト入力欄がほぼ全面を占め、右上に2つのアイコン(一括送信・共有)だけが乗るミニマルな見た目。閉じる専用ボタンは無く、システムの戻る操作で閉じる。ウィジェット本体は直近に保存したメモの内容を表示する(未保存なら「M E M O / + 書いて共有」のプレースホルダー)。

Discord Webhook直接投稿・カスタムドットフォントは試作したがどちらも不要と判断され削除済み。

## アーキテクチャ
- ウィジェット本体: `NothingWidget : GlanceAppWidget()`。`provideGlance`で`MemoStore.lastText(context)`を読み、`Content(memo)`に渡す。黒背景・中央寄せでMonospace表示、ウィジェット全体がタップ可能(`clickable(actionStartActivity<QuickMemoActivity>())`)。サイズはデフォルト1x1(`minWidth`/`minHeight` 40dp + `targetCellWidth`/`targetCellHeight` 1)、`resizeMode="horizontal|vertical"`で手動リサイズ可能。
- レシーバ: `NothingWidgetReceiver : GlanceAppWidgetReceiver()`。
- `QuickMemoActivity`: `AppCompatActivity`。カスタムテーマ`Theme.QuickMemo`(`windowNoTitle` + `windowIsTranslucent` + `backgroundDimEnabled=false`)で、タイトルバーも背景の暗いオーバーレイも出さない。`onCreate`で`window.setLayout(MATCH_PARENT, WRAP_CONTENT)` + `setGravity(Gravity.TOP)`にして、画面上部にだけ黒帯が乗るようにしている。`windowSoftInputMode="stateVisible"`でキーボードを自動表示。開いた時点で`MemoStore.lastText`を下書きとして復元する。
  - レイアウトは`FrameLayout`。`EditText`(`memo_input`, Monospace)が全面を占め、右上に`ImageButton`が2つ(`bulk_send_button`・`share_button`、システム標準アイコン+白 tint)乗る。
  - `share_button`タップ: (1)テキストが前回保存時から変わっていれば`MemoStore.addMemo`で保存 + `NothingWidget().updateAll(context)`でウィジェットに即反映 (2)`Intent.ACTION_SEND`を`Intent.createChooser`で開く (3)**`finish()`しない**(複数宛先へ続けて共有できるように)。
  - `bulk_send_button`タップ: `TargetAppStore.SLOT_LABELS`(Perplexity/Genspark/DeepSeek/Grok)のチェックリストダイアログを表示。チェックした分だけ`sendToSlot()`を順に呼ぶ。
  - **ホーム画面ウィジェット自体はテキスト入力欄を持てない**(RemoteViews/Glance共通の制約)ため、この画面が唇一の入力経路。ウィジェットの再描画も別プロセスのため、保存だけでは反映されず`GlanceAppWidget.updateAll()`(suspend)を明示的に呼ぶ必要がある。
- `MemoStore`: 書いたメモをSharedPreferences(JSON配列、`text` + `created_at`)に保存する。`lastText()`が直近のメモを返す(ウィジェット表示・下書き復元に使用)。
- `TargetAppStore`: 「一括送信」の送信先(Perplexity/Genspark/DeepSeek/Grok)をラベル→`ComponentName`(package+class)で記憶する。**パッケージ名を決め打ちしない**(不確実で壊れやすいため)。
- `ChosenComponentReceiver`: `Intent.createChooser(..., pendingIntent.intentSender)`のコールバックを受け、選ばれたアプリの`ComponentName`を`TargetAppStore`に保存するだけの`BroadcastReceiver`(`exported=false`、内部利用のみ)。
- `QuickMemoActivity.sendToSlot(label, text)`: 登録済みならその`ComponentName`へ`ACTION_SEND`を直接`startActivity`。未登録(または登録先が見つからない=アンインストール済み)なら、`ChosenComponentReceiver`宛のPendingIntent(`FLAG_MUTABLE`必須)付きで`Intent.createChooser`を開き、選ばれたアプリを記憶しつつ実際にそのアプリへも送る(1アクションで「送る」と「覚える」を両立)。
- `res/layout/widget_nothing.xml` は Glance の描画が読み込まれる前の一瞬だけ表示されるプレースホルダ(`widget_info.xml`の`initialLayout`)。実際の表示は全て `Content()` 側で書く。

## 意図的にやらないこと
- 宛先(Essential Space・特定のAIアプリなど)をハードコードしたIntent送信は**しない**。「一括送信」の4スロットも、パッケージ名の決め打ちではなく初回選択・記憶方式。
- ウィジェット内にテキスト入力欄を持たせようとしない(技術的に不可能。Nothing Playground(playground.nothing.tech)のウィジェットはNothing独自の非公開プラットフォームで動いており、標準AppWidget/RemoteViewsの制約を受けないため同じ見た目を再現できない)。
- 「共有」「一括送信」のたびに保存が重複しないよう、同一テキストの連続操作では2回目以降`MemoStore.addMemo`を呼ばない(直前保存済みテキストと比較)。
- 画面を常時ボタンだらけにしない。閉じるボタンは常設せず、戻る操作に任せる。
- **他アプリへの送信を裏側で自動完了させることはできない**(Androidの仕様上の制約)。「一括送信」は各アプリの画面を順番に開くところまでで、そのアプリ内で送信を押すのはユーザー自身。
- Discord Webhook直接投稿・カスタムドットフォント(`WebhookStore`/`DiscordWebhookPoster`/`ndot_47`等)は試作後に削除済み。再度追加する場合は明示的な指示を待つこと。

## 制約・規約
- 言語: Kotlin 2.0.21。AGP 8.7.2。Compose CompilerはKotlinプラグイン経由(`org.jetbrains.kotlin.plugin.compose`)。
- `androidx.glance:glance-appwidget` / `androidx.glance:glance` は 1.1.1 で固定。`Alignment`は`androidx.glance.layout.Alignment`、`FontFamily`は`androidx.glance.text.FontFamily`(いずれも`androidx.glance`直下ではない)。
- `androidx.appcompat:appcompat:1.7.0`、`org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1`(`updateAll()`がsuspend関数のため)を追加済み。
- モジュール構成: `nothing-widget/app` の単一モジュール。
- `applicationId` / `namespace`: `com.t25hash.nothingwidget`。変更しない。
- `minSdk 26` / `compileSdk 34` / `targetSdk 34`。
- ネットワーク権限は不要。`allowBackup="true"`。
- `PendingIntent`にFLAG_MUTABLEを付ける箇所(`ChosenComponentReceiver`宛)は、Android 12+で`EXTRA_CHOSEN_COMPONENT`コールバックを受け取るために必須。外さないこと。
- Gradle wrapperバイナリはリポジトリに含めていない。CIは `gradle/actions/setup-gradle` で直接 `gradle` コマンドを使う(8.7系)。
- CIはビルド前にリポジトリ直下の`debug.keystore.base64`を`~/.android/debug.keystore`へ復元してから`assembleDebug`する(全ウィジェット共通の固定 debug鍵)。これにより毎回同じ署名のAPKが出るので、実機側でアンインストールせずに上書きインストールできる。

## CI
`git push` すると `.github/workflows/build.yml` が `nothing-widget/` 配下の変更を検知して `gradle assembleDebug` を実行し、`app-debug.apk` をartifactとしてアップロードする。ビルドが通ることを変更の完了条件にする。

## 次にやること(未着手)
- 実機での動作確認(Perplexity/Genspark/DeepSeek/Grokが実際にACTION_SEND(text/plain)を受け取れるか、一括送信で各アプリを順に開いた時の戻り挙動)
- 保存したメモの閲覧・削除UI(今は保存するだけで、見返す手段が無い)
- TargetAppStoreに登録したアプリを確認/登録し直すUI(今は「登録済みアプリが見つからない」場合しか選び直しが起きない)
