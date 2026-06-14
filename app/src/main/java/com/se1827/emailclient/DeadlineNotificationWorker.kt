package com.se1827.emailclient

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.se1827.emailclient.auth.TokenStore
import com.se1827.emailclient.data.DeadlineRepository
import com.se1827.emailclient.network.NetworkClient
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class DeadlineNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val repo = DeadlineRepository()
    private val prefs by lazy { DeadlinePreferences(context) }

    override suspend fun doWork(): Result {
        if (!prefs.notificationsEnabled) return Result.success()

        // Ensure NetworkClient is initialized with TokenStore (for auth header)
        NetworkClient.init(context.applicationContext)

        // Purge stale notified IDs (older than 72h) to prevent unbounded growth
        prefs.purgeStaleNotifiedIds()

        val response = repo.getDeadlines(prefs.windowDays).getOrNull() ?: return Result.retry()
        val notifiedIds = prefs.getNotifiedIds()
        val remindMinutes = prefs.remindMinutesBefore.toLong()
        val localZone = ZoneId.systemDefault()
        val now = OffsetDateTime.now(localZone)
        val currentLocalTime = LocalTime.now(localZone)
        val isEveningWindow = currentLocalTime.hour in 20..23

        createNotificationChannels()

        var hasUrgentDeadlines = false

        for (deadline in response.deadlines) {
            if (deadline.id in notifiedIds) continue

            val dueTime = try { OffsetDateTime.parse(deadline.due) } catch (e: Exception) { continue }
            val hoursUntilDue = java.time.Duration.between(now, dueTime).toHours()
            val minutesUntilDue = java.time.Duration.between(now, dueTime).toMinutes()

            // Track if any deadline is within 2h for urgent scheduling
            if (minutesUntilDue in 0..120) hasUrgentDeadlines = true

            val shouldNotify = when {
                // Always notify overdue items
                deadline.isOverdue -> true

                // Evening heads-up: between 8pm and midnight, notify for tomorrow morning deadlines (before noon)
                isEveningWindow && prefs.eveningHeadsUpEnabled -> {
                    val dueTomorrow = dueTime.toLocalDate() == now.toLocalDate().plusDays(1)
                    val dueBeforeNoon = dueTime.toLocalTime().hour < 12
                    dueTomorrow && dueBeforeNoon
                }

                // Standard reminder: within the configured window (minutes-based)
                else -> minutesUntilDue in 0..remindMinutes
            }

            if (shouldNotify) {
                val isEveningHeadsUp = isEveningWindow && !deadline.isOverdue &&
                    dueTime.toLocalDate() == now.toLocalDate().plusDays(1)

                fireNotification(
                    id = deadline.id,
                    title = deadline.title,
                    dueIso = deadline.due,
                    isOverdue = deadline.isOverdue,
                    priority = deadline.priority,
                    type = deadline.type,
                    sender = deadline.sender,
                    isEveningHeadsUp = isEveningHeadsUp
                )
                prefs.addNotifiedId(deadline.id)
            }
        }

        // If there are urgent deadlines (< 2h), schedule a one-time expedited check in 15 minutes
        if (hasUrgentDeadlines && prefs.urgentCheckEnabled) {
            scheduleUrgentCheck(context)
        }

        return Result.success()
    }

    private fun fireNotification(
        id: String,
        title: String,
        dueIso: String,
        isOverdue: Boolean,
        priority: String,
        type: String,
        sender: String?,
        isEveningHeadsUp: Boolean
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val emoji = when {
            isEveningHeadsUp -> "🌙"
            type == "event" -> "📅"
            type == "action" -> "⚡"
            else -> "📧"
        }

        val localZone = ZoneId.systemDefault()
        val timeLabel = try {
            val dt = OffsetDateTime.parse(dueIso).atZoneSameInstant(localZone).toOffsetDateTime()
            when {
                isOverdue -> {
                    val h = java.time.Duration.between(dt, OffsetDateTime.now(localZone)).toHours()
                    if (h >= 24) "Overdue by ${h / 24}d ${h % 24}h" else "Overdue by ${h}h"
                }
                isEveningHeadsUp -> {
                    "Tomorrow · ${dt.format(DateTimeFormatter.ofPattern("HH:mm"))}"
                }
                else -> dt.format(DateTimeFormatter.ofPattern("EEE d MMM · HH:mm"))
            }
        } catch (e: Exception) { dueIso }

        // Build contextual content text
        val contentText = when {
            isEveningHeadsUp && sender != null ->
                "Heads up — $timeLabel with ${sender.substringBefore(" <").trim()}. Check for prep material."
            isEveningHeadsUp ->
                "Heads up — $timeLabel. Review any prep material before bed."
            isOverdue && sender != null ->
                "⚠ $timeLabel · from ${sender.substringBefore(" <").trim()}"
            isOverdue -> "⚠ $timeLabel"
            sender != null -> "Due: $timeLabel · from ${sender.substringBefore(" <").trim()}"
            else -> "Due: $timeLabel"
        }

        val bigText = when {
            isEveningHeadsUp && sender != null ->
                "You have \"$title\" at $timeLabel. ${sender.substringBefore(" <").trim()} may have sent material you haven't reviewed yet."
            isEveningHeadsUp ->
                "You have \"$title\" at $timeLabel. Make sure you're prepared."
            isOverdue ->
                "This deadline is overdue — $timeLabel. Take action now."
            sender != null ->
                "Due: $timeLabel. Originally from ${sender.substringBefore(" <").trim()}."
            else -> "Due: $timeLabel"
        }

        // Use high-priority channel for critical/overdue
        val isCritical = isOverdue || priority == "critical"
        val channelId = if (isCritical) CHANNEL_ID_URGENT else CHANNEL_ID

        val notifPriority = when {
            isCritical -> NotificationCompat.PRIORITY_HIGH
            priority == "high" -> NotificationCompat.PRIORITY_DEFAULT
            else -> NotificationCompat.PRIORITY_LOW
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "alerts")
        }
        val pi = PendingIntent.getActivity(
            context,
            id.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$emoji $title")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(notifPriority)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setGroup(NOTIF_GROUP)
            .build()

        nm.notify(id.hashCode(), notif)

        // Post/update the summary notification for grouping
        val summary = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Deadline Reminders")
            .setContentText("You have upcoming deadlines")
            .setGroup(NOTIF_GROUP)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()
        nm.notify(SUMMARY_NOTIF_ID, summary)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Standard channel
            val standard = NotificationChannel(
                CHANNEL_ID,
                "Deadline Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for upcoming deadlines and action items"
            }
            nm.createNotificationChannel(standard)

            // Urgent channel — vibrate + sound for critical/overdue
            val urgent = NotificationChannel(
                CHANNEL_ID_URGENT,
                "Urgent Deadlines",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority alerts for overdue and critical deadlines"
                enableVibration(true)
            }
            nm.createNotificationChannel(urgent)
        }
    }

    companion object {
        const val CHANNEL_ID = "deadline_reminders"
        const val CHANNEL_ID_URGENT = "deadline_urgent"
        const val WORK_NAME = "deadline_check"
        const val URGENT_WORK_NAME = "deadline_urgent_check"
        const val NOTIF_GROUP = "com.se1827.emailclient.DEADLINES"
        const val SUMMARY_NOTIF_ID = 99999

        /** Call once from MainActivity.onCreate() to ensure the worker is scheduled. */
        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<DeadlineNotificationWorker>(1, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                req
            )
        }

        /** Schedule a one-shot expedited check in ~15 minutes for imminent deadlines. */
        fun scheduleUrgentCheck(context: Context) {
            val req = OneTimeWorkRequestBuilder<DeadlineNotificationWorker>()
                .setInitialDelay(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                URGENT_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                req
            )
        }
    }
}
