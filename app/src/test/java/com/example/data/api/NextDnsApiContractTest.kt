package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class NextDnsApiContractTest {

  private lateinit var server: MockWebServer
  private lateinit var api: NextDnsApiService

  @Before
  fun setUp() {
    server = MockWebServer()
    server.start()

    val moshi = Moshi.Builder()
      .add(KotlinJsonAdapterFactory())
      .build()

    api = Retrofit.Builder()
      .baseUrl(server.url("/"))
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(NextDnsApiService::class.java)
  }

  @After
  fun tearDown() {
    server.shutdown()
  }

  @Test
  fun profileCreate_acceptsIdOnlyResponse() = runTest {
    server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody("""{"data":{"id":"abc123"}}""")
    )

    val response = api.createProfile("test-key", NameRequest("Test Profile"))

    assertTrue(response.isSuccessful)
    assertEquals("abc123", response.body()?.data?.id)
    assertFalse(response.body().hasApiErrors())
  }

  @Test
  fun http200WithErrors_isNotSemanticSuccess() = runTest {
    server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(
          """{"errors":[{"code":"invalid","detail":"Invalid profile name","source":{"parameter":"name"}}]}"""
        )
    )

    val response = api.createProfile("test-key", NameRequest("Bad Profile"))

    assertTrue(response.isSuccessful)
    assertTrue(response.body().hasApiErrors())
    assertFalse(response.body().isSemanticallySuccessful())
    assertEquals("name", response.body()?.errors?.firstOrNull()?.source?.parameter)
  }

  @Test
  fun logsResponse_parsesPaginationAndStreamMetadata() = runTest {
    server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(
          """{"data":[],"meta":{"pagination":{"cursor":"cursor-2"},"stream":{"id":"stream-42"}}}"""
        )
    )

    val response = api.getLogs(
      apiKey = "test-key",
      profileId = "abcdef",
      limit = 100,
      from = "-1h",
      to = "now",
      sort = "asc",
      cursor = "cursor-1",
      raw = 1
    )

    assertTrue(response.isSuccessful)
    assertEquals("cursor-2", response.body()?.meta?.pagination?.cursor)
    assertEquals("stream-42", response.body()?.meta?.stream?.id)

    val request = server.takeRequest()
    val path = request.path.orEmpty()
    assertTrue(path.contains("from=-1h"))
    assertTrue(path.contains("to=now"))
    assertTrue(path.contains("sort=asc"))
    assertTrue(path.contains("cursor=cursor-1"))
    assertFalse(path.contains("before="))
  }

  @Test
  fun configurationGets_parseNextDnsDataEnvelope() = runTest {
    server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(
          """{"data":{"threatIntelligenceFeeds":false,"googleSafeBrowsing":true,"tlds":[{"id":"ru"}]}}"""
        )
    )
    server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(
          """{"data":{"disguisedTrackers":true,"allowAffiliate":false,"blocklists":[{"id":"oisd"}],"natives":[{"id":"apple"}]}}"""
        )
    )
    server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(
          """{"data":{"safeSearch":true,"youtubeRestrictedMode":false,"blockBypass":false,"services":[{"id":"tiktok","active":true}],"categories":[{"id":"porn","active":true}]}}"""
        )
    )

    val security = api.getSecurity("test-key", "profile-1")
    val privacy = api.getPrivacy("test-key", "profile-1")
    val parental = api.getParentalControl("test-key", "profile-1")

    assertTrue(security.body().isSemanticallySuccessful())
    assertEquals(false, security.body()?.data?.threatIntelligenceFeeds)
    assertEquals("ru", security.body()?.data?.tlds?.firstOrNull()?.id)

    assertTrue(privacy.body().isSemanticallySuccessful())
    assertEquals("oisd", privacy.body()?.data?.blocklists?.firstOrNull()?.id)
    assertEquals(true, privacy.body()?.data?.disguisedTrackers)

    assertTrue(parental.body().isSemanticallySuccessful())
    assertEquals("tiktok", parental.body()?.data?.services?.firstOrNull()?.id)
    assertEquals(false, parental.body()?.data?.blockBypass)
  }

  @Test
  fun profilesRequest_supportsCursorPagination() = runTest {
    server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(
          """{"data":[{"id":"p2","name":"Second"}],"meta":{"pagination":{"cursor":null}}}"""
        )
    )

    val response = api.getProfiles("test-key", cursor = "cursor-1")

    assertTrue(response.isSuccessful)
    assertEquals("p2", response.body()?.data?.firstOrNull()?.id)

    val request = server.takeRequest()
    assertTrue(request.path.orEmpty().contains("cursor=cursor-1"))
  }

  @Test
  fun retentionCodec_usesSecondsAtApiBoundary() {
    assertEquals(21_600, LogRetentionCodec.toSeconds("6 saat"))
    assertEquals(86_400, LogRetentionCodec.toSeconds("1 gün"))
    assertEquals(604_800, LogRetentionCodec.toSeconds("1 hafta"))
    assertEquals(2_592_000, LogRetentionCodec.toSeconds("1 ay"))
    assertEquals(7_776_000, LogRetentionCodec.toSeconds("3 ay"))
    assertEquals(15_552_000, LogRetentionCodec.toSeconds("6 ay"))
    assertEquals(31_536_000, LogRetentionCodec.toSeconds("1 yıl"))
    assertEquals(63_072_000, LogRetentionCodec.toSeconds("2 yıl"))

    assertEquals("3 ay", LogRetentionCodec.toLabel(7_776_000))
    assertNull(LogRetentionCodec.toLabel(123))
    assertNull(LogRetentionCodec.toSeconds("Bilinmiyor"))
  }
}
