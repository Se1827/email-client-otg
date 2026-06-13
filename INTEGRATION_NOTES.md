# Integration Notes

## 1. Project Structure
- Single-module Android project (`app`).
- Main package: `com.se1827.emailclient`.
- Source code is contained entirely in `MainActivity.kt` and `SmartCard.kt`.

## 2. UI Framework
- Jetpack Compose with Material 3.
- No formal Navigation graph (like Compose Navigation) is used; navigation is handled via a simple `selectedTab` state switching between `Dashboard`, `Triage`, `Alerts`, and `SmartCards`.
- We will preserve this simple navigation and not introduce a heavyweight navigation library unless necessary.

## 3. State Management
- Currently using Compose `remember { mutableStateOf(...) }` inside the UI.
- **Action**: We will introduce standard Jetpack `ViewModel`s (e.g., `MainViewModel`) to hold the `StateFlow`s for emails, alerts, and dashboard stats, as the current state is purely local and synchronous.

## 4. Networking Layer
- Dependencies (`retrofit`, `retrofit2-converter-gson`, `okhttp3-logging-interceptor`, `kotlinx-coroutines-android`) are already present in `app/build.gradle.kts`.
- No actual networking classes exist.
- **Action**: Create `ApiService` (Retrofit interface), `NetworkModule` (for providing Retrofit instance, handling `BASE_URL`), and data classes representing the FastAPI backend models.

## 5. DI Framework
- None is currently present.
- **Action**: To maintain simplicity and match the "preserve architecture patterns" guideline, we will use a manual DI pattern (e.g., a simple `ServiceLocator` or `AppContainer` object) instead of introducing Hilt/Koin, which would require massive boilerplate and app-wide changes.

## 6. Existing Mock Data
- Located at the bottom of `MainActivity.kt` (`sampleAlerts`, `sampleEmails`) and `SmartCard.kt`.
- **Action**: These will be replaced by data fetched from the `ApiService`.

## 7. Screen Inventory & Mapping
- **DashboardScreen**: Map to `GET /api/dashboard`. Needs to poll every ~5s.
- **TriageScreen** (Inbox): Map to `GET /api/emails`. Refresh button maps to `POST /api/emails/refresh`.
- **AlertsScreen**: Map to `GET /api/notifications`. Dismiss maps to `POST /api/notifications/{id}/dismiss`.
- **SmartCardsScreen**: Currently uses `SmartCardEmail`. We will map this to `GET /api/emails` filtered by classification or specific backend endpoints if necessary.

## 8. Existing Models
- `AgentAlert` -> Backend `Notification`
- `EmailUi` -> Backend `Email` + `Classification`
- `DashboardStats` -> Backend `DashboardStats`
- **Action**: Create Network DTOs and map them to these UI models to preserve existing Compose components.
