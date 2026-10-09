package com.example.data.legal

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.LegalDocumentLinks
import com.example.ui.theme.AppTheme
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LegalDocumentLinksTest {
  @get:Rule val rule = createComposeRule()
  private lateinit var context: Context
  @Before fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context.getSharedPreferences("legal_acceptance", Context.MODE_PRIVATE).edit().clear().commit()
  }
  @After fun tearDown() {
    context.getSharedPreferences("legal_acceptance", Context.MODE_PRIVATE).edit().clear().commit()
  }
  @Test fun readingPrivacyDoesNotAcceptTerms() {
    rule.setContent { AppTheme { LegalDocumentLinks() } }
    rule.onNodeWithTag("legal_privacy_link").performClick()
    rule.onNodeWithTag("legal_document_content").assertTextContains("Privacy Policy", substring = true)
    assertFalse(LegalAcceptanceStore(context).isAccepted())
    rule.onNodeWithTag("legal_reader_close").performClick()
    rule.onNodeWithTag("legal_privacy_link").assertIsDisplayed()
    assertFalse(LegalAcceptanceStore(context).isAccepted())
  }
  @Test fun reopeningTermsPreservesExistingAcceptanceReceipt() {
    val store = LegalAcceptanceStore(context) { 1_791_500_000_000L }
    assertTrue(store.acceptCurrentTerms())
    val before = context.getSharedPreferences("legal_acceptance", Context.MODE_PRIVATE).all
    rule.setContent { AppTheme { LegalDocumentLinks() } }
    rule.onNodeWithTag("legal_terms_link").performClick()
    rule.onNodeWithTag("legal_document_content").assertTextContains(LegalAcceptanceStore.CURRENT_TERMS_REVISION, substring = true)
    rule.onNodeWithTag("legal_reader_close").performClick()
    assertEquals(before, context.getSharedPreferences("legal_acceptance", Context.MODE_PRIVATE).all)
  }
  @Test fun offlineLicenseReaderDoesNotAcceptTerms() {
    rule.setContent { AppTheme { LegalDocumentLinks() } }
    rule.onNodeWithTag("source_licenses_link").performClick()
    rule.onNodeWithTag("source_licenses_content").assertIsDisplayed()
    rule.onNodeWithText("Open Source Client for NextDNS — source and dependency notices", substring = true).assertIsDisplayed()
    assertFalse(LegalAcceptanceStore(context).isAccepted())
    rule.onNodeWithTag("source_licenses_close").performClick()
    rule.onNodeWithTag("source_licenses_link").assertIsDisplayed()
    assertFalse(LegalAcceptanceStore(context).isAccepted())
  }
}
