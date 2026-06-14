package com.se1827.emailclient.wear

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.se1827.emailclient.wear.data.DeadlineRepository
import com.se1827.emailclient.wear.network.NetworkClient
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * Background worker that polls the server for deadlines and notifications,
 * then fires system-level notifications on the watch face.
 *
 * Mirrors the behaviour of the phone's DeadlineNotificationWorker but
 * adapted for Wear OS constraints (shorter intervals, haptic feedback,
 * simpler notification layout).
 */
class WearNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val repo = DeadlineRepository()
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun doWork(): Result {
        // Ensure NetworkClient is initialized with TokenStore for auth header
        NetworkClient.init(context.applicationContext)

        // Purge stale notified IDs (older than 48h) to prevent unbounded growth
        purgeStaleNotifiedIds()

        createNotificationChannels()

        val notifiedIds = getNotifiedIds()
        val localZone = ZoneId.systemDefault()
        val now = OffsetDateTime.now(localZone)

        // ── Fetch deadlines ──────────────────────────────────────────────────
        repo.getDeadlines(3).onSuccess { response ->
            for (deadline in response.deadlines) {
                if (deadline.id in notifiedIds) continue

                val dueTime = try {
                    OffsetDateTime.parse(deadline.due)
                } catch (e: Exception) {
                    continue
                }
                val minutesUntilDue = java.time.Duration.between(now, dueTime).toMinutes()

                val shouldNotify = when {
                    deadline.isOverdue -> true
                    minutesUntilDue in 0..60 -> true   // Within 1 hour
                    else -> false
                }

                if (shouldNotify) {
                    val emoji = when (deadline.type) {
                        "event" -> "📅"
                        "action" -> "⚡"
                        else -> "📧"
                    }

                    val timeLabel = try {
                        val dt = dueTime.atZoneSameInstant(localZone).toOffsetDateTime()
                        when {
                            deadline.isOverdue -> {
                                val h = java.time.Duration.between(dt, now).toHours()
                                if (h >= 24) "Overdue ${h / 24}d" else "Overdue ${h}h"
                            }
                            else -> dt.format(DateTimeFormatter.ofPattern("HH:mm"))
                        }
                    } catch (e: Exception) {
                        deadline.due
                    }

                    val isCritical = deadline.isOverdue || deadline.priority == "critical"

                    fireNotification(
                        id = deadline.id.hashCode(),
                        title = "$emoji ${deadline.title}",
                        text = if (deadline.isOverdue) "⚠ $timeLabel" else "Due: $timeLabel",
                        channelId = if (isCritical) CHANNEL_ID_URGENT else CHANNEL_ID,
                        priority = if (isCritical) NotificationCompat.PRIORITY_HIGH
                                   else NotificationCompat.PRIORITY_DEFAULT
                    )
                    addNotifiedId(deadline.id)

                    // Haptic buzz for urgent items on Wear OS
                    if (isCritical) vibrateWatch()
                }
            }
        }

        // ── Fetch server notifications ───────────────────────────────────────
        repo.getNotifications().onSuccess { notifications ->
            for (notif in notifications.distinctBy { it.id }) {
                val notifKey = "notif_${notif.id}"
                if (notifKey in notifiedIds) continue
                if (notif.isRead) continue

                val isCritical = notif.severity == "critical"

                fireNotification(
                    id = notif.id.hashCode(),
                    title = notif.title,
                    text = notif.message,
                    channelId = if (isCritical) CHANNEL_ID_URGENT else CHANNEL_ID,
                    priority = if (isCritical) NotificationCompat.PRIORITY_HIGH
                               else NotificationCompat.PRIORITY_DEFAULT
                )
                addNotifiedId(notifKey)

                if (isCritical) vibrateWatch()
            }
        }

        return Result.success()
    }

    // ── Notification helpers ──────────────────────────────────────────────────

    private fun fireNotification(
        id: Int,
        title: String,
        text: String,
        channelId: String,
        priority: Int
    ) {
        // Check permission on Android 13+ (Wear OS 4+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context, id, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(priority)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setGroup(NOTIF_GROUP)
            .build()

        nm.notify(id, notif)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val standard = NotificationChannel(
                CHANNEL_ID,
                "Email Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Deadline and email alert notifications"
            }
            nm.createNotificationChannel(standard)

            val urgent = NotificationChannel(
                CHANNEL_ID_URGENT,
                "Urgent Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical and overdue deadline alerts"
                enableVibration(true)
            }
            nm.createNotificationChannel(urgent)
        }
    }

    private fun vibrateWatch() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {
            // Vibration is best-effort; don't crash the worker
        }
    }

    // ── Notified-ID persistence (prevents duplicate notifications) ────────────

    private fun getNotifiedIds(): Set<String> =
        prefs.getStringSet(KEY_NOTIFIED_IDS, emptySet()) ?: emptySet()

    private fun addNotifiedId(id: String) {
        val current = getNotifiedIds().toMutableSet().also { it.add(id) }
        prefs.edit()
            .putStringSet(KEY_NOTIFIED_IDS, current)
            .putLong("${KEY_NOTIFIED_TS_PREFIX}$id", System.currentTimeMillis())
            .apply()
    }

    private fun purgeStaleNotifiedIds(maxAgeHours: Int = 48) {
        val cutoff = System.currentTimeMillis() - (maxAgeHours * 3600_000L)
        val current = getNotifiedIds().toMutableSet()
        val editor = prefs.edit()
        val toRemove = mutableSetOf<String>()

        for (id in current) {
            val ts = prefs.getLong("${KEY_NOTIFIED_TS_PREFIX}$id", 0L)
            if (ts > 0 && ts < cutoff) {
                toRemove.add(id)
                editor.remove("${KEY_NOTIFIED_TS_PREFIX}$id")
            }
        }

        if (toRemove.isNotEmpty()) {
            current.removeAll(toRemove)
            editor.putStringSet(KEY_NOTIFIED_IDS, current)
        }
        editor.apply()
    }

    companion object {
        const val CHANNEL_ID = "wear_email_alerts"
        const val CHANNEL_ID_URGENT = "wear_email_urgent"
        const val WORK_NAME = "wear_notification_check"
        const val NOTIF_GROUP = "com.se1827.emailclient.wear.ALERTS"

        private const val PREFS_NAME = "wear_notification_prefs"
        private const val KEY_NOTIFIED_IDS = "notified_ids"
        private const val KEY_NOTIFIED_TS_PREFIX = "notified_ts_"

        /**
         * Schedule the periodic notification check.
         * Runs every 30 minutes (minimum for Wear OS battery constraints).
         */
        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<WearNotificationWorker>(30, TimeUnit.MINUTES)
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
    }
}
