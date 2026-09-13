package com.t25hash.discordwidget

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
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

private val urlParamKey = ActionParameters.Key<String>(MainActivity.EXTRA_URL)

// Nothing OS の "Ndot" 風(ドットマトリクス的な間延びした大文字英字)を
// Glanceのフォント指定なしで近似するための見出しテキスト。
private const val HEADER_TITLE = "D I S C O R D"

class DiscordChannelWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val messages = DiscordStore.cache(context)
        val guildId = DiscordStore.guildId(context)
        val channelId = DiscordStore.channelId(context)
        val openUrl = if (guildId.isNotBlank() && channelId.isNotBlank()) {
            "https://discord.com/channels/$guildId/$channelId"
        } else {
            null
        }
        provideContent {
            Content(messages, openUrl)
        }
    }
}

@Composable
private fun Content(messages: List<DiscordMessage>, openUrl: String?) {
    val headerModifier = if (openUrl != null) {
        GlanceModifier.clickable(
            actionStartActivity<MainActivity>(actionParametersOf(urlParamKey to openUrl)),
        )
    } else {
        GlanceModifier
    }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(8.dp),
    ) {
        Text(
            text = HEADER_TITLE,
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            ),
            modifier = headerModifier,
        )
        if (messages.isEmpty()) {
            Text(
                text = "まだメッセージがありません",
                style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 12.sp),
            )
        } else {
            LazyColumn {
                items(messages) { m ->
                    Text(
                        text = "${m.author}: ${m.content}",
                        style = TextStyle(color = ColorProvider(Color.White), fontSize = 12.sp),
                        modifier = GlanceModifier.padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}

class DiscordChannelWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DiscordChannelWidget()
}
