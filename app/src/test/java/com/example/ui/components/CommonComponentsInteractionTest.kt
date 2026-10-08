package com.example.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CommonComponentsInteractionTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun nextDnsButton_singleTapInvokesCallbackOnce() {
    var clicks = 0

    composeTestRule.setContent {
      AppTheme {
        NextDnsButton(
          text = "Uygula",
          onClick = { clicks += 1 }
        )
      }
    }

    composeTestRule.onNodeWithText("Uygula").performClick()

    assertEquals(1, clicks)
  }

  @Test
  fun settingToggleRow_hasSingleInteractiveTarget() {
    var changes = 0

    composeTestRule.setContent {
      var checked by remember { mutableStateOf(false) }
      AppTheme {
        NextDnsSettingToggleRow(
          title = "Koruma",
          checked = checked,
          onCheckedChange = { value ->
            checked = value
            changes += 1
          },
          modifier = Modifier.testTag("setting_toggle_row")
        )
      }
    }

    val node = composeTestRule.onNodeWithTag("setting_toggle_row")

    node
      .assertIsToggleable()
      .assertIsOff()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
      .assertIsOn()

    assertEquals(1, changes)
  }
}
