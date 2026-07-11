package com.piyush.livetranslate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.piyush.livetranslate.core.ui.BrandMark
import com.piyush.livetranslate.core.ui.LiveTranslateTheme
import com.piyush.livetranslate.feature.auth.LoginScreen
import com.piyush.livetranslate.feature.auth.OnboardingScreen
import com.piyush.livetranslate.feature.history.HistoryScreen
import com.piyush.livetranslate.feature.home.HomeScreen
import com.piyush.livetranslate.feature.home.ProfileScreen
import com.piyush.livetranslate.feature.settings.AboutScreen
import com.piyush.livetranslate.feature.settings.PrivacyScreen
import com.piyush.livetranslate.feature.settings.SettingsScreen
import com.piyush.livetranslate.feature.translate.ConversationScreen
import com.piyush.livetranslate.feature.translate.LiveTranslateScreen
import com.piyush.livetranslate.feature.translate.OcrScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { !appViewModel.state.value.ready }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appState by appViewModel.state.collectAsStateWithLifecycle()
            LiveTranslateTheme(appState.settings.themeMode, appState.settings.dynamicColor) {
                LiveTranslateApp(appState)
            }
        }
    }
}

private object Routes {
    const val Splash = "splash"
    const val Onboarding = "onboarding"
    const val Login = "login"
    const val Home = "home"
    const val Live = "live?source={source}&target={target}"
    const val Conversation = "conversation"
    const val Ocr = "ocr"
    const val History = "history"
    const val Favorites = "favorites"
    const val Settings = "settings"
    const val Privacy = "privacy"
    const val About = "about"
    const val Profile = "profile"
}

@Composable
private fun LiveTranslateApp(appState: AppUiState) {
    val nav = rememberNavController()
    fun clearTo(route: String) = nav.navigate(route) { popUpTo(0); launchSingleTop = true }
    NavHost(navController = nav, startDestination = Routes.Splash) {
        composable(Routes.Splash) {
            SplashContent()
            LaunchedEffect(appState.ready) {
                if (appState.ready) {
                    delay(650)
                    clearTo(if (appState.settings.onboardingComplete) Routes.Home else Routes.Onboarding)
                }
            }
        }
        composable(Routes.Onboarding) { OnboardingScreen(onComplete = { clearTo(Routes.Login) }) }
        composable(Routes.Login) {
            LoginScreen(
                webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
                onSignedIn = { clearTo(Routes.Home) },
                onContinueOffline = { clearTo(Routes.Home) },
            )
        }
        composable(Routes.Home) {
            HomeScreen(
                onLiveTranslate = { source, target -> nav.navigate("live?source=$source&target=$target") },
                onConversation = { nav.navigate(Routes.Conversation) },
                onOcr = { nav.navigate(Routes.Ocr) },
                onHistory = { nav.navigate(Routes.History) },
                onFavorites = { nav.navigate(Routes.Favorites) },
                onProfile = { nav.navigate(Routes.Profile) },
                onSettings = { nav.navigate(Routes.Settings) },
            )
        }
        composable(
            Routes.Live,
            arguments = listOf(
                navArgument("source") { type = NavType.StringType; defaultValue = "auto" },
                navArgument("target") { type = NavType.StringType; defaultValue = "en-US" },
            ),
        ) { LiveTranslateScreen(onBack = nav::popBackStack) }
        composable(Routes.Conversation) { ConversationScreen(onBack = nav::popBackStack) }
        composable(Routes.Ocr) { OcrScreen(onBack = nav::popBackStack) }
        composable(Routes.History) { HistoryScreen(false, nav::popBackStack) }
        composable(Routes.Favorites) { HistoryScreen(true, nav::popBackStack) }
        composable(Routes.Settings) { SettingsScreen(nav::popBackStack, { nav.navigate(Routes.Privacy) }, { nav.navigate(Routes.About) }) }
        composable(Routes.Privacy) { PrivacyScreen(nav::popBackStack) }
        composable(Routes.About) { AboutScreen(nav::popBackStack) }
        composable(Routes.Profile) {
            ProfileScreen(
                onBack = { nav.popBackStack() },
                onLoggedOut = { clearTo(Routes.Login) },
            )
        }
    }
}

@Composable
private fun SplashContent() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BrandMark(showCreator = true)
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Text("Private • Fast • Effortless", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
