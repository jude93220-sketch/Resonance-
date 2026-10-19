package com.resonance

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

class ResonanceWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
        refreshFromSession(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            refreshFromSession(context)
            return
        }
        if (intent.action !in ACTIONS) return
        runControllerCommand(context, intent.action!!)
    }

    companion object {
        const val ACTION_PREVIOUS = "com.resonance.widget.PREVIOUS"
        const val ACTION_PLAY_PAUSE = "com.resonance.widget.PLAY_PAUSE"
        const val ACTION_NEXT = "com.resonance.widget.NEXT"
        const val ACTION_REFRESH = "com.resonance.widget.REFRESH"
        private val ACTIONS = setOf(ACTION_PREVIOUS, ACTION_PLAY_PAUSE, ACTION_NEXT)

        fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_resonance)
            if (AppSettings.widgetStyle(context) == 1) {
                views.setInt(R.id.widget_container, "setBackgroundColor", 0xDD12335A.toInt())
                views.setTextColor(R.id.widget_play_pause, 0xFFFFB300.toInt())
            }
            views.setOnClickPendingIntent(
                R.id.widget_previous,
                commandIntent(context, ACTION_PREVIOUS),
            )
            views.setOnClickPendingIntent(
                R.id.widget_play_pause,
                commandIntent(context, ACTION_PLAY_PAUSE),
            )
            views.setOnClickPendingIntent(
                R.id.widget_next,
                commandIntent(context, ACTION_NEXT),
            )
            val open = PendingIntent.getActivity(
                context,
                900,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_container, open)
            manager.updateAppWidget(id, views)
        }

        private fun commandIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, ResonanceWidget::class.java).setAction(action)
            return PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun runControllerCommand(context: Context, action: String) {
            val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val future: ListenableFuture<MediaController> =
                MediaController.Builder(context, token).buildAsync()
            future.addListener({
                try {
                    val controller = future.get()
                    when (action) {
                        ACTION_PREVIOUS -> controller.seekToPrevious()
                        ACTION_PLAY_PAUSE -> if (controller.isPlaying) controller.pause() else controller.play()
                        ACTION_NEXT -> controller.seekToNext()
                    }
                    controller.release()
                    refreshFromSession(context)
                } catch (_: Throwable) {
                    MediaController.releaseFuture(future)
                }
            }, ContextCompat.getMainExecutor(context))
        }

        private fun refreshFromSession(context: Context) {
            val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val future = MediaController.Builder(context, token).buildAsync()
            future.addListener({
                try {
                    val controller = future.get()
                    val manager = AppWidgetManager.getInstance(context)
                    val component = ComponentName(context, ResonanceWidget::class.java)
                    manager.getAppWidgetIds(component).forEach { id ->
                        val views = RemoteViews(context.packageName, R.layout.widget_resonance)
                        views.setTextViewText(
                            R.id.widget_title,
                            controller.mediaMetadata.title?.toString() ?: "Resonance",
                        )
                        views.setTextViewText(
                            R.id.widget_artist,
                            controller.mediaMetadata.artist?.toString() ?: "No track playing",
                        )
                        views.setTextViewText(
                            R.id.widget_play_pause,
                            if (controller.isPlaying) "Ⅱ" else "▶",
                        )
                        update(context, manager, id)
                        manager.partiallyUpdateAppWidget(id, views)
                    }
                    controller.release()
                } catch (_: Throwable) {
                    MediaController.releaseFuture(future)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }
}
