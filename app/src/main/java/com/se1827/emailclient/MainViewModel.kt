package com.se1827.emailclient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.se1827.emailclient.data.EmailRepository
import com.se1827.emailclient.network.DashboardStatsDto
import com.se1827.emailclient.network.EmailDto
import com.se1827.emailclient.network.NotificationDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

class MainViewModel(
    private val repository: EmailRepository = EmailRepository()
) : ViewModel() {

    val dashboardStats: StateFlow<UiState<DashboardStats>> = flow {
        while (currentCoroutineContext().isActive) {
            repository.getDashboardStats().onSuccess { statsDto ->
                val stats = DashboardStats(
                    totalEmails = statsDto.totalEmails,
                    unread = statsDto.unreadCount,
                    classified = statsDto.classifiedCount,
                    starred = statsDto.starredCount,
                    // pendingDrafts = drafts awaiting review, not total classified
                    pendingDrafts = statsDto.notifications.count { n ->
                        n.type == "ai_insight" && n.title.contains("draft", ignoreCase = true)
                    }.takeIf { it > 0 }
                        ?: (statsDto.priorityBreakdown["critical"] ?: 0) +
                        (statsDto.priorityBreakdown["high"] ?: 0),
                    responseReadiness = if (statsDto.totalEmails > 0)
                        (statsDto.classifiedCount.toFloat() / statsDto.totalEmails)
                    else 0f,
                    criticalCount = statsDto.priorityBreakdown["critical"] ?: 0
                )
                emit(UiState.Success(stats))
            }.onFailure {
                emit(UiState.Error(it.message ?: "Unknown error"))
            }
            delay(5000L)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState.Loading
    )

    private val _emails = MutableStateFlow<UiState<List<EmailUi>>>(UiState.Loading)
    val emails: StateFlow<UiState<List<EmailUi>>> = _emails.asStateFlow()

    private val _alerts = MutableStateFlow<UiState<List<AgentAlert>>>(UiState.Loading)
    val alerts: StateFlow<UiState<List<AgentAlert>>> = _alerts.asStateFlow()

    private val _smartCards = MutableStateFlow<UiState<List<SmartCardEmail>>>(UiState.Loading)
    val smartCards: StateFlow<UiState<List<SmartCardEmail>>> = _smartCards.asStateFlow()

    init {
        fetchEmails()
        fetchAlerts()
    }

    fun fetchEmails() {
        _emails.value = UiState.Loading
        _smartCards.value = UiState.Loading
        viewModelScope.launch {
            repository.getEmails().onSuccess { dtos ->
                val uiEmails = dtos.map { it.toUiModel() }
                _emails.value = UiState.Success(uiEmails)

                val smartCardEmails = dtos.map { it.toSmartCardModel() }
                _smartCards.value = UiState.Success(smartCardEmails)
            }.onFailure {
                _emails.value = UiState.Error(it.message ?: "Unknown error fetching emails")
                _smartCards.value = UiState.Error(it.message ?: "Unknown error fetching emails")
            }
        }
    }

    fun fetchAlerts() {
        _alerts.value = UiState.Loading
        viewModelScope.launch {
            repository.getNotifications().onSuccess { dtos ->
                // Deduplicate by ID to prevent LazyColumn key-conflict crashes
                _alerts.value = UiState.Success(
                    dtos.distinctBy { it.id }.map { it.toUiModel() }
                )
            }.onFailure {
                // Only show error if we have no prior data — otherwise keep stale data
                val existing = (_alerts.value as? UiState.Success)?.data
                if (existing == null) {
                    _alerts.value = UiState.Error(it.message ?: "Unknown error fetching alerts")
                }
            }
        }
    }

    fun refreshEmails() {
        viewModelScope.launch {
            repository.refreshEmails().onSuccess {
                fetchEmails()
                fetchAlerts()
            }
        }
    }

    fun dismissAlert(id: String) {
        // Optimistically remove from UI first, then fire API call
        val currentList = (_alerts.value as? UiState.Success)?.data ?: return
        val updated = currentList.filter { it.id != id }
        _alerts.value = UiState.Success(updated)
        viewModelScope.launch {
            repository.dismissNotification(id)
        }
    }

    private fun EmailDto.toUiModel(): EmailUi {
        val priorityEnum = when (classification?.priority?.lowercase()) {
            "critical" -> Priority.Critical
            "high" -> Priority.High
            "low" -> Priority.Low
            else -> Priority.Normal
        }

        return EmailUi(
            id = id,
            sender = sender,
            subject = subject,
            preview = body.take(100),
            time = formatTimeAgo(timestamp),
            priority = priorityEnum,
            category = classification?.category ?: "unknown",
            isUnread = !isRead,
            isStarred = isStarred,
            hasDraft = draftReply != null
        )
    }

    private fun NotificationDto.toUiModel(): AgentAlert {
        val severityEnum = when (severity.lowercase()) {
            "critical" -> AlertSeverity.Critical
            "warning" -> AlertSeverity.Warning
            else -> AlertSeverity.Info
        }
        return AgentAlert(
            id = id,
            title = title,
            message = message,
            severity = severityEnum,
            time = formatTimeAgo(timestamp)
        )
    }

    private fun EmailDto.toSmartCardModel(): SmartCardEmail {
        val scenarioEnum = when (classification?.category?.lowercase()) {
            "meeting" -> CardScenario.Meeting
            "deadline", "action-required" -> CardScenario.Task
            "spam" -> CardScenario.Spam
            "info" -> CardScenario.Default
            else -> CardScenario.Default
        }

        return SmartCardEmail(
            id = id,
            sender = sender,
            subject = subject,
            bodyPreview = body.take(100),
            scenario = scenarioEnum,
            pnr = Regex("PNR\\s*[:\\-]?\\s*([A-Z0-9]{6})", RegexOption.IGNORE_CASE).find(body)?.groupValues?.get(1),
            meetingTime = Regex("(\\d{1,2}:\\d{2}\\s*(?:AM|PM))", RegexOption.IGNORE_CASE).find(body)?.groupValues?.get(1),
            amount = Regex("(₹|\\$|€|£)\\s*([0-9,]+(?:\\.\\d{2})?)").find(body)?.value,
            bankName = null,
            links = Regex("https?://[\\w-]+(\\.[\\w-]+)+[/#?]?.*$").findAll(body).map { it.value }.toList(),
            isSpam = classification?.category?.lowercase() == "spam"
        )
    }

    private fun formatTimeAgo(isoString: String): String {
        return try {
            val time = OffsetDateTime.parse(isoString)
            val now = OffsetDateTime.now()
            val mins = ChronoUnit.MINUTES.between(time, now)
            val hours = ChronoUnit.HOURS.between(time, now)
            val days = ChronoUnit.DAYS.between(time, now)
            when {
                mins < 1 -> "Now"
                mins < 60 -> "${mins}m"
                hours < 24 -> "${hours}h"
                else -> "${days}d"
            }
        } catch (e: Exception) {
            "..."
        }
    }
}
