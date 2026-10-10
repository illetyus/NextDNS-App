package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.NextDnsApp
import com.example.data.api.NextDnsApiService
import com.example.data.api.NextDnsNetworkClient
import com.example.data.api.NextDnsTestResponse
import com.example.data.api.NextDnsTestService
import com.example.data.local.NextDnsPreferences
import com.example.data.model.NextDnsProfile
import com.example.data.security.ApiKeyProtector
import kotlinx.coroutines.*
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = NextDnsApp::class)
class RepositoryRegressionTest {
  private class FixtureProtector : ApiKeyProtector {
    override fun encrypt(plainText: String) = "fixture:$plainText"
    override fun decrypt(envelope: String) = envelope.removePrefix("fixture:")
  }

  private lateinit var server: MockWebServer
  private lateinit var preferences: NextDnsPreferences
  private lateinit var repository: NextDnsRepository
  private lateinit var api: NextDnsApiService
  private lateinit var testApi: NextDnsTestService
  private val requests = java.util.concurrent.CopyOnWriteArrayList<String>()
  private val gates = mutableListOf<CountDownLatch>()
  @Volatile private var response: (RecordedRequest) -> MockResponse = { json("{}", 404) }

  @Before fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.getSharedPreferences("legal_acceptance", Context.MODE_PRIVATE).edit().clear().commit()
    preferences = NextDnsPreferences(context, FixtureProtector())
    preferences.clear()
    preferences.apiKey = "fixture-account-a"
    preferences.activeProfileId = "aaaaaa"
    preferences.saveProfiles(listOf(NextDnsProfile("aaaaaa", "A"), NextDnsProfile("bbbbbb", "B")))
    runCatching { WorkManager.initialize(context, Configuration.Builder().build()) }
    server = MockWebServer()
    server.dispatcher = object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        requests.add("${request.method} ${request.requestUrl?.encodedPath}")
        return response(request)
      }
    }
    server.start()
    val retrofit = Retrofit.Builder().baseUrl(server.url("/"))
      .addConverterFactory(MoshiConverterFactory.create(NextDnsNetworkClient.moshi)).build()
    api = retrofit.create(NextDnsApiService::class.java)
    testApi = retrofit.create(NextDnsTestService::class.java)
    repository = newRepository()
  }

  private fun newRepository(probe: suspend (String?) -> NextDnsTestResponse? = { null }) =
    NextDnsRepository(preferences, apiService = api, diagnosticService = testApi,
      diagnosticProbe = probe, backgroundWorkEnabled = false)

  @After fun tearDown() {
    gates.forEach { it.countDown() }
    server.shutdown()
    preferences.clear()
  }

  private fun gate() = CountDownLatch(1).also { gates.add(it) }
  private fun await(latch: CountDownLatch) {
    assertTrue("Fixture request must reach its barrier", latch.await(5, TimeUnit.SECONDS))
  }
  private suspend fun assertCancelled(request: Deferred<*>) {
    try {
      request.await()
      fail("An obsolete request must not report success")
    } catch (_: CancellationException) { }
  }

  @Test fun lateLogsCannotPopulateAnotherProfile() = runBlocking {
    val entered = gate(); val release = gate()
    response = { entered.countDown(); await(release); json("""{"data":[{"domain":"old.example","timestamp":"2026-10-10T00:00:00Z","status":"default"}]}""") }
    val pending = async(Dispatchers.IO) { repository.refreshLogsFromApi() }
    await(entered)
    repository.setActiveProfile("bbbbbb")
    release.countDown()
    assertCancelled(pending)
    assertTrue(repository.logs.value.isEmpty())
    assertEquals("bbbbbb", preferences.activeProfileId)
  }

  @Test fun profileRoundTripStillRejectsOldLogs() = runBlocking {
    val entered = gate(); val release = gate()
    response = { entered.countDown(); await(release); json("""{"data":[{"domain":"old.example"}]}""") }
    val pending = async(Dispatchers.IO) { repository.refreshLogsFromApi() }
    await(entered)
    repository.setActiveProfile("bbbbbb")
    repository.setActiveProfile("aaaaaa")
    release.countDown()
    assertCancelled(pending)
    assertTrue(repository.logs.value.isEmpty())
  }

  @Test fun lateCreatedProfileCannotRestorePreferencesAfterLogout() = runBlocking {
    val entered = gate(); val release = gate()
    response = { req ->
      if (req.method == "POST") json("""{"data":{"id":"cccccc"}}""")
      else { entered.countDown(); await(release); json("""{"data":{"id":"cccccc","name":"Created"}}""") }
    }
    val pending = async(Dispatchers.IO) { repository.createProfileRemote("Created") }
    await(entered)
    assertTrue(repository.logout().isSuccess)
    release.countDown()
    assertCancelled(pending)
    assertTrue(repository.profiles.value.isEmpty())
    assertEquals("", preferences.apiKey)
    assertEquals("", preferences.activeProfileId)
    assertNull(preferences.getProfiles())
  }

  @Test fun mutationForACannotBeVerifiedUsingB() = runBlocking {
    val entered = gate(); val release = gate()
    response = { req ->
      if (req.method == "PATCH") { entered.countDown(); await(release); json("{}") }
      else json("""{"data":{"web3":true}}""")
    }
    val pending = async(Dispatchers.IO) { repository.setWeb3(true) }
    await(entered)
    repository.setActiveProfile("bbbbbb")
    release.countDown()
    assertCancelled(pending)
    assertFalse(requests.any { it.startsWith("GET /profiles/bbbbbb/") })
    assertFalse(repository.configSettings.value.web3)
  }

  @Test fun successfulMutationReadsAndCachesTheSameProfile() = runBlocking {
    response = { req -> if (req.method == "PATCH") json("{}") else json("""{"data":{"web3":true}}""") }
    assertTrue(repository.setWeb3(true).isSuccess)
    assertEquals(listOf("PATCH /profiles/aaaaaa/settings", "GET /profiles/aaaaaa/settings"), requests.toList())
    assertTrue(repository.configSettings.value.web3)
    assertEquals(true, preferences.getConfigSettings("aaaaaa")?.web3)
  }

  @Test fun web3TrueFalseAndMissingFieldPreserveAuthoritativeState() = runBlocking {
    response = { json("""{"data":{"web3":true}}""") }
    assertTrue(repository.refreshSection(SyncSection.SETTINGS))
    assertTrue(repository.configSettings.value.web3)
    response = { json("""{"data":{}}""") }
    repository.refreshSection(SyncSection.SETTINGS)
    assertTrue(repository.configSettings.value.web3)
    response = { json("""{"data":{"web3":false}}""") }
    repository.refreshSection(SyncSection.SETTINGS)
    assertFalse(repository.configSettings.value.web3)
    repository.loadLocalProfileData("aaaaaa")
    assertFalse(repository.configSettings.value.web3)
  }

  @Test fun diagnosticTimeoutDoesNotReplaceLastSuccessWithFreshSuccess() = runBlocking {
    var answer: NextDnsTestResponse? = NextDnsTestResponse(status = "ok", profile = "aaaaaa", protocol = "DOT")
    repository = newRepository { answer }
    val success = repository.runDiagnosticTest("aaaaaa")
    answer = null
    val failure = repository.runDiagnosticTest("aaaaaa")
    assertEquals("error", failure.status)
    assertNotNull(failure.errorMessage)
    assertEquals(success.lastTestedTime, failure.lastTestedTime)
    assertFalse(failure.isTesting)
  }

  @Test fun cancellingRefreshClearsItsBusyState() = runBlocking {
    val entered = gate(); val release = gate()
    response = { entered.countDown(); await(release); json("""{"data":{}}""") }
    val pending = async(Dispatchers.IO) { repository.refreshSection(SyncSection.SETTINGS) }
    await(entered)
    assertTrue(repository.sectionSyncStates.value.getValue(SyncSection.SETTINGS).isRefreshing)
    pending.cancelAndJoin()
    release.countDown()
    assertFalse(repository.sectionSyncStates.value.getValue(SyncSection.SETTINGS).isRefreshing)
  }

  @Test fun malformedExportLinkReturnsFailureInsteadOfThrowing() = runBlocking {
    response = { json("""{"data":{"url":"https://"}}""") }
    assertTrue(repository.exportLogs(ByteArrayOutputStream()).isFailure)
  }

  @Test fun successfulHttpReadDoesNotVerifyAnUnappliedMutation() = runBlocking {
    response = { req -> if (req.method == "PATCH") json("{}") else json("""{"data":{"web3":false}}""") }
    assertTrue(repository.setWeb3(true).isFailure)
    assertFalse(repository.configSettings.value.web3)
    assertFalse(repository.sectionSyncStates.value.getValue(SyncSection.SETTINGS).isSaving)
  }

  @Test fun obsoleteSettingsCannotWriteAnotherProfilesCache() = runBlocking {
    val entered = gate(); val release = gate()
    response = { entered.countDown(); await(release); json("""{"data":{"web3":true}}""") }
    val pending = async(Dispatchers.IO) { repository.refreshSection(SyncSection.SETTINGS) }
    await(entered)
    repository.setActiveProfile("bbbbbb")
    release.countDown()
    assertCancelled(pending)
    assertNull(preferences.getConfigSettings("aaaaaa"))
    assertNull(preferences.getConfigSettings("bbbbbb"))
    assertFalse(repository.configSettings.value.web3)
  }

  @Test fun oldRefreshCancellationCannotClearNewRefreshFlag() = runBlocking {
    val firstEntered = gate(); val secondEntered = gate()
    val firstRelease = gate(); val secondRelease = gate()
    val count = java.util.concurrent.atomic.AtomicInteger()
    response = {
      if (count.incrementAndGet() == 1) { firstEntered.countDown(); await(firstRelease) }
      else { secondEntered.countDown(); await(secondRelease) }
      json("""{"data":{"web3":true}}""")
    }
    val first = async(Dispatchers.IO) { repository.refreshSection(SyncSection.SETTINGS) }
    await(firstEntered)
    val second = async(Dispatchers.IO) { repository.refreshSection(SyncSection.SETTINGS) }
    await(secondEntered)
    first.cancelAndJoin()
    firstRelease.countDown()
    assertTrue(repository.sectionSyncStates.value.getValue(SyncSection.SETTINGS).isRefreshing)
    secondRelease.countDown()
    assertTrue(second.await())
    assertFalse(repository.sectionSyncStates.value.getValue(SyncSection.SETTINGS).isRefreshing)
  }

  @Test fun differentSectionRefreshesKeepBothFlagsAndResults() = runBlocking {
    val settingsEntered = gate(); val securityEntered = gate(); val release = gate()
    response = { request ->
      if (request.requestUrl!!.encodedPath.endsWith("settings")) settingsEntered.countDown() else securityEntered.countDown()
      await(release)
      json("""{"data":{"web3":true,"ddns":true}}""")
    }
    val settings = async(Dispatchers.IO) { repository.refreshSection(SyncSection.SETTINGS) }
    val security = async(Dispatchers.IO) { repository.refreshSection(SyncSection.SECURITY) }
    await(settingsEntered); await(securityEntered)
    assertTrue(repository.sectionSyncStates.value.getValue(SyncSection.SETTINGS).isRefreshing)
    assertTrue(repository.sectionSyncStates.value.getValue(SyncSection.SECURITY).isRefreshing)
    release.countDown()
    assertTrue(settings.await()); assertTrue(security.await())
    assertTrue(repository.configSettings.value.web3)
    assertTrue(repository.securitySettings.value.ddns)
  }

  companion object {
    private fun json(body: String, code: Int = 200) = MockResponse()
      .setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)
  }
}
