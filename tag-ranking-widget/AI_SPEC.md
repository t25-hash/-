# AI_SPEC — tag-ranking-widget

このファイルはAIコーディングエージェント(Claude Code / Codex)がこのプロジェクトを変更する前に読む前提のスペックです。

## 現状
QiitaとZennからタグ指定で記事を取得し、いいね数順に並べてホーム画面ウィジェットに出す。更新は1日おき(WorkManager)。

## ソースごとの扱い(重要)
- **Qiita**: 公式API(`qiita.com/api/v2/items`)。問題なし。クエリ `tag:<TAG> created:>=YYYY-MM-DD` で期間を切り、`likes_count`でこちら側ソート(APIに人気順ソートは無い)。未認証60回/時、IP単位。
- **Zenn**: 公式APIなし。`zenn.dev/api/articles?topicname=<TAG>&order=...` を想定しているが**正解のエンドポイントは未確認**(同じオーナーの別リポジトリ `t25-hash/bandersnach` でもキーワード検索用のエンドポイントで同様の問題にぶつかっており、候補 URL を順に試す方式を取っている)。`RankingFetcher.fetchZenn` も同じ方式：候補を順に試し、最初に中身のあるものを使う。**実機で確認して、正しいエンドポイントが分かったら候補リストをその1本に固定してよい**。
- **note**: 対象外。利用規約でスクレイピング・自動取得を明示的に禁止しており、RSSもクリエイター/マガジン単位でサイト全体のタグ集計には使えないため、**noteを追加しないこと**。

## アーキテクチャ
- `RankingFetcher`: Qiita/ZennへのHTTP GETとJSONパース。`HttpURLConnection` + `org.json`のみ(依存を増やさない)。
- `RankingStore`: 追うタグ一覧(カンマ区切り文字列)と取得結果のキャッシュをSharedPreferencesに保存。
- `RankingWorker`: `CoroutineWorker`。全タグ分Qiita+Zennを取得してキャッシュを書き換え、ウィジェットを更新。`schedule()`で10日おきの定期実行を登録、`runOnce()`で即時実行。
- `TagRankingWidget`: Glance。キャッシュを読んでリスト表示するだけ(ウィジェット自体はネットに行かない)。タップで記事URLをブラウザで開く。
- `MainActivity`: 追うタグの編集(カンマ区切り入力)と手動更新ボタン。

## 制約・規約
- 言語: Kotlin。AGP 8.7.2 / Kotlin 2.0.21(他プロジェクトと共通の実績ありバージョン)。
- `applicationId` / `namespace`: `com.t25hash.tagranking`。
- `minSdk 26` / `compileSdk 34` / `targetSdk 34`。
- Gradle wrapperバイナリはリポジトリに含めない。CIは`gradle/actions/setup-gradle`で直接`gradle`を使う(8.10.2)。

## 次にやること(未着手)
- 実機Zennエンドポイントの確認と固定化
- 取得失敗時のウィジェット上のエラー表示(現在は黙って空リストになるだけ)
