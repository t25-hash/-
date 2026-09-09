package com.t25hash.naroureader

import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "bookmark_url"
    }

    private val allowedHosts = listOf(
        "syosetu.com",
        "kakuyomu.jp",
    )

    private val startUrl = "https://ncode.syosetu.com/"

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        overridePendingTransition(0, 0)

        webView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest,
            ): Boolean {
                val host = request.url.host ?: return true
                val allowed = allowedHosts.any { host == it || host.endsWith(".$it") }
                return !allowed
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                val script = ReaderScript.forUrl(url)
                if (script.isNotEmpty()) {
                    view.evaluateJavascript(script, null)
                }
            }
        }

        val omnibox = findViewById<EditText>(R.id.omnibox)
        val goButton = findViewById<Button>(R.id.go_button)
        val bookmarkButton = findViewById<Button>(R.id.bookmark_button)

        val submit = {
            val input = omnibox.text.toString().trim()
            if (input.isNotEmpty()) {
                webView.loadUrl(resolveInput(input))
            }
        }

        goButton.setOnClickListener { submit() }
        omnibox.setOnEditorActionListener { _, actionId, event ->
            val isGo = actionId == EditorInfo.IME_ACTION_GO ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            if (isGo) submit()
            isGo
        }

        bookmarkButton.setOnClickListener {
            val url = webView.url
            if (url != null) {
                BookmarkStore.add(this, url, webView.title ?: url)
                lifecycleScope.launch {
                    BookshelfWidget().updateAll(this@MainActivity)
                }
            }
        }

        val initialUrl = intent.getStringExtra(EXTRA_URL) ?: startUrl
        webView.loadUrl(initialUrl)
    }

    private fun resolveInput(input: String): String {
        return when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            allowedHosts.any { input.contains(it) } -> "https://$input"
            else -> {
                // キーワードはなろうの検索にフォールバック
                // (カクヨムの検索URLパターンは未確認のため未対応。
                //  カクヨムを開きたい場合はURLを直接貼り付けること。)
                val encoded = URLEncoder.encode(input, "UTF-8")
                "https://yomou.syosetu.com/search.php?word=$encoded"
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
