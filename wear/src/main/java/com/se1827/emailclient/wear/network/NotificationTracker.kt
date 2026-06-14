package com.se1827.emailclient.wear.network

import android.content.Context
import android.content.SharedPreferences

class NotificationTracker(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("draft_notifications", Context.MODE_PRIVATE)

    fun getNotifiedIds(): Set<String> {
        return prefs.getStringSet("notified_drafts", emptySet()) ?: emptySet()
    }

    fun isNotified(draftId: String): Boolean {
        return getNotifiedIds().contains(draftId)
    }

    fun markAsNotified(draftId: String) {
        val current = getNotifiedIds().toMutableSet()
        current.add(draftId)
        prefs.edit().putStringSet("notified_drafts", current).apply()
    }

    fun syncNotifiedIds(currentPendingDraftIds: Set<String>) {
        val notified = getNotifiedIds()
        // Retain only IDs that are STILL pending
        val toRetain = notified.intersect(currentPendingDraftIds)
        if (toRetain.size != notified.size) {
            prefs.edit().putStringSet("notified_drafts", toRetain).apply()
        }
    }
}
