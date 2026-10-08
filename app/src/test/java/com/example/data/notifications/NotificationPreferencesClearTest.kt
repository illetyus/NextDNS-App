package com.example.data.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationPreferencesClearTest {
  private lateinit var store: NotificationPreferences

  @Before fun setUp() = runBlocking {
    store = NotificationPreferences(ApplicationProvider.getApplicationContext<Context>())
    store.clearAccountState()
  }

  @After fun tearDown() = runBlocking {
    store.clearAccountState()
  }

  @Test fun signOutPurgesTogglesDigestsDatesAndBaselines() = runBlocking {
    store.setConfigChangeAlertsEnabled(true)
    store.setDailySummaryEnabled(true)
    store.establishConfigBaseline("profile-A", "sensitive-hash")
    store.markConfigObserved("profile-A", "new-hash")
    store.markConfigNotified("profile-A", "new-hash", 12345L)
    store.suppressConfigForLocalMutation("profile-A", 54321L)
    store.markSummaryDay("2026-10-08")

    assertTrue(store.currentSettings().anyEnabled)
    assertEquals("new-hash", store.configState("profile-A").observedDigest)
    assertEquals("2026-10-08", store.lastSummaryDay())

    store.clearAccountState()

    assertFalse(store.currentSettings().anyEnabled)
    val cleared = store.configState("profile-A")
    assertNull(cleared.observedDigest)
    assertNull(cleared.lastNotifiedDigest)
    assertEquals(0L, cleared.lastNotifiedAt)
    assertEquals(0L, cleared.localMutationSuppressUntil)
    assertNull(store.lastSummaryDay())
  }
}
