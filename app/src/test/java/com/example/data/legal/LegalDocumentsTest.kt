package com.example.data.legal

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LegalDocumentsTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  private fun asset(path: String): String = context.assets.open("legal/$path").bufferedReader().use { it.readText() }

  @Test fun allFiveTermsAndEnglishPrivacyPassContentVerification() {
    for (language in listOf("tr", "en", "de", "fr", "es")) {
      val bundle = LegalDocuments.load(context, language).getOrThrow()
      assertEquals(language, bundle.language)
      assertTrue(bundle.draft)
      assertTrue(bundle.terms.contains("Revision: ${LegalAcceptanceStore.CURRENT_TERMS_REVISION}"))
      assertTrue(bundle.privacy.contains("Privacy Policy"))
    }
    assertEquals("en", LegalDocuments.load(context, "ja").getOrThrow().language)
  }

  @Test fun changedMissingOrWrongRevisionAssetsFailClosed() {
    assertTrue(LegalDocuments.readBundle("en") { if (it == "terms_en.txt") asset(it)+"changed" else asset(it) }.isFailure)
    assertTrue(LegalDocuments.readBundle("en") { if (it == "privacy_en.txt") error("Missing") else asset(it) }.isFailure)
    assertTrue(LegalDocuments.readBundle("en") { asset(it).replace(LegalAcceptanceStore.CURRENT_TERMS_REVISION,"old-revision") }.isFailure)
  }
}
