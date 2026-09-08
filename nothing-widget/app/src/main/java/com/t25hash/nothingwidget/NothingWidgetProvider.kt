package com.t25hash.nothingwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

/**
 * 骨組みのみ。onUpdate は固定レイアウトを貼るだけで、実データ表示は未実装。
 */
class NothingWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_nothing)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
