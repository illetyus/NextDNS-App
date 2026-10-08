package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.api.*
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/** Android runtime fixture coverage; does not mutate any NextDNS account. */
@RunWith(AndroidJUnit4::class)
class NativeApiFixtureTest {
  @Test fun tlsMutationKeepsPayloadMinimalAndRejectsSemanticErrors() = runBlocking {
    val certificate = HeldCertificate.Builder().commonName("localhost").addSubjectAlternativeName("localhost").build()
    val serverCertificates = HandshakeCertificates.Builder().heldCertificate(certificate).build()
    val clientCertificates = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
    val server = MockWebServer()
    server.useHttps(serverCertificates.sslSocketFactory(), false)
    server.start()
    try {
      // Trust only the ephemeral fixture certificate in this test-only client.
      val client = NextDnsNetworkClient.client.newBuilder()
        .sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager).build()
      val api = Retrofit.Builder().baseUrl(server.url("/"))
        .client(client).addConverterFactory(MoshiConverterFactory.create(NextDnsNetworkClient.moshi))
        .build().create(NextDnsApiService::class.java)
      server.enqueue(MockResponse().setBody("""{"data":null}"""))
      val result = api.updateSecurity("synthetic-fixture-key", "fixture-profile", SecurityUpdateRequest(googleSafeBrowsing = false))
      assertTrue(result.body().isSemanticallySuccessful())
      val request = server.takeRequest(10, TimeUnit.SECONDS)!!
      assertEquals("PATCH", request.method)
      assertEquals("/profiles/fixture-profile/security", request.path)
      val payload = JSONObject(request.body.readUtf8())
      assertEquals(setOf("googleSafeBrowsing"), payload.keys().asSequence().toSet())
      assertFalse(payload.getBoolean("googleSafeBrowsing"))
      server.enqueue(MockResponse().setBody("""{"errors":[{"code":"invalid","detail":"Fixture rejection"}]}"""))
      assertFalse(api.updateSecurity("synthetic-fixture-key", "fixture-profile", SecurityUpdateRequest(googleSafeBrowsing = true)).body().isSemanticallySuccessful())
      server.enqueue(MockResponse().setResponseCode(401))
      assertFalse(api.getProfiles("synthetic-fixture-key").isSuccessful)
    } finally {
      server.shutdown()
    }
  }
}
