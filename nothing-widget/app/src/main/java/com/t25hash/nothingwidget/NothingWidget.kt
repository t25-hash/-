package com.t25hash.nothingwidget

import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text

class NothingWidget : GlanceAppWidget() {
    @Composable
    override fun Content() {
        Box(modifier = GlanceModifier.fillMaxSize()) {
            Text(text = "Nothing Widget — coming soon")
        }
    }
}

class NothingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NothingWidget()
}
