# AI_SPEC — narou-reader

このファイルはAIコーディングエージェント(Claude Code / Codex)がこのプロジェクトを変更する前に読む前提のスペックです。

## 現状
なろう/カクヨムの実ページをWebViewで開き、本文コンテナに縦書きCSSを注入して読む、薄いアプリ。本文は一切保存・複製しない(毎回サイトから読む)。上部にURL/キーワード入力欄(omnibox)あり。

## 絶対に守ること(規約上の制約)
- なろう公式が「本文を機械的に取得しアプリで表示・ダウンロードする行為は違反」と明記している。**本文をローカルに保存・キャッシュする実装を追加しない**こと。
- 広告要素にはCSSを一切当てない(縮小・非表示どちらも禁止)。AdSenseの規約上、広告を視覚的に縮小してもインプレッションはカウントされ続けるため、無効なインプレッションを生む禁止行為になる。**広告関連のセレクタや`display:none`/`transform:scale`をads要素に書かない**こと。
- アプリ内で許可するドメインは `syosetu.com` / `ncode.syosetu.com` / `kakuyomu.jp` のみ(`MainActivity.allowedHosts`)。他ドメインへの遷移はブロックする。

## アーキテクチャ
- `MainActivity`: 上部omnibox(URL入力・キーワード検索入力兵用)+ WebViewの単一Activity。`shouldOverrideUrlLoading`で許可ドメイン外への遷移をブロック。
  - `resolveInput()`: `http(s)://`で始まればそのままURLとして読み込む。許可ホスト名を含む文字列なら`https://`を補って読み込む。それ以外はなろうのキーワード検索(`yomou.syosetu.com/search.php?word=`)にフォールバック。
  - **カクヨムのキーワード検索URLパターンは未確認のため未実装**。カクヨムを開きたい場合はユーザーがURLを直接貼り付ける運用。確認できたらここに追加すること。
- `ReaderScript`: サイトのURLに応じて注入するJS(CSS文字列)を組み立てる。本文セレクタ:
  - なろう: `.p-novel__body, #novel_honbun`
  - カクヨム: `.widget-episodeBody`
- 縦書き実装は `writing-mode: vertical-rl` + 固定`height`のみ。CSS columnsは使わない(ブラウザが自動で右→左に段組みする)。
- ページ送りはタップ位置に応じた`scrollLeft`の瞬時ジャンプ(`scroll-behavior: smooth`を使わないことでアニメーション無しを実現)。

## 制約・規約
- 言語: Kotlin。AGP 8.7.2 / Kotlin 2.0.21(nothing-widgetと共通の実績あるバージョン)。
- `applicationId` / `namespace`: `com.t25hash.naroureader`。
- `minSdk 26` / `compileSdk 34` / `targetSdk 34`。
- Gradle wrapperバイナリはリポジトリに含めない。CIは`gradle/actions/setup-gradle`で直接`gradle`コマンドを使う(8.10.2)。

## 次にやること(未着手)
- カクヨムの検索URLパターンを確認してomniboxの検索先に含める
- サイト側のHTML構造は変わりうるので、セレクタが見つからない場合のフォールバック表示
- ブックマーク機能(読んでる話のURLを保存 — これはメタ情報なので規約上問題ない)
- フォントサイズ・行間のユーザー設定
