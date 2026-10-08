package com.example.i18n

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.NextDnsApp
import com.example.R
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.AppTheme
import com.example.ui.viewmodel.NextDnsViewModel
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [28], qualifiers = "w320dp-h800dp-mdpi", application = NextDnsApp::class)
class LocaleRenderingTest {
  @get:Rule val rule = createComposeRule()

  @Test fun homeRendersInEveryLanguageAtLargeFontScale() {
    val context = ApplicationProvider.getApplicationContext<NextDnsApp>()
    var language by mutableStateOf("en")
    LocalePreferences.setSelection(context, language)
    rule.setContent {
      key(language) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
          AppTheme { HomeScreen(viewModel = remember { NextDnsViewModel() }) }
        }
      }
    }
    LocalePreferences.supportedLanguages.forEach { choice ->
      rule.runOnIdle {
        LocalePreferences.setSelection(context, choice)
        language = choice
      }
      rule.waitForIdle()
      rule.onNodeWithText("Open Source Client for NextDNS").assertIsDisplayed()
      rule.onNodeWithText(AppStrings.get(R.string.ui_18d49619ba)).assertIsDisplayed()
      rule.onRoot().captureRoboImage(filePath = "build/outputs/roborazzi/localization/home-$choice.png")
    }
  }
}
