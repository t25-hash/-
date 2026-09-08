package com.t25hash.naroureader

/**
 * サイトの本文コンテナに縦書きCSSを注入するJSを組み立てる。
 * 広告要素には一切手を付けない(AI_SPEC.md参照)。
 */
object ReaderScript {

    fun forUrl(url: String): String {
        val selector = when {
            url.contains("syosetu.com") -> ".p-novel__body, #novel_honbun"
            url.contains("kakuyomu.jp") -> ".widget-episodeBody"
            else -> return ""
        }

        val css = """
            html, body { margin:0; padding:0; background:#f7f3ea; }
            $selector {
              writing-mode: vertical-rl;
              text-orientation: mixed;
              text-combine-upright: digits 2;
              height: calc(100vh - 24px);
              width: auto;
              overflow-x: auto; overflow-y: hidden;
              padding: 12px; box-sizing: border-box;
              font-family: "Noto Serif JP", serif;
              font-size: 18px; line-height: 2; color:#222;
              scroll-behavior: auto;
            }
            $selector * { animation:none !important; transition:none !important; }
        """.trimIndent().replace("\n", " ")

        return """
            (function () {
              var s = document.querySelector(${jsQuote(selector)});
              if (!s) return;
              var style = document.createElement('style');
              style.textContent = ${jsQuote(css)};
              document.head.appendChild(style);
              var pageWidth = window.innerWidth;
              s.addEventListener('click', function (e) {
                s.scrollLeft += (e.clientX < pageWidth / 2) ? -pageWidth : pageWidth;
              });
            })();
        """.trimIndent()
    }

    private fun jsQuote(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
