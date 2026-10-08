package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.NextDnsPreferences
import com.example.data.security.ApiKeyProtector
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LegalRepositoryGateTest {
  private class FakeProtector : ApiKeyProtector {
    override fun encrypt(plainText: String) = "enc:$plainText"
    override fun decrypt(envelope: String) = envelope.removePrefix("enc:")
  }

  @Test
  fun retainedApiKeyDoesNotStartAccountConnectionWithoutCurrentTerms() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.getSharedPreferences("legal_acceptance", Context.MODE_PRIVATE)
      .edit().clear().commit()
    context.getSharedPreferences("nextdns_secure_prefs", Context.MODE_PRIVATE)
      .edit().clear().commit()

    try {
      val prefs = NextDnsPreferences(context, FakeProtector())
      prefs.apiKey = "nonproduction-test-key"
      prefs.activeProfileId = "test-profile"

      val repository = NextDnsRepository(prefs)
      assertEquals(ApiConnectionStatus.Disconnected, repository.apiStatus.value)
      assertTrue(repository.loginWithApiKey("nonproduction-test-key").isFailure)
      assertEquals(ApiConnectionStatus.Disconnected, repository.apiStatus.value)
    } finally {
      context.getSharedPreferences("legal_acceptance", Context.MODE_PRIVATE)
        .edit().clear().commit()
      context.getSharedPreferences("nextdns_secure_prefs", Context.MODE_PRIVATE)
        .edit().clear().commit()
    }
  }
}
