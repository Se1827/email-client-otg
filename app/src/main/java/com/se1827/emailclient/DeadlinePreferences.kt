package com.se1827.emailclient

import android.content.Context
import android.content.SharedPreferences

/**
 * Stores and retrieves deadline reminder preferences.
 * All values live in a private SharedPreferences file — no backend required.
 */
class DeadlinePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Minutes before a deadline at which the notification fires.
     * Supports granular intervals: 10, 15, 30, 60, 120, 360, 720, 1440.
     * Default = 60 min (1 hour).
     */
    var remindMinutesBefore: Int
        get() = prefs.getInt(KEY_REMIND_MINUTES, DEFAULT_REMIND_MINUTES)
        set(v) = prefs.edit().putInt(KEY_REMIND_MINUTES, v).apply()

    /** Whether deadline notifications are enabled at all. */
    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFS_ENABLED, true)
        set(v) = prefs.edit().putBoolean(KEY_NOTIFS_ENABLED, v).apply()

    /** Window (in days) used when fetching deadlines for notification checks. */
    var windowDays: Int
        get() = prefs.getInt(KEY_WINDOW_DAYS, 7)
        set(v) = prefs.edit().putInt(KEY_WINDOW_DAYS, v).apply()

    /** Whether evening heads-up notifications are enabled (fires between 20:00–23:59 for tomorrow morning deadlines). */
    var eveningHeadsUpEnabled: Boolean
        get() = prefs.getBoolean(KEY_EVENING_HEADSUP, true)
        set(v) = prefs.edit().putBoolean(KEY_EVENING_HEADSUP, v).apply()

    /** Whether the urgent 15-minute fast poll is enabled for deadlines due within 2 hours. */
    var urgentCheckEnabled: Boolean
        get() = prefs.getBoolean(KEY_URGENT_CHECK, true)
        set(v) = prefs.edit().putBoolean(KEY_URGENT_CHECK, v).apply()

    /** Set of deadline IDs that have already been notified (persisted across restarts). */
    fun getNotifiedIds(): Set<String> =
        prefs.getStringSet(KEY_NOTIFIED_IDS, emptySet()) ?: emptySet()

    fun addNotifiedId(id: String) {
        val current = getNotifiedIds().toMutableSet().also { it.add(id) }
        prefs.edit().putStringSet(KEY_NOTIFIED_IDS, current).apply()
        // Record the timestamp so we can expire old IDs
        prefs.edit().putLong("${KEY_NOTIFIED_TS_PREFIX}$id", System.currentTimeMillis()).apply()
    }

    fun clearNotifiedIds() {
        prefs.edit().remove(KEY_NOTIFIED_IDS).apply()
    }

    /**
     * Purge notified IDs older than [maxAgeHours] to prevent unbounded growth.
     * Default TTL is 72 hours (3 days).
     */
    fun purgeStaleNotifiedIds(maxAgeHours: Int = 72) {
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
        private const val PREFS_NAME = "deadline_prefs"
        private const val KEY_REMIND_MINUTES = "remind_minutes_before"
        private const val KEY_NOTIFS_ENABLED = "notifications_enabled"
        private const val KEY_WINDOW_DAYS = "window_days"
        private const val KEY_NOTIFIED_IDS = "notified_deadline_ids"
        private const val KEY_NOTIFIED_TS_PREFIX = "notified_ts_"
        private const val KEY_EVENING_HEADSUP = "evening_headsup_enabled"
        private const val KEY_URGENT_CHECK = "urgent_check_enabled"

        const val DEFAULT_REMIND_MINUTES = 60

        /** Predefined interval options for the UI picker. */
        val INTERVAL_OPTIONS = listOf(
            10 to "10 min",
            15 to "15 min",
            30 to "30 min",
            60 to "1 hour",
            120 to "2 hours",
            360 to "6 hours",
            720 to "12 hours",
            1440 to "1 day"
        )
    }
}
