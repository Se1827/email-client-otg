package com.se1827.emailclient.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.navigation.navDeepLink
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import com.se1827.emailclient.wear.network.DraftCheckWorker
import java.util.concurrent.TimeUnit
import com.se1827.emailclient.wear.ui.screen.EmailDetailScreen
import com.se1827.emailclient.wear.ui.screen.EmailListScreen
import com.se1827.emailclient.wear.ui.theme.EmailAgentWearTheme
import com.se1827.emailclient.wear.viewmodel.EmailViewModel

private const val ROUTE_LIST = "list"
private const val ROUTE_DETAIL = "detail/{emailId}"
private const val ARG_EMAIL_ID = "emailId"

class MainActivity : ComponentActivity() {
    private val viewModel: EmailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val draftCheckWork = PeriodicWorkRequestBuilder<DraftCheckWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DraftCheckWork",
            ExistingPeriodicWorkPolicy.KEEP,
            draftCheckWork
        )

        setContent {
            EmailAgentWearTheme {
                WearNavHost(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun WearNavHost(viewModel: EmailViewModel) {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = ROUTE_LIST
    ) {
        composable(ROUTE_LIST) {
            EmailListScreen(
                viewModel = viewModel,
                onEmailClick = { emailId -> navController.navigate("detail/$emailId") }
            )
        }

        composable(
            route = ROUTE_DETAIL,
            deepLinks = listOf(navDeepLink { uriPattern = "emailagent://draft/{emailId}" })
        ) { backStackEntry ->
            val emailId = backStackEntry.arguments?.getString(ARG_EMAIL_ID) ?: return@composable
            val emailItem = viewModel.getEmailById(emailId) ?: return@composable
            val isProcessing by viewModel.isProcessing.collectAsState()

            EmailDetailScreen(
                emailItem = emailItem,
                isProcessing = isProcessing,
                onSend = {
                    viewModel.approveDraft(emailId)
                    navController.popBackStack()
                },
                onSkip = {
                    viewModel.dismissLocally(emailId)
                    navController.popBackStack()
                }
            )
        }
    }
}
