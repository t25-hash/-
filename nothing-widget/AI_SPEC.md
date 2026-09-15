# AI_SPEC — nothing-widget

このファイルはAIコーディングエージェント(Claude Code / Codex)がこのプロジェクトを変更する前に読む前提のスペックです。

## 現状
「パパっとメモして共有」ウィジェット。**Jetpack Glance**でウィジェット本体(`NothingWidget`)をタップすると、上部に黒帯だけがすっと出る`QuickMemoActivity`が開く。メモ帳アプリのように、テキスト入力欄がほぼ全面を占め、右上に3つのアイコン(AIログを見る・一括送信・共有)だけが乗るミニマルな見た目。閉じる専用ボタンは無く、システムの戻る操作で閉じる。ウィジェット本体は直近に保存したメモの内容を表示する(未保存なら「M E M O / + 書いて共有」のプレースホルダー)。

このアプリ自体もACTION_SEND(text/plain)の共有先として登録されており、Perplexity/ChatGPT等の共有ボタンから選ばれると、通常のメモ編集UIを出さずそのテキストをAIログ(Markdown、追記のみ)に記録して閉じる「受け口」としても動く。

ランチャーアイコンはNdot風のドットで「ME / MO」の2行を描いたアダプティブアイコン(ベクター)。

Discord Webhook直接投稿・カスタムドットフォントは試作したがどちらも不要と判断され削除済み。

## アーキテクチャ
- ウィジェット本体: `NothingWidget : GlanceAppWidget()`。`provideGlance`で`MemoStore.lastText(context)`を読み、`Content(memo)`に渡す。黒背景・中央寄せでMonospace表示、ウィジェット全体がタップ可能(`clickable(actionStartActivity<QuickMemoActivity>())`)。サイズはデフォルト1x1(`minWidth`/`minHeight` 40dp + `targetCellWidth`/`targetCellHeight` 1)、`resizeMode="horizontal|vertical"`で手動リサイズ可能。
- レシーバ: `NothingWidgetReceiver : GlanceAppWidgetReceiver()`。
- `QuickMemoActivity`: `AppCompatActivity`。`onCreate`の先頭で`intent.action == Intent.ACTION_SEND && intent.type == "text/plain"`を判定し、**true なら**受け取ったテキストを`AiLogStore.append()`で追記して`finish()`するだけ(通常のメモUIは出さない)。false(=ウィジェットタップ等の通常起動)なら通常のメモ編集UIを表示する。
  - 通常起動時: カスタムテーマ`Theme.QuickMemo`(`windowNoTitle` + `windowIsTranslucent` + `backgroundDimEnabled=false`)で、タイトルバーも背景の暗いオーバーレイも出さない。`window.setLayout(MATCH_PARENT, WRAP_CONTENT)` + `setGravity(Gravity.TOP)`で画面上部にだけ黒帯が乗る。`windowSoftInputMode="stateVisible"`でキーボードを自動表示。開いた時点で`MemoStore.lastText`を下書きとして復元する。
  - レイアウトは`FrameLayout`。`EditText`(`memo_input`, Monospace)が全面を占め、右上に`ImageButton`が3つ(`view_log_button`・`bulk_send_button`・`share_button`、システム標準アイコン+白tint)乗る。
  - `share_button`タップ: (1)テキストが前回保存時から変わっていれば`MemoStore.addMemo`で保存 + `NothingWidget().updateAll(context)`でウィジェットに即反映 (2)`Intent.ACTION_SEND`を`Intent.createChooser`で開く (3)**`finish()`しない**(複数宛先へ続けて共有できるように)。
  - `bulk_send_button`タップ: `TargetAppStore.SLOT_LABELS`(Perplexity/Genspark/DeepSeek/Grok/Gemini/ChatGPT/Claude/Copilot/Meta AI/Poe/Qwen/Kimi、計12個)のチェックリストダイアログを表示し、チェックされたラベルを`bulkSend()`へ渡す。
  - `view_log_button`タップ: `AiLogViewActivity`を開く。
  - **ホーム画面ウィジェット自体はテキスト入力欄を持てない**(RemoteViews/Glance共通の制約)ため、この画面が唯一の入力経路。ウィジェットの再描画も別プロセスのため、保存だけでは反映されず`GlanceAppWidget.updateAll()`(suspend)を明示的に呼ぶ必要がある。
- `MemoStore`: 書いたメモをSharedPreferences(JSON配列、`text` + `created_at`)に保存する。`lastText()`が直近のメモを返す(ウィジェット表示・下書き復元に使用)。
- `TargetAppStore`: 「一括送信」の送信先(`SLOT_LABELS`、現在12個)をラベル→`ComponentName`(package+class)で記憶する。**パッケージ名を決め打ちしない**(不確実で壊れやすいため)。候補を増やす場合は`SLOT_LABELS`にラベル文字列を足すだけでよい(初回選択・記憶方式なので、パッケージ名の調査は不要)。
- `ChosenComponentReceiver`: `Intent.createChooser(..., pendingIntent.intentSender)`のコールバックを受け、選ばれたアプリの`ComponentName`を`TargetAppStore`に保存するだけの`BroadcastReceiver`(`exported=false`、内部利用のみ)。
- `QuickMemoActivity.bulkSend(labels, text)`: 一括送信の本体。チェックされたラベルを「登録済み(`ComponentName`が確定していて、かつ`isLaunchable()`が通る)」と「未登録」に分ける。
  - 未登録が1つでも残っていれば**一括起動はせず**、`registerSlot()`でchooserを**1個だけ**開いて登録を進める(そのアプリへは実際に送信もされる)。全部登録済みになった次の回から一括送信が走る。
  - 全部登録済みなら、`ACTION_SEND`のIntent配列を**逆順**で組み立てて`startActivities()`を**1回だけ**呼ぶ。
- `QuickMemoActivity.isLaunchable(component)`: `PackageManager#getActivityInfo`で送信先が今も生きているか確認する。`startActivities()`は配列のうち1つでも解決できないと全体が例外になり、しかもスタック状態が未定義になるため、配列に入れる前に必ず通す。
- `QuickMemoActivity.registerSlot(label, text)`: `ChosenComponentReceiver`宛のPendingIntent(`FLAG_MUTABLE`必須)付きで`Intent.createChooser`を開き、選ばれたアプリを記憶しつつ実際にそのアプリへも送る(1アクションで「送る」と「覚える」を両立)。
- `AiLogStore`: 他アプリから共有されたテキストを`filesDir/ai_log.md`に`## yyyy-MM-dd HH:mm`見出し付きで**追記のみ**(上書きしない)で保存する。アプリ専用の内部ストレージなので他アプリ・他ユーザーからは直接見えない。`clear()`で本体を空にでき、`exportCopy()`で共有時点の内容を`filesDir/export/ai_log.md`へ複製する。
- `AiLogViewActivity`: `ai_log.md`の内容をそのまま(Markdownのソースとして)スクロール表示するだけの画面。`AppCompatActivity`なので`android:theme`に`Theme.AppCompat`系(ここでは`Theme.QuickMemo`を流用)が必須 — 付け忘れると起動時に必ずクラッシュする。右上の共有アイコンをタップすると、(1)`AiLogStore.exportCopy()`で複製を作り、(2)その複製を`FileProvider`経由で`ACTION_SEND`共有し(Obsidianに送る、メールする等)、(3)**直後に本体(`ai_log.md`)をクリアして**表示も空の状態に更新する。元ファイルではなく複製を共有するのは、共有シート側が後からファイルを読みに来てもクリア済みの空ファイルを掴まないようにするため。手動でログを消す手段(アプリ内クリアボタン等)は用意していない — エクスポート操作自体がクリアを兼ねる設計。
- `res/xml/file_paths.xml` + `AndroidManifest.xml`の`FileProvider`宣言(`${applicationId}.fileprovider`): `filesDir`配下(`export/`サブディレクトリ含む)をエクスポートする設定。
- ランチャーアイコン: `res/mipmap-anydpi-v26/ic_launcher.xml`(アダプティブアイコン) + `res/drawable/ic_launcher_foreground.xml`(ドットのベクター) + `res/values/colors.xml`の`ic_launcher_background`(黒)。`minSdk`が26なので全端末がアダプティブアイコンを使う=**PNGのmipmapは置かない**(バイナリをリポジトリに入れずに済む)。前景は5x7のドットフォントで「ME / MO」を12x15グリッドに組んだもので、アダプティブアイコンは中央72dpの円の内側しか表示が保証されないため、点灯しているドットが全てその円に収まる大きさに合わせてある。
- `res/layout/widget_nothing.xml` は Glance の描画が読み込まれる前の一瞬だけ表示されるプレースホルダ(`widget_info.xml`の`initialLayout`)。実際の表示は全て `Content()` 側で書く。

## 意図的にやらないこと
- 宛先(Essential Space・特定のAIアプリなど)をハードコードしたIntent送信は**しない**。「一括送信」のスロットも、パッケージ名の決め打ちではなく初回選択・記憶方式。
- **一括送信で`startActivity()`をループで連続に呼ばない**。以前はそうしていたが、2回目以降が「バックグラウンドからの起動」と判定されて無視されることがあり(Android 10以降の制限)、「たまにアプリが起動しない」原因になっていた。登録済み分は`startActivities()`1回にまとめ、未登録分は1回に1個だけchooserを出す方式にして、連続呼び出し自体を無くしている。
- **`startActivities()`に渡すIntentへ`FLAG_ACTIVITY_NEW_TASK`を付けない**。Activityから呼ぶ場合このフラグは不要で(必須なのは`ContextImpl`=Activity以外のContextから呼ぶ場合だけ)、付けると各アプリが別タスクに分かれて「戻るたびに次のアプリへ進む」流れが壊れる。
- ウィジェット内にテキスト入力欄を持たせようとしない(技術的に不可能。Nothing Playground(playground.nothing.tech)のウィジェットはNothing独自の非公開プラットフォームで動いており、標準AppWidget/RemoteViewsの制約を受けないため同じ見た目を再現できない)。
- **バックグラウンドでのクリップボード監視はしない**(Android 10以降、フォアグラウンドでないアプリはクリップボードを読めない制約があるため、原理的に不可能)。「コピーするたび自動記録」の代わりに、共有(ACTION_SEND)経由で受け取る方式にしている。
- 「共有」「一括送信」のたびに保存が重複しないよう、同一テキストの連続操作では2回目以降`MemoStore.addMemo`を呼ばない(直前保存済みテキストと比較)。AIログ側(`AiLogStore`)は逐次追記の性質上、重複排除はしていない。
- 画面を常時ボタンだらけにしない。閉じるボタンは常設せず、戻る操作に任せる。
- **他アプリへの送信を裏側で自動完了させることはできない**(Androidの仕様上の制約)。「一括送信」は各アプリの画面を順番に開くところまでで、そのアプリ内で送信を押すのはユーザー自身。同様に、AIログのエクスポート共有も相手アプリでの送信完了は検知できないため、共有ボタンを押した時点でクリアする近似的な挙動にしている。
- AIログ(`ai_log.md`)はデフォルトで外部から見えない場所に置く(ユーザーの選択)。共有可能な場所(Documents/Download等)に変更する場合は明示的な指示を待つこと。
- Discord Webhook直接投稿・カスタムドットフォント(`WebhookStore`/`DiscordWebhookPoster`/`ndot_47`等)は試作後に削除済み。再度追加する場合は明示的な指示を待つこと。
- `AppCompatActivity`を新設する時は必ず`android:theme`に`Theme.AppCompat`系を指定すること(`AiLogViewActivity`のクラッシュの原因になった)。

## 制約・規約
- 言語: Kotlin 2.0.21。AGP 8.7.2。Compose CompilerはKotlinプラグイン経由(`org.jetbrains.kotlin.plugin.compose`)。
- `androidx.glance:glance-appwidget` / `androidx.glance:glance` は 1.1.1 で固定。`Alignment`は`androidx.glance.layout.Alignment`、`FontFamily`は`androidx.glance.text.FontFamily`(いずれも`androidx.glance`直下ではない)。
- `androidx.appcompat:appcompat:1.7.0`、`org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1`(`updateAll()`がsuspend関数のため)を追加済み。`FileProvider`は`androidx.core:core-ktx`に含まれるので追加不要。
- モジュール構成: `nothing-widget/app` の単一モジュール。
- `applicationId` / `namespace`: `com.t25hash.nothingwidget`。変更しない。
- `minSdk 26` / `compileSdk 34` / `targetSdk 34`。
- ネットワーク権限は不要。`allowBackup="true"`。
- **`startActivities()`の仕様(AOSPのJavadocで確認済み。推測で書き換えないこと)**:
  - 「generally none of the activities except **the last** in the array will be created at this point, but rather will be created when the user first visits them (due to pressing back from the activity on top)」 — 配列の**最後**が最初に前面へ出る。だから`bulkSend()`は逆順で配列を組んでいる。
  - 「Because of this additional information, the `FLAG_ACTIVITY_NEW_TASK` launch flag is **not required**; if not specified, the new activity will be added to the task of the caller」(`Activity#startActivities`) — Activityから呼ぶなら不要。必須なのは`ContextImpl`側だけ。
  - 「This method throws `ActivityNotFoundException` if there was no Activity found for **any** given Intent. In this case **the state of the activity stack is undefined**」 — 1つでも解決できないと全体が失敗する。`isLaunchable()`の事前確認を外さないこと。
- `AndroidManifest.xml`の`<queries>`(ACTION_SEND / text/plain)は、Android 11以降のパッケージ可視性フィルタで`isLaunchable()`が誤って「存在しない」と判定しないために必要。消さないこと。
- `PendingIntent`にFLAG_MUTABLEを付ける箇所(`ChosenComponentReceiver`宛)は、Android 12+で`EXTRA_CHOSEN_COMPONENT`コールバックを受け取るために必須。外さないこと。
- `QuickMemoActivity`はACTION_SEND(text/plain)の`<intent-filter>`(`category.DEFAULT`必須)を持つ。ウィジェットタップ経由の起動(`actionStartActivity<QuickMemoActivity>()`)は明示Intentなのでフィルタの影響を受けず、`intent.action`もACTION_SENDにならないため両モードは衝突しない。
- Gradle wrapperバイナリはリポジトリに含めていない。CIは `gradle/actions/setup-gradle` で直接 `gradle` コマンドを使う(8.7系)。`android-actions/setup-android`ステップは使わない(2026年9月にGoogle側の廃止パッケージ絡みで壊れたため削除済み。GitHub提供ランナーに元々入っているSDKで足りる)。
- CIはビルド前にリポジトリ直下の`debug.keystore.base64`を`~/.android/debug.keystore`へ復元してから`assembleDebug`する(全ウィジェット共通の固定debug鍵)。これにより毎回同じ署名のAPKが出るので、実機側でアンインストールせずに上書きインストールできる。
- `versionCode`/`versionName`は`1`/`"0.1"`のまま固定。**どのビルドを入れても設定画面のバージョン表示が変わらない**ので、「最新APKが実機に入っているか」を見た目では判別できない点に注意(実際、1つ前のビルドで動作確認して不具合と誤認した事例がある)。判別が必要な場合はAPKを解析するか、バージョンを振る仕組みを入れること。

## CI
`git push` すると `.github/workflows/build.yml` が `nothing-widget/` 配下の変更を検知して `gradle assembleDebug` を実行し、`app-debug.apk` をartifactとしてアップロードする。ビルドが通ることを変更の完了条件にする。

## 次にやること(未着手)
- 実機での動作確認(一括送信で登録済みアプリが`startActivities()`でまとめて開くか、戻るたびに次のアプリへ進むか、未登録スロットの1個ずつ登録フローが自然か、ランチャーアイコンが円形マスクで欠けないか)
- 保存したメモの閲覧・削除UI(今は保存するだけで、見返す手段が無い)
- TargetAppStoreに登録したアプリを確認/登録し直すUI(今は「登録済みアプリが見つからない」場合しか選び直しが起きない)
