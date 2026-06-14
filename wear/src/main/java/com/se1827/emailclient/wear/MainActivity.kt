package com.se1827.emailclient.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.se1827.emailclient.wear.auth.AuthViewModel
import com.se1827.emailclient.wear.network.NetworkClient
import com.se1827.emailclient.wear.ui.screen.AlertsScreen
import com.se1827.emailclient.wear.ui.screen.EmailDetailScreen
import com.se1827.emailclient.wear.ui.screen.EmailListScreen
import com.se1827.emailclient.wear.ui.screen.LoginScreen
import com.se1827.emailclient.wear.ui.theme.EmailAgentWearTheme
import com.se1827.emailclient.wear.viewmodel.AlertsViewModel
import com.se1827.emailclient.wear.viewmodel.EmailViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications

private const val ROUTE_LIST = "list"
private const val ROUTE_ALERTS = "alerts"
private const val ROUTE_DETAIL = "detail/{emailId}"
private const val ARG_EMAIL_ID = "emailId"

class MainActivity : ComponentActivity() {
    private val emailViewModel: EmailViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    // Runtime notification permission request for Wear OS 4+ (API 33+)
    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — notifications will work or be silently skipped */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        NetworkClient.init(applicationContext)

        // Schedule the periodic notification worker (every 30 minutes)
        WearNotificationWorker.schedule(this)

        // Request POST_NOTIFICATIONS permission on Android 13+ (Wear OS 4+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            EmailAgentWearTheme {
                val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()
                val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

                if (isLoggedIn) {
                    WearNavHost(emailViewModel = emailViewModel)
                } else {
                    LoginScreen(
                        authState = authUiState,
                        onLogin = { password -> authViewModel.login(password) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WearNavHost(emailViewModel: EmailViewModel) {
    val navController = rememberSwipeDismissableNavController()
    val alertsViewModel: AlertsViewModel = viewModel()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = ROUTE_LIST
    ) {
        composable(ROUTE_LIST) {
            EmailListScreen(
                viewModel = emailViewModel,
                onEmailClick = { emailId -> navController.navigate("detail/$emailId") },
                onNavigateToAlerts = { navController.navigate(ROUTE_ALERTS) }
            )
        }

        composable(ROUTE_ALERTS) {
            AlertsScreen(viewModel = alertsViewModel)
        }

        composable(ROUTE_DETAIL) { backStackEntry ->
            val emailId = backStackEntry.arguments?.getString(ARG_EMAIL_ID) ?: return@composable
            val emailItem = emailViewModel.getEmailById(emailId) ?: return@composable
            val isProcessing by emailViewModel.isProcessing.collectAsState()

            EmailDetailScreen(
                emailItem = emailItem,
                isProcessing = isProcessing,
                onSend = {
                    emailViewModel.approveDraft(emailId)
                    navController.popBackStack()
                },
                onSkip = {
                    emailViewModel.dismissLocally(emailId)
                    navController.popBackStack()
                }
            )
        }
    }
}
