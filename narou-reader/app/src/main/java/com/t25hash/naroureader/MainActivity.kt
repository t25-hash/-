package com.t25hash.naroureader

import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val allowedHosts = listOf(
        "syosetu.com",
        "kakuyomu.jp",
    )

    private val startUrl = "https://ncode.syosetu.com/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        overridePendingTransition(0, 0)

        val webView = findViewById<WebView>(R.id.webview)
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

        webView.loadUrl(startUrl)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        val webView = findViewById<WebView>(R.id.webview)
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
