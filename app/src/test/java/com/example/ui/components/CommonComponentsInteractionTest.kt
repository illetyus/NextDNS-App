package com.example.ui.components

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNode
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
      AppTheme {
        NextDnsSettingToggleRow(
          title = "Koruma",
          checked = false,
          onCheckedChange = { changes += 1 }
        )
      }
    }

    composeTestRule.onNode(hasClickAction()).performClick()

    assertEquals(1, changes)
  }
}
