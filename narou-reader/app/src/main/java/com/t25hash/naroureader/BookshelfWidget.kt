package com.t25hash.naroureader

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.clickable
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

val bookmarkUrlParamKey = ActionParameters.Key<String>(MainActivity.EXTRA_URL)

class BookshelfWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val bookmarks = BookmarkStore.all(context)
        provideContent {
            Content(bookmarks)
        }
    }
}

@Composable
private fun Content(bookmarks: List<Bookmark>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(8.dp),
    ) {
        Text(
            text = "本棚",
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            ),
        )
        if (bookmarks.isEmpty()) {
            Text(
                text = "しおりはまだありません",
                style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 12.sp),
            )
        } else {
            LazyColumn {
                items(bookmarks) { bookmark ->
                    Text(
                        text = bookmark.title,
                        style = TextStyle(color = ColorProvider(Color.White), fontSize = 12.sp),
                        modifier = GlanceModifier
                            .padding(vertical = 4.dp)
                            .clickable(
                                actionStartActivity<MainActivity>(
                                    actionParametersOf(bookmarkUrlParamKey to bookmark.url),
                                ),
                            ),
                    )
                }
            }
        }
    }
}

class BookshelfWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BookshelfWidget()
}
