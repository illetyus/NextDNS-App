package com.example.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.security.ApiKeyProtector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NextDnsPreferencesClearTest {
  private class FakeProtector : ApiKeyProtector {
    override fun encrypt(plainText: String) = "encrypted:$plainText"
    override fun decrypt(envelope: String) = envelope.removePrefix("encrypted:")
  }

  @Test fun signOutRemovesCredentialAndCachedProfileAtomically() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.getSharedPreferences("nextdns_secure_prefs", Context.MODE_PRIVATE)
      .edit().clear().commit()
    val prefs = NextDnsPreferences(context, FakeProtector())
    prefs.apiKey = "sample-test-key"
    prefs.activeProfileId = "profile-1"
    assertTrue(prefs.clear())
    assertTrue(prefs.apiKey.isBlank())
    assertTrue(prefs.activeProfileId.isBlank())
    assertFalse(context.getSharedPreferences("nextdns_secure_prefs", Context.MODE_PRIVATE)
      .contains("saved_api_key_encrypted_v1"))
  }
}
