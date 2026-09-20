# AI_SPEC — rotation-control

このファイルはAIコーディングエージェント(Claude Code / Codex)がこのプロジェクトを変更する前に読む前提のスペックです。

## 現状
「他のアプリを強制的に縦画面で開かせる」ミニマルアプリ。トップ画面のトグル1つだけ。ONにするとフォアグラウンドサービスが透明オーバーレイウィンドウを出し続け、他のアプリがandroid:screenOrientation="landscape"で横固定していても縦で開かせる。

**アスペクト比は保持しない**(意図的)。対象アプリ自体が実際に縦レイアウトで再描画する方式なので、縦用UIを持たないアプリ(ゲーム等)では引き伸ばされたり崩れたりする可能性がある。画面をキャプチャして回転表示する方式(アスペクト保持可能)はタッチ座標変換の実績ある実装例が見つからず、コスト対効果で見送った経緯がある(下記参照)。

## 仕組み(AOSPソースで確認済み、推測ではない)
- `WindowManager.LayoutParams.screenOrientation`は公開API(`@hide`ではない)。`core/java/android/view/WindowManager.java`に定義あり。
- `WindowContainer#getOrientation()`(`services/core/java/com/android/server/wm/WindowContainer.java`)は自分の子コンテナを**Z順で上から**確認し、最初に明示的な向きを持つものを採用する。つまりフォアグラウンドアプリの上に乗るオーバーレイウィンドウが`screenOrientation`を指定していれば、そのアプリ自身の要求より優先される。
- `ForceOrientationService`が透明1x1・タッチ無視・フォーカス不可(`FLAG_NOT_TOUCHABLE` + `FLAG_NOT_FOCUSABLE`)の`TYPE_APPLICATION_OVERLAY`ウィンドウを`screenOrientation=PORTRAIT`で出すだけで実現している。root不要・隠しAPI不要・隠しAPIへのADB権限付与も不要。必要なのは通常のユーザー許可(`SYSTEM_ALERT_WINDOW`)のみ。
- 参考: https://github.com/curmudgeon-works/curmudgeon-rotation のREADMEが同じ手法("force" overlay)を明示しており、自体の向きを固定しているアプリ(HBO Max例)にも効くと確認できる。このリポジトリ自体はGPLv3で、このセッションからソースを読むことはできなかった(クロスティアaccess制限)のでコードの流用は一切していない。同じ公開手法をAOSPソースで独自に検証して新規実装したもの。

## アーキテクチャ
- `MainActivity`: トグルスイッチのみ。ONにする前に`Settings.canDrawOverlays()`を確認し、未許可なら`Settings.ACTION_MANAGE_OVERLAY_PERMISSION`を開く。許可済みなら`ForceOrientationService`をフォアグラウンドサービスとして起動。OFFは`stopService`。
- `ForceOrientationService`: `onStartCommand`でオーバーレイを追加し常騋通知を出す。通知には「解除」アクション(`ACTION_STOP`)付き — 黙って強制し続けてOFFの手段が無いのは悪質なので必ず即座に解除できるようにしている。`onDestroy`で必ずオーバーレイを除去(サービスがどう止まってもオーバーレイが残らないように)。
- `android:foregroundServiceType="specialUse"` + `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`: Android 14(API 34)以降はforeground serviceにtype指定が必須で、この用途に合う既定タイプがないため`specialUse`を使用。

## 意図的にやらないこと
- root化・ADB経由の特殊権限付与(`WRITE_SECURE_SETTINGS`等)は使わない。通常のストア配布でも成立する方法に限定している。
- 隠しAPI・リフレクション経由の`IActivityTaskManager`直接呼び出しなどは使わない(Androidバージョンごとに壊れやすいため)。
- アスペクト比を保ったまま回転表示する(画面キャプチャ+回転+タッチ座標変換)方式は、実績ある完成品OSSが見つからず、タッチ転送部分をゼロから自作する必要があるため保留中。必要になったら改めて検討。
- 通知の常騋表示と「解除」アクションは必ず残す(ユーザーが気づかずに他アプリを壊し続ける状況を避ける)。

## 制約・規約
- 言語: Kotlin 2.0.21。AGP 8.7.2。Composeは使用していない(シンプルなトグル画面のみなのでViewで十分)。
- `applicationId` / `namespace`: `com.t25hash.rotationcontrol`。
- `minSdk 26` / `compileSdk 34` / `targetSdk 34`。nothing-widgetと同一方針。
- CIはnothing-widgetと同じパターン(`gradle/actions/setup-gradle` + 共通`debug.keystore.base64`復元)。

## 次にやること(未着手)
- 実機での動作確認(横固定アプリで実際に縦表示になるか、レイアウト崩れの度合いはアプリによって当然異なる)
- アプリごとの自動適用(Accessibility Serviceでフォアグラウンドアプリを検知して自動ON/OFF)は未実装
- クイック設定タイルは未実装
