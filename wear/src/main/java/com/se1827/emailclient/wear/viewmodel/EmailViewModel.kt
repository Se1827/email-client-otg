package com.se1827.emailclient.wear.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.se1827.emailclient.wear.data.model.EmailItem

class EmailViewModel : ViewModel() {
    val emails = mutableStateListOf(
        EmailItem(
            id = "1",
            senderDisplayName = "Aarav Mehta",
            draftPreview = "I can approve the final demo deck after one last review.",
            fullDraftBody = "Hi Aarav,\n\nI can approve the final demo deck after one last review. Please send the current version and I will confirm the walkthrough flow before the client sync.\n\nRegards",
            priority = "critical",
            category = "action-required"
        ),
        EmailItem(
            id = "2",
            senderDisplayName = "Nisha Rao",
            draftPreview = "Thanks for moving the architecture review. I will join at 3:30 PM.",
            fullDraftBody = "Hi Nisha,\n\nThanks for the update. I will join the architecture review at 3:30 PM and come prepared to discuss auth and notification retries.\n\nRegards",
            priority = "high",
            category = "meeting"
        ),
        EmailItem(
            id = "3",
            senderDisplayName = "Team Digest",
            draftPreview = "No action needed on the build summary right now.",
            fullDraftBody = "Noted. No action needed on the build summary right now.",
            priority = "low",
            category = "info"
        )
    )

    fun getEmailById(id: String): EmailItem? = emails.firstOrNull { it.id == id }

    fun approveDraft(id: String) {
        emails.removeAll { it.id == id }
    }

    fun dismissLocally(id: String) {
        emails.removeAll { it.id == id }
    }
}
