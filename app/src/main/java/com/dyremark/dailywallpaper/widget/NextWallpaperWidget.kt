package com.dyremark.dailywallpaper.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.dyremark.dailywallpaper.R
import com.dyremark.dailywallpaper.work.WallpaperWorker

/**
 * Home-screen widget: a single button that swaps to a new wallpaper on tap, without opening the
 * app. The tap fires a self-targeted broadcast, which enqueues the shared [WallpaperWorker].
 */
class NextWallpaperWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_next_wallpaper).apply {
                setOnClickPendingIntent(R.id.widget_root, nextWallpaperIntent(context))
            }
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_NEXT_WALLPAPER) {
            WorkManager.getInstance(context).enqueue(
                OneTimeWorkRequestBuilder<WallpaperWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build(),
            )
        }
    }

    companion object {
        private const val ACTION_NEXT_WALLPAPER =
            "com.dyremark.dailywallpaper.action.NEXT_WALLPAPER"

        private fun nextWallpaperIntent(context: Context): PendingIntent {
            val intent = Intent(context, NextWallpaperWidget::class.java)
                .setAction(ACTION_NEXT_WALLPAPER)
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
