# AI_SPEC — discord-channel-widget

このファイルはAIコーディングエージェント(Claude Code / Codex)がこのプロジェクトを変更する前に読む前提のスペックです。

## 現状
自分が管理/参加しているDiscordサーバーの特定チャンネルの最新メッセージをホーム画面ウィジェットに表示する。更新は30分おき(WorkManager)。

## 対応範囲(重要)
- **サーバーのチャンネルのみ対応。個人のDMは対応しない。**
  - Discordの公式Bot APIは、Bot自身が招待されたサーバーのチャンネルしか読めない。他人同士の個人 DMは原理的に読めない。
  - 個人 DMを読むには「セルフボット」(自分のユーザートークンをbotのように自動操作すること)が必要になるが、これはDiscordの利用規約で明確に禁止されており、アカウント停止のリスクがあるため**絶対に実装しないこと**。
- 使うのは公式REST API (`GET /channels/{channel.id}/messages`, `Authorization: Bot <TOKEN>`) のみ。

## セットアップ前提(ユーザー側で事前に必要な作業)
1. https://discord.com/developers/applications でアプリケーションを作成し、Botを追加してBot Tokenを発行
2. Bot設定で **Message Content Intent** を有効化(無効だとメッセージ本文が空で返ってくることがある)
3. OAuth2 URL Generatorで `bot` スコープ + `View Channel` / `Read Message History` 権限を付与したURLを発行し、対象サーバーに招待
4. アプリの設定画面でBot Token・チャンネルID・(任意で)サーバーIDを入力して保存

## セキュリティ上の扱い
- Botトークンはアプリ専用のSharedPreferences(平文)に保存する。他アプリからは読めない(root化されていない前提)。
- `AndroidManifest.xml` で `android:allowBackup="false"` にしている。adb backupやクラウド自動バックアップ経由でトークンが平文のまま流出するのを防ぐため。
- トークンが流出した場合はDeveloper Portalで即座にリセットすること。

## アーキテクチャ
- `DiscordFetcher`: Discord REST APIへのHTTP GETとJSONパース。`HttpURLConnection` + `org.json`のみ(依存を増やさない)。取得したメッセージは古い→新しい順に並べ替える(APIは新しい順で返すため)。
- `DiscordStore`: Botトークン・チャンネルID・サーバーID・取得結果のキャッシュをSharedPreferencesに保存。
- `DiscordWorker`: `CoroutineWorker`。チャンネルの最新メッセージを取得してキャッシュを書き換え、ウィジェットを更新。`schedule()`で30分おきの定期実行を登録、`runOnce()`で即時実行。
- `DiscordChannelWidget`: Glance。キャッシュを読んでリスト表示するだけ(ウィジェット自体はネットに行かない)。サーバーIDが設定されていればヘッダータップでDiscordアプリ/Webの該当チャンネルを開く。
- `MainActivity`: Botトークン/チャンネルID/サーバーIDの入力と手動更新ボタン。保存・更新後は`WorkInfo`を監視して完了時にステータス表示(最終更新時刻・件数)を更新する(tag-ranking-widgetで「保存/更新が効いているか分からない」という問題が出たための対策)。

## 制約・規約
- 言語: Kotlin。AGP 8.7.2 / Kotlin 2.0.21(他プロジェクトと共通の実績ありバージョン)。
- `applicationId` / `namespace`: `com.t25hash.discordwidget`。
- `minSdk 26` / `compileSdk 34` / `targetSdk 34`。
- Gradle wrapperバイナリはリポジトリに含めない。CIは`gradle/actions/setup-gradle`で直接`gradle`を使う(8.10.2)。

## 次にやること(未着手)
- 実機での動作確認(このリポジトリのCI環境からはDiscordのトークンを使った実通信テストができないため)
- 取得失敗時(トークン無効・権限不足・レート制限など)のウィジェット上のエラー表示(現在は黙って空リストになるだけ)
