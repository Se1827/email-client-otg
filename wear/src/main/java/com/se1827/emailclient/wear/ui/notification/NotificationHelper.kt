package com.se1827.emailclient.wear.ui.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.se1827.emailclient.wear.MainActivity

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AI Drafts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for AI generated draft replies"
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun showDraftNotification(draftId: String, senderContext: String, priority: String) {
        val color = when (priority.lowercase()) {
            "critical" -> Color.RED
            "high" -> 0xFFFFA500.toInt() // Orange
            "normal" -> Color.BLUE
            "low" -> Color.GREEN
            else -> Color.BLUE
        }

        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("emailagent://draft/$draftId"),
            context,
            MainActivity::class.java
        )

        val pendingIntent = PendingIntent.getActivity(
            context,
            draftId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            // Wear OS requires a small icon
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("AI Draft Ready")
            .setContentText(senderContext)
            .setColor(color)
            .setColorized(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(draftId.hashCode(), notification)
    }

    companion object {
        private const val CHANNEL_ID = "ai_drafts_channel"
    }
}
