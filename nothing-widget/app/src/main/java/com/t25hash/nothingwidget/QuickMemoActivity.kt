package com.t25hash.nothingwidget

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ウィジェットタップで開く「パパっとメモ」画面。メモ帳アプリのように、
 * テキスト入力欄が画面のほぼ全部を占め、右上にアイコンだけを置く
 * ミニマルな見た目にしている。閉じる操作は専用ボタンを置かず、
 * システムの戻る操作に任せる。
 *
 * ホーム画面ウィジェット自体はテキスト入力欄を持てない(RemoteViews/Glanceの制約)ため、
 * 透過テーマでこのActivityを一瞬だけ開く。共有はOS標準のシェアシート(ACTION_SEND)に
 * 丸投げする。
 *
 * このActivity自体もACTION_SEND(text/plain)の受け口として登録されている。
 * Perplexity/ChatGPT等の共有ボタンからこのアプリが選ばれた場合は、通常の
 * メモ編集UIを出さず、そのテキストをそのままAiLogStoreに追記して閉じるだけの
 * 「AIログ取り」モードとして動く(バックグラウンドでのクリップボード監視は
 * Android 10以降できないため、共有経由で受け取る方式にしている)。
 *
 * 「一括送信」は、チェックしたAIアプリへ同じ文章を送る。登録済み(ComponentName確定)の
 * アプリはstartActivities()で1回のシステムコールにまとめる。未登録のアプリが残っている
 * 回は一括起動せず、chooserを1個だけ出して登録を済ませる。startActivity()を間を置かず
 * 連続で呼ぶと2回目以降が「バックグラウンドからの起動」と判定されて無視されることが
 * あるため(Android 10以降の制限)、連続呼び出し自体を無くす設計にしている。
 * 各アプリ内で送信を押すのはユーザー自身(裏側で自動完了させることはAndroidの仕様上不可能)。
 *
 * フォントはアプリ・ウィジェット共通でMonospace(等幅)に統一している。
 */
class QuickMemoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                AiLogStore.append(this, sharedText)
                Toast.makeText(this, "AIログに追記しました", Toast.LENGTH_SHORT).show()
            }
            finish()
            return
        }

        setContentView(R.layout.activity_quick_memo)

        // 画面全体を覆う暗いオーバーレイなしで、上部にだけ黒帯が出るようにする。
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.TOP)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        val memoInput = findViewById<EditText>(R.id.memo_input)
        val shareButton = findViewById<ImageButton>(R.id.share_button)
        val bulkSendButton = findViewById<ImageButton>(R.id.bulk_send_button)
        val viewLogButton = findViewById<ImageButton>(R.id.view_log_button)

        memoInput.typeface = Typeface.MONOSPACE

        // 直近に保存したメモを下書きとして復元する(ウィジェット本体にも同じ内容が出ている)。
        var lastSavedText: String? = MemoStore.lastText(this)
        memoInput.setText(lastSavedText ?: "")
        memoInput.setSelection(memoInput.text.length)
        memoInput.requestFocus()

        fun saveIfChanged(text: String) {
            if (text.isNotEmpty() && text != lastSavedText) {
                MemoStore.addMemo(this, text)
                lastSavedText = text
                // Glance公式の更新API。ウィジェットの再描画は別プロセスなので、
                // この呼び出しなしでは保存内容が画面上に反映されない。
                CoroutineScope(Dispatchers.Main).launch {
                    NothingWidget().updateAll(this@QuickMemoActivity)
                }
            }
        }

        shareButton.setOnClickListener {
            val text = memoInput.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            saveIfChanged(text)

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(sendIntent, null))
            // ここでfinish()しない: 同じメモを複数の宛先へ続けて共有できるように
            // するため。閉じるのは戻る操作で。
        }

        bulkSendButton.setOnClickListener {
            val text = memoInput.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "メモが空です", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveIfChanged(text)

            val labels = TargetAppStore.SLOT_LABELS.toTypedArray()
            val checked = BooleanArray(labels.size)
            AlertDialog.Builder(this)
                .setTitle("一括送信するアプリを選択")
                .setMultiChoiceItems(labels, checked) { _, which, isChecked -> checked[which] = isChecked }
                .setPositiveButton("送信") { _, _ ->
                    bulkSend(labels.filterIndexed { index, _ -> checked[index] }, text)
                }
                .setNegativeButton("キャンセル", null)
                .show()
        }

        viewLogButton.setOnClickListener {
            startActivity(Intent(this, AiLogViewActivity::class.java))
        }
    }

    /**
     * 一括送信。全スロットが登録済みならstartActivities()1回で送る。
     * 未登録が残っている回は一括起動せず、chooserを1個だけ出して登録を済ませる
     * (chooserをN個連続で開くと2個目以降が無視されることがあるため)。
     */
    private fun bulkSend(labels: List<String>, text: String) {
        val resolved = mutableListOf<ComponentName>()
        val unresolved = mutableListOf<String>()
        labels.forEach { label ->
            val component = TargetAppStore.component(this, label)
            if (component != null && isLaunchable(component)) resolved += component else unresolved += label
        }

        if (unresolved.isNotEmpty()) {
            val label = unresolved.first()
            Toast.makeText(this, "未登録: ${label}を登録します(残り${unresolved.size}件)", Toast.LENGTH_SHORT).show()
            registerSlot(label, text)
            return
        }

        if (resolved.isEmpty()) return

        // startActivities()は配列の最後だけが即座に前面へ出て、残りは戻る操作で順に現れる。
        // なので逆順で渡すと、戻るたびに次のアプリへ進む流れになる。
        // FLAG_ACTIVITY_NEW_TASKは付けない: Activityから呼ぶ場合は不要で、付けると各アプリが
        // 別タスクに分かれて上記の流れが壊れる。
        val intents = resolved.reversed().map { component ->
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                setComponent(component)
            }
        }.toTypedArray()

        try {
            startActivities(intents)
        } catch (e: ActivityNotFoundException) {
            // 生存確認の直後にアンインストールされた等。
            Toast.makeText(this, "起動できないアプリがありました", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * startActivities()は配列のうち1つでも解決できないと全体が例外になり、
     * しかもスタックの状態が未定義になる(Context#startActivitiesのJavadoc)。
     * そのため配列へ入れる前に1件ずつ生存確認する。
     */
    private fun isLaunchable(component: ComponentName): Boolean = try {
        packageManager.getActivityInfo(component, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    /** 未登録スロットを1個だけchooserで解決させる(選ばれた先はChosenComponentReceiverが保存する)。 */
    private fun registerSlot(label: String, text: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val receiverIntent = Intent(this, ChosenComponentReceiver::class.java).apply {
            putExtra(ChosenComponentReceiver.EXTRA_SLOT_LABEL, label)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            label.hashCode(),
            receiverIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        startActivity(
            Intent.createChooser(sendIntent, "${label}として使うアプリを選択", pendingIntent.intentSender),
        )
    }
}
