package com.example.data.legal

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LegalAcceptanceStoreTest {
  private lateinit var context: Context

  @Before fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context.getSharedPreferences("legal_acceptance", Context.MODE_PRIVATE)
      .edit().clear().commit()
  }

  @After fun tearDown() {
    context.getSharedPreferences("legal_acceptance", Context.MODE_PRIVATE)
      .edit().clear().commit()
  }

  @Test fun freshInstall_blocksUntilExplicitAcceptance() {
    val store = LegalAcceptanceStore(context)
    assertFalse(store.isAccepted())
    assertTrue(store.acceptCurrentTerms())
    assertTrue(store.isAccepted())
  }

  @Test fun acceptancePersistsAcrossStoreRecreation() {
    assertTrue(LegalAcceptanceStore(context).acceptCurrentTerms())
    assertTrue(LegalAcceptanceStore(context).isAccepted())
  }

  @Test fun outdatedRevisionDoesNotUnlockGate() {
    context.getSharedPreferences("legal_acceptance", Context.MODE_PRIVATE)
      .edit().putString("accepted_terms_revision", "older-material-revision").commit()
    assertFalse(LegalAcceptanceStore(context).isAccepted())
  }
}
