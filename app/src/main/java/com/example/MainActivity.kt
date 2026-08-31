package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.data.preferences.ThemePreferences
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.NextDnsViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: NextDnsViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    val splashScreen = installSplashScreen()
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    splashScreen.setKeepOnScreenCondition {
        // Keep splash screen on until we know the login status
        viewModel.isInitializing.value
    }
    
    setContent {
        val context = LocalContext.current
        val themePrefs = remember { ThemePreferences(context) }
        val themeMode by themePrefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)

        AppTheme(themeMode = themeMode) {
            val isLoggedIn by viewModel.isLoggedIn.collectAsState()

        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          if (isLoggedIn != null) {
            AnimatedContent(
              targetState = isLoggedIn == true,
              transitionSpec = {
                if (targetState) {
                  (slideInVertically { height -> height } + fadeIn()).togetherWith(
                    slideOutVertically { height -> -height } + fadeOut()
                  )
                } else {
                  (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                    slideOutVertically { height -> height } + fadeOut()
                  )
                }
              },
              label = "auth_navigation"
            ) { loggedIn ->
              if (loggedIn) {
                HomeScreen(viewModel = viewModel)
              } else {
                LoginScreen(viewModel = viewModel)
              }
            }
          }
        }
      }
    }
  }
}
