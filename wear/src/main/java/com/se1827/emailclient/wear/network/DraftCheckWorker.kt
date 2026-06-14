package com.se1827.emailclient.wear.network

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.se1827.emailclient.wear.data.EmailRepository
import com.se1827.emailclient.wear.ui.notification.NotificationHelper

class DraftCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val repository = EmailRepository()
    private val notificationTracker = NotificationTracker(context)
    private val notificationHelper = NotificationHelper(context)

    override suspend fun doWork(): Result {
        return try {
            val drafts = repository.getPendingDrafts()
            
            // Handle Notification Deduplication
            val draftIds = drafts.map { it.id }.toSet()
            notificationTracker.syncNotifiedIds(draftIds)
            
            drafts.forEach { draft ->
                if (!notificationTracker.isNotified(draft.id)) {
                    notificationHelper.showDraftNotification(draft.id, draft.senderDisplayName, draft.priority)
                    notificationTracker.markAsNotified(draft.id)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
