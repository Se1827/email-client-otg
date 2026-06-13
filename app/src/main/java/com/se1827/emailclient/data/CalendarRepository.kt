package com.se1827.emailclient.data

import com.se1827.emailclient.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CalendarRepository(private val apiService: ApiService = NetworkClient.apiService) {

    suspend fun getCalendarEvents(): Result<List<CalendarEventDto>> = withContext(Dispatchers.IO) {
        runCatching { apiService.getCalendarEvents() }
    }

    suspend fun getUpcomingEvents(days: Int = 7): Result<List<CalendarEventDto>> = withContext(Dispatchers.IO) {
        runCatching { apiService.getUpcomingEvents(days) }
    }

    suspend fun createEvent(request: CreateEventRequest): Result<CalendarEventDto> = withContext(Dispatchers.IO) {
        runCatching { apiService.createCalendarEvent(request) }
    }

    suspend fun updateEvent(id: String, request: CreateEventRequest): Result<CalendarEventDto> = withContext(Dispatchers.IO) {
        runCatching { apiService.updateCalendarEvent(id, request) }
    }

    suspend fun deleteEvent(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { apiService.deleteCalendarEvent(id) }
    }

    suspend fun syncCalendar(): Result<SyncResponse> = withContext(Dispatchers.IO) {
        runCatching { apiService.syncCalendar() }
    }
}
