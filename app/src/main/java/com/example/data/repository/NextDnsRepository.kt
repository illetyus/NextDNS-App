package com.example.data.repository

import android.util.Log
import com.example.NextDnsApp
import com.example.data.api.*
import com.example.data.local.NextDnsPreferences
import com.example.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.Locale
import okhttp3.Request
import retrofit2.Response

sealed interface ApiConnectionStatus {
  object Disconnected : ApiConnectionStatus
  object Connecting : ApiConnectionStatus
  data class Connected(val profileCount: Int) : ApiConnectionStatus
  data class Error(val message: String) : ApiConnectionStatus
}

private class ProfilesFetchException(
  val httpCode: Int,
  message: String
) : Exception(message)

class NextDnsRepository(
  private val preferences: NextDnsPreferences = NextDnsApp.preferences
) {
  private val TAG = "NextDnsRepo"
  private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var streamJob: Job? = null
  private var analyticsPollingJob: Job? = null
  @Volatile private var logsStreamSeedId: String? = null
  @Volatile private var nextLogsCursor: String? = null

  private val _apiKey = MutableStateFlow(preferences.apiKey)
  val apiKey = _apiKey.asStateFlow()

  private val _apiStatus = MutableStateFlow<ApiConnectionStatus>(
    if (preferences.apiKey.isNotBlank()) ApiConnectionStatus.Connecting else ApiConnectionStatus.Disconnected
  )
  val apiStatus = _apiStatus.asStateFlow()

  private val _profiles = MutableStateFlow<List<NextDnsProfile>>(preferences.getProfiles() ?: emptyList())
  val profiles = _profiles.asStateFlow()

  private val _activeProfileId = MutableStateFlow(preferences.activeProfileId)
  val activeProfileId = _activeProfileId.asStateFlow()

  private val _securitySettings = MutableStateFlow(SecuritySettings())
  val securitySettings = _securitySettings.asStateFlow()

  private val _privacySettings = MutableStateFlow(PrivacySettings())
  val privacySettings = _privacySettings.asStateFlow()

  private val _parentalControlSettings = MutableStateFlow(ParentalControlSettings())
  val parentalControlSettings = _parentalControlSettings.asStateFlow()

  private val _denylist = MutableStateFlow<List<AllowDenyItem>>(emptyList())
  val denylist = _denylist.asStateFlow()

  private val _allowlist = MutableStateFlow<List<AllowDenyItem>>(emptyList())
  val allowlist = _allowlist.asStateFlow()

  private val _logs = MutableStateFlow<List<DnsLogEntry>>(emptyList())
  val logs = _logs.asStateFlow()

  private val _analytics = MutableStateFlow(AnalyticsSummary())
  val analytics = _analytics.asStateFlow()

  private val _knownDeviceNameToId = java.util.concurrent.ConcurrentHashMap<String, String>()
  private val _knownDeviceIdToName = java.util.concurrent.ConcurrentHashMap<String, String>()

  private val _allKnownDevices = MutableStateFlow<List<String>>(emptyList())
  val allKnownDevices = _allKnownDevices.asStateFlow()

  private var currentAnalyticsDevice: String? = null
  private var currentAnalyticsTime: String? = null

  fun updateKnownDevices(newDevices: List<String>) {
    val valid = newDevices.filter { it.isNotBlank() && it != "Bilinmeyen" && it != "Cihaz" && it != "Bilinmeyen Cihaz" }
    if (valid.isNotEmpty()) {
      val current = _allKnownDevices.value.toMutableSet()
      current.addAll(valid)
      _allKnownDevices.value = current.toList()
    }
  }

  private val _configSettings = MutableStateFlow(ConfigSettings())
  val configSettings = _configSettings.asStateFlow()

  private val _testResult = MutableStateFlow(DiagnosticTestResult())
  val testResult = _testResult.asStateFlow()

  private val _isLiveStreaming = MutableStateFlow(false)
  val isLiveStreaming = _isLiveStreaming.asStateFlow()

  private val _isSyncing = MutableStateFlow(false)
  val isSyncing = _isSyncing.asStateFlow()

  private val _sectionSyncStates = MutableStateFlow(
    SyncSection.values().associateWith { SectionSyncState() }
  )
  val sectionSyncStates = _sectionSyncStates.asStateFlow()

  private fun resetProfileScopedRuntimeState() {
    stopLogsStream()
    logsStreamSeedId = null
    nextLogsCursor = null
    currentAnalyticsDevice = null
    currentAnalyticsTime = null
    _knownDeviceNameToId.clear()
    _knownDeviceIdToName.clear()
    _allKnownDevices.value = emptyList()
    _sectionSyncStates.value = SyncSection.values().associateWith { SectionSyncState() }
  }

  private fun updateSectionSyncState(
    section: SyncSection,
    transform: (SectionSyncState) -> SectionSyncState
  ) {
    val current = _sectionSyncStates.value
    val state = current[section] ?: SectionSyncState()
    _sectionSyncStates.value = current + (section to transform(state))
  }

  suspend fun refreshSection(section: SyncSection): Boolean {
    val key = _apiKey.value
    val profileId = _activeProfileId.value
    if (key.isBlank() || profileId.isBlank()) return false

    val attemptAt = System.currentTimeMillis()
    updateSectionSyncState(section) {
      it.copy(isRefreshing = true, lastAttemptAt = attemptAt, errorMessage = null)
    }

    val success = try {
      when (section) {
        SyncSection.SETUP -> applySetupFromApi(key, profileId)
        SyncSection.SECURITY -> applySecuritySettingsFromApi(key, profileId)
        SyncSection.PRIVACY -> applyPrivacySettingsFromApi(key, profileId)
        SyncSection.PARENTAL -> applyParentalSettingsFromApi(key, profileId)
        SyncSection.DENYLIST -> applyDenylistFromApi(key, profileId)
        SyncSection.ALLOWLIST -> applyAllowlistFromApi(key, profileId)
        SyncSection.SETTINGS -> applyConfigSettingsFromApi(key, profileId)
        SyncSection.ACCOUNT -> applyAccountFromApi(key)
      }
    } catch (cancel: CancellationException) {
      throw cancel
    } catch (error: Exception) {
      Log.e(TAG, "[refreshSection] error", error)
      false
    }

    updateSectionSyncState(section) { previous ->
      if (success) {
        previous.copy(
          isRefreshing = false,
          lastSuccessAt = System.currentTimeMillis(),
          errorMessage = null
        )
      } else {
        previous.copy(
          isRefreshing = false,
          errorMessage = "NextDNS verisi yenilenemedi."
        )
      }
    }
    return success
  }

  // Live NextDNS Public Catalogs & Account Metadata
  private val _availableBlocklistsCatalog = MutableStateFlow<List<BlocklistEntry>>(emptyList())
  val availableBlocklistsCatalog = _availableBlocklistsCatalog.asStateFlow()

  private val _availableNativesCatalog = MutableStateFlow<List<NativeTrackingDto>>(emptyList())
  val availableNativesCatalog = _availableNativesCatalog.asStateFlow()

  private val _availableParentalServicesCatalog = MutableStateFlow<List<ParentalServiceCatalogDto>>(emptyList())
  val availableParentalServicesCatalog = _availableParentalServicesCatalog.asStateFlow()

  private val _availableParentalCategoriesCatalog = MutableStateFlow<List<ParentalCategoryDto>>(emptyList())
  val availableParentalCategoriesCatalog = _availableParentalCategoriesCatalog.asStateFlow()

  private val _availableTldsCatalog = MutableStateFlow<List<SecurityTldCatalogDto>>(emptyList())
  val availableTldsCatalog = _availableTldsCatalog.asStateFlow()

  private val _accountInfo = MutableStateFlow(NextDnsAccountInfo())
  val accountInfo = _accountInfo.asStateFlow()

  private val _profileSetup = MutableStateFlow(SetupDto())
  val profileSetup = _profileSetup.asStateFlow()

  init {
    repoScope.launch {
      loadAllLiveCatalogs()
      runDiagnosticTest()
      val savedKey = preferences.apiKey
      val savedPid = preferences.activeProfileId
      if (savedKey.isNotBlank()) {
        loginWithApiKey(savedKey, restoreProfileId = savedPid)
      } else if (savedPid.isNotBlank()) {
        loadLocalProfileData(savedPid)
      }
    }
  }

  // =========================================================================
  // Safe API Helper (Eliminates try/catch duplication and bumpy road)
  // =========================================================================

  private suspend fun <T> safeApiCall(operationName: String, block: suspend () -> T): T? {
    return try {
      block()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "[$operationName] error: ${e.message}", e)
      null
    }
  }

  suspend fun loadAllLiveCatalogs() = withContext(Dispatchers.IO) {
    try {
      // 1. Blocklists Catalog
      val blocklistDtos = NextDnsNetworkClient.fetchAvailableBlocklistsDirect()
      if (blocklistDtos != null && blocklistDtos.isNotEmpty()) {
        _availableBlocklistsCatalog.value = blocklistDtos.map { dto ->
          val cleanId = dto.id.lowercase().trim()
          val isRecommended = cleanId == "nextdns-recommended"
          BlocklistEntry(
            id = dto.id,
            name = dto.name?.takeIf { it.isNotBlank() } ?: (if (isRecommended) "NextDNS Reklam & İzleyici Koruması" else dto.id),
            description = dto.description ?: (if (isRecommended) "NextDNS tarafından optimize edilmiş dengeli ve kapsamlı engelleme listesi." else ""),
            entriesCount = dto.entries ?: 0L,
            active = false,
            website = dto.website ?: (if (isRecommended) "https://nextdns.io" else ""),
            category = determineBlocklistCategory(cleanId, dto.name ?: "", dto.description ?: ""),
            updatedTime = formatIsoDateWithRelative(dto.updatedOn)
          )
        }
      }

      // 2. Natives Catalog
      val nativesDtos = NextDnsNetworkClient.fetchAvailableNativesDirect()
      if (nativesDtos != null && nativesDtos.isNotEmpty()) {
        _availableNativesCatalog.value = nativesDtos
      }

      // 3. Parental Services Catalog
      val parentServices = NextDnsNetworkClient.fetchAvailableParentalServicesDirect()
      if (parentServices != null && parentServices.isNotEmpty()) {
        _availableParentalServicesCatalog.value = parentServices
      }

      // 4. Parental Categories Catalog
      val parentCats = NextDnsNetworkClient.fetchAvailableParentalCategoriesDirect()
      if (parentCats != null && parentCats.isNotEmpty()) {
        _availableParentalCategoriesCatalog.value = parentCats
      }

      // 5. TLDs Catalog
      val tlds = NextDnsNetworkClient.fetchAvailableTldsDirect()
      if (tlds != null && tlds.isNotEmpty()) {
        _availableTldsCatalog.value = tlds
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error loading live catalogs: ${e.message}", e)
    }
  }

  fun loadLocalProfileData(profileId: String) {
    _securitySettings.value = preferences.getSecuritySettings(profileId) ?: SecuritySettings()
    _privacySettings.value = preferences.getPrivacySettings(profileId) ?: PrivacySettings()
    _parentalControlSettings.value = preferences.getParentalControlSettings(profileId) ?: ParentalControlSettings()
    _denylist.value = preferences.getDenylist(profileId) ?: emptyList()
    _allowlist.value = preferences.getAllowlist(profileId) ?: emptyList()
    _configSettings.value = preferences.getConfigSettings(profileId) ?: ConfigSettings()
    _logs.value = preferences.getLogs(profileId) ?: emptyList()
  }

  // =========================================================================
  // Diagnostic Tests
  // =========================================================================

  suspend fun runDiagnosticTest(targetProfileId: String? = null): DiagnosticTestResult = withContext(Dispatchers.IO) {
    _testResult.value = _testResult.value.copy(isTesting = true)
    val startT = System.currentTimeMillis()

    val profId = targetProfileId ?: _activeProfileId.value.takeIf { it.isNotBlank() }

    // Direct OkHttp with random subdomain per profile matching NextDNS website
    var body = NextDnsNetworkClient.fetchTestConnectionDirect(profId)

    // Fallback if needed
    if (body == null) {
      body = safeApiCall("runDiagnosticTest") {
        val resp = NextDnsNetworkClient.testApi.testConnection()
        if (resp.isSuccessful) resp.body() else null
      }
    }

    val latency = (System.currentTimeMillis() - startT).toInt().coerceAtLeast(1)

    val result = if (body != null) {
      val rawStatus = (body.status ?: "unconfigured").lowercase().trim()
      val clientIp = body.client?.takeIf { it.isNotBlank() }
        ?: body.srcIP?.takeIf { it.isNotBlank() }
        ?: ""
      val isEnc = body.protocol?.uppercase() in listOf("DOH", "DOT", "DOQ")

      DiagnosticTestResult(
        status = rawStatus,
        protocol = body.protocol ?: if (rawStatus == "ok") "DoH" else "",
        profileId = body.profile ?: "",
        clientIp = clientIp,
        resolver = body.resolver,
        serverPoP = body.server ?: "",
        latencyMs = latency,
        isEncrypted = isEnc,
        isTesting = false,
        lastTestedTime = System.currentTimeMillis()
      )
    } else {
      _testResult.value.copy(
        isTesting = false,
        lastTestedTime = System.currentTimeMillis(),
        errorMessage = "Sunucuya bağlanılamadı"
      )
    }

    _testResult.value = result
    result
  }

  suspend fun linkCurrentIp(profileId: String): Boolean = withContext(Dispatchers.IO) {
    val ok = NextDnsNetworkClient.linkIpAddress(profileId)
    runDiagnosticTest()
    ok
  }

  // =========================================================================
  // Authentication & Profile Fetching
  // =========================================================================

  private suspend fun fetchAllProfilesFromApi(key: String): Result<List<ProfileDto>> {
    val profiles = mutableListOf<ProfileDto>()
    val seenCursors = mutableSetOf<String>()
    var cursor: String? = null

    do {
      val response = safeApiCall("getProfiles") {
        NextDnsNetworkClient.api.getProfiles(key, cursor)
      } ?: return Result.failure(
        ProfilesFetchException(-1, "NextDNS profil listesine bağlanılamadı.")
      )

      val body = response.body()
      if (!response.isSuccessful || body.hasApiErrors()) {
        val detail = body?.errors?.firstOrNull()?.detail
          ?: "NextDNS profil listesi alınamadı."
        return Result.failure(ProfilesFetchException(response.code(), detail))
      }

      profiles += body?.data.orEmpty()
      val nextCursor = body?.meta?.pagination?.cursor
      cursor = if (!nextCursor.isNullOrBlank() && seenCursors.add(nextCursor)) nextCursor else null
    } while (cursor != null)

    return Result.success(profiles.distinctBy { it.id })
  }

  suspend fun loginWithApiKey(key: String, restoreProfileId: String? = null): Result<Int> = withContext(Dispatchers.IO) {
    _apiStatus.value = ApiConnectionStatus.Connecting

    val profilesResult = fetchAllProfilesFromApi(key)
    if (profilesResult.isFailure) {
      val cause = profilesResult.exceptionOrNull()
      val code = (cause as? ProfilesFetchException)?.httpCode ?: -1
      val errorMsg = when (code) {
        401 -> "API Anahtarı geçersiz (401 Yetkisiz). Lütfen my.nextdns.io/account adresinden anahtarınızı kontrol edin."
        403 -> "Erişim engellendi (403 Yasak). Lütfen API anahtarınızı kontrol edin."
        else -> cause?.message ?: "NextDNS API sunucusuna bağlanılamadı."
      }
      _apiStatus.value = ApiConnectionStatus.Error(errorMsg)
      return@withContext Result.failure(Exception(errorMsg))
    }

    val apiProfiles = profilesResult.getOrThrow()
    if (apiProfiles.isEmpty()) {
      val msg = "Hesabınızda hiçbir NextDNS profili bulunamadı."
      _apiStatus.value = ApiConnectionStatus.Error(msg)
      return@withContext Result.failure(Exception(msg))
    }

    _apiKey.value = key
    preferences.apiKey = key

    val mapped = apiProfiles.map {
      NextDnsProfile(id = it.id, name = it.name, fingerprint = it.fingerprint ?: "")
    }

    _profiles.value = mapped
    preferences.saveProfiles(mapped)

    val targetProfileId = if (!restoreProfileId.isNullOrBlank() && mapped.any { it.id == restoreProfileId }) {
      restoreProfileId
    } else {
      mapped.first().id
    }

    resetProfileScopedRuntimeState()
    _activeProfileId.value = targetProfileId
    preferences.activeProfileId = targetProfileId
    _apiStatus.value = ApiConnectionStatus.Connected(mapped.size)

    loadLocalProfileData(targetProfileId)
    loadActiveProfileDataFromApi(key, targetProfileId)
    Result.success(mapped.size)
  }

  suspend fun refreshProfilesFromApi(): Boolean = withContext(Dispatchers.IO) {
    val key = _apiKey.value
    if (key.isBlank()) return@withContext false

    val result = fetchAllProfilesFromApi(key)
    if (result.isFailure) return@withContext false

    val remoteProfiles = result.getOrThrow().map {
      NextDnsProfile(
        id = it.id,
        name = it.name,
        fingerprint = it.fingerprint ?: ""
      )
    }

    val currentActiveId = _activeProfileId.value
    _profiles.value = remoteProfiles
    preferences.saveProfiles(remoteProfiles)
    _apiStatus.value = ApiConnectionStatus.Connected(remoteProfiles.size)

    if (remoteProfiles.isEmpty()) {
      resetProfileScopedRuntimeState()
      _activeProfileId.value = ""
      preferences.activeProfileId = ""
      return@withContext true
    }

    if (remoteProfiles.none { it.id == currentActiveId }) {
      val replacement = remoteProfiles.first().id
      resetProfileScopedRuntimeState()
      _activeProfileId.value = replacement
      preferences.activeProfileId = replacement
      loadLocalProfileData(replacement)
      loadActiveProfileDataFromApi(key, replacement)
    }

    true
  }

  suspend fun loadActiveProfileDataFromApi(key: String = _apiKey.value, profileId: String = _activeProfileId.value) = withContext(Dispatchers.IO) {
    if (key.isBlank() || profileId.isBlank()) return@withContext
    _isSyncing.value = true

    try {
      SyncSection.values().forEach { section ->
        refreshSection(section)
      }
      applyLogsFromApi(key, profileId)
      applyDevicesAnalyticsFromApi(key, profileId)
    } finally {
      fetchAnalytics(key, profileId, null, null)
      _isSyncing.value = false
    }
  }

  fun updateUserEmail(email: String, name: String = "") {
    preferences.userEmail = email
    if (name.isNotBlank()) preferences.userName = name
    val current = _accountInfo.value
    _accountInfo.value = current.copy(
      email = email,
      name = name.ifBlank { current.name ?: email.substringBefore("@").replaceFirstChar { it.uppercase() } }
    )
  }

  private suspend fun applyAccountFromApi(key: String): Boolean {
    try {
      val accResp = NextDnsNetworkClient.api.getAccount(key)
      val body = accResp.body()
      if (accResp.isSuccessful && body.isSemanticallySuccessful()) {
        val d = body?.data
        if (d != null) {
          val email = d.email?.takeIf { it.isNotBlank() } ?: preferences.userEmail.takeIf { it.isNotBlank() }
          val name = d.name?.takeIf { it.isNotBlank() } ?: preferences.userName.takeIf { it.isNotBlank() } ?: email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
          _accountInfo.value = NextDnsAccountInfo(
            email = email,
            name = name,
            plan = d.plan,
            subscriptionStatus = d.subscription?.status,
            subscriptionPeriod = d.subscription?.period
          )
          return true
        }
      }
    } catch (_: Exception) {
      // /account is undocumented/non-contractual; keep a local fallback.
    }

    val email = preferences.userEmail.takeIf { it.isNotBlank() }
    val name = preferences.userName.takeIf { it.isNotBlank() } ?: email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
    _accountInfo.value = NextDnsAccountInfo(
      email = email,
      name = name,
      plan = null,
      subscriptionStatus = null,
      subscriptionPeriod = null
    )
    return false
  }

  private suspend fun applySetupFromApi(key: String, profileId: String): Boolean {
    val setupResp = safeApiCall("getProfileSetup") {
      NextDnsNetworkClient.api.getProfileSetup(key, profileId)
    } ?: return false
    if (!setupResp.isSuccessful) return false
    val d = setupResp.body() ?: return false
    _profileSetup.value = d
    return true
  }

  // =========================================================================
  // Settings Loading Sub-Routines (Guard Clause & Single Responsibility)
  // =========================================================================

  private suspend fun applySecuritySettingsFromApi(key: String, profileId: String): Boolean {
    val secResp = safeApiCall("applySecuritySettings") {
      NextDnsNetworkClient.api.getSecurity(key, profileId)
    } ?: return false
    val body = secResp.body() ?: return false
    if (!secResp.isSuccessful || body.hasApiErrors()) return false
    val d = body.data ?: return false
    val previous = _securitySettings.value

    val tldList = d.tlds?.map { it.id } ?: previous.blockedTlds
    val updated = SecuritySettings(
      threatIntelligenceFeeds = d.threatIntelligenceFeeds ?: previous.threatIntelligenceFeeds,
      aiThreatDetection = d.aiThreatDetection ?: previous.aiThreatDetection,
      googleSafeBrowsing = d.googleSafeBrowsing ?: previous.googleSafeBrowsing,
      cryptojacking = d.cryptojacking ?: previous.cryptojacking,
      dnsRebinding = d.dnsRebinding ?: previous.dnsRebinding,
      idnHomographs = d.idnHomographs ?: previous.idnHomographs,
      typosquatting = d.typosquatting ?: previous.typosquatting,
      dga = d.dga ?: previous.dga,
      nrd = d.nrd ?: previous.nrd,
      ddns = d.ddns ?: previous.ddns,
      parkedDomains = d.parking ?: previous.parkedDomains,
      csam = d.csam ?: previous.csam,
      blockedTlds = tldList
    )
    _securitySettings.value = updated
    preferences.saveSecuritySettings(profileId, updated)
    return true
  }

  private suspend fun applyPrivacySettingsFromApi(key: String, profileId: String): Boolean {
    var activeBlocklistDtos: List<BlocklistDto>? = null
    var activeNativeDtos: List<NativeTrackingDto>? = null
    var disguisedTrackersVal: Boolean? = null
    var allowAffiliatesVal: Boolean? = null

    val bResp = safeApiCall("getProfileBlocklists") { NextDnsNetworkClient.api.getProfileBlocklists(key, profileId) }
    if (bResp?.isSuccessful == true && bResp.body().isSemanticallySuccessful()) {
      activeBlocklistDtos = bResp.body()?.data
    }

    val nResp = safeApiCall("getProfileNatives") { NextDnsNetworkClient.api.getProfileNatives(key, profileId) }
    if (nResp?.isSuccessful == true && nResp.body().isSemanticallySuccessful()) {
      activeNativeDtos = nResp.body()?.data
    }

    val privResp = safeApiCall("getPrivacy") {
      NextDnsNetworkClient.api.getPrivacy(key, profileId)
    }
    val privBody = privResp?.body()
    if (privResp?.isSuccessful == true && privBody.isSemanticallySuccessful()) {
      val p = privBody?.data
      if (p != null) {
        if (activeBlocklistDtos == null) activeBlocklistDtos = p.blocklists
        if (activeNativeDtos == null) activeNativeDtos = p.natives
        disguisedTrackersVal = p.disguisedTrackers
        allowAffiliatesVal = p.allowAffiliateLinks
      }
    }

    val availResp = safeApiCall("getAvailableBlocklists") { NextDnsNetworkClient.api.getAvailableBlocklists(key) }
    var availableDtos = if (
      availResp?.isSuccessful == true &&
      availResp.body().isSemanticallySuccessful()
    ) {
      availResp.body()?.data
    } else {
      null
    }
    if (availableDtos.isNullOrEmpty()) {
      availableDtos = NextDnsNetworkClient.fetchAvailableBlocklistsDirect()
    }
    val safeAvailDtos = availableDtos ?: emptyList()

    val activeMap = activeBlocklistDtos?.associateBy { it.id.lowercase().trim() } ?: emptyMap()

    val updatedBlocklists = if (safeAvailDtos.isNotEmpty()) {
      val list = safeAvailDtos.map { dto ->
        val cleanId = dto.id.lowercase().trim()
        val isActive = activeMap.containsKey(cleanId)
        val isRecommended = cleanId == "nextdns-recommended"

        val displayName = when {
          !dto.name.isNullOrBlank() -> dto.name
          isRecommended -> "NextDNS Reklam & İzleyici Koruması"
          else -> dto.id
        }

        val displayDesc = when {
          !dto.description.isNullOrBlank() -> dto.description
          isRecommended -> "NextDNS tarafından optimize edilmiş dengeli ve kapsamlı reklam/izleyici engelleme listesi."
          else -> ""
        }

        val displayWebsite = when {
          !dto.website.isNullOrBlank() -> dto.website
          isRecommended -> "https://nextdns.io"
          else -> ""
        }

        val formattedUpdated = formatIsoDateWithRelative(dto.updatedOn)
        val category = determineBlocklistCategory(cleanId, displayName, displayDesc)

        BlocklistEntry(
          id = dto.id,
          name = displayName,
          description = displayDesc,
          entriesCount = dto.entries ?: 0L,
          active = isActive,
          website = displayWebsite,
          category = category,
          updatedTime = formattedUpdated
        )
      }.toMutableList()

      // Also append any active blocklist in the profile that is not in the public list
      val existingIds = list.map { it.id.lowercase().trim() }.toSet()
      activeBlocklistDtos?.forEach { activeDto ->
        val cleanId = activeDto.id.lowercase().trim()
        if (!existingIds.contains(cleanId)) {
          list.add(
            BlocklistEntry(
              id = activeDto.id,
              name = activeDto.name ?: activeDto.id,
              description = activeDto.description ?: "Özel filtre listesi.",
              entriesCount = activeDto.entries ?: 0L,
              active = true,
              website = activeDto.website ?: "",
              category = "Özel",
              updatedTime = formatIsoDateWithRelative(activeDto.updatedOn)
            )
          )
        }
      }
      list
    } else {
      val baseCatalog = _privacySettings.value.blocklists.ifEmpty { _availableBlocklistsCatalog.value }
      baseCatalog.map { catItem ->
        val cleanId = catItem.id.lowercase().trim()
        val remote = activeMap[cleanId]
        if (remote != null) {
          catItem.copy(
            id = remote.id,
            active = true,
            name = remote.name?.takeIf { it.isNotBlank() } ?: catItem.name,
            entriesCount = remote.entries ?: catItem.entriesCount,
            updatedTime = formatIsoDateWithRelative(remote.updatedOn).ifBlank { catItem.updatedTime }
          )
        } else {
          catItem.copy(active = false)
        }
      }
    }

    val baseNatives = _privacySettings.value.nativeTracking.ifEmpty {
      _availableNativesCatalog.value.map { NativeTrackingEntry(id = it.id, name = it.id.replaceFirstChar { c -> c.uppercase() }, active = false) }
    }
    val updatedNatives = if (activeNativeDtos != null) {
      val activeNatIds = activeNativeDtos.map { it.id.lowercase().trim() }.toSet()
      baseNatives.map { nat ->
        nat.copy(active = activeNatIds.contains(nat.id.lowercase().trim()))
      }
    } else {
      baseNatives
    }

    val updated = PrivacySettings(
      blocklists = updatedBlocklists,
      nativeTracking = updatedNatives,
      disguisedTrackers = disguisedTrackersVal ?: _privacySettings.value.disguisedTrackers,
      allowAffiliates = allowAffiliatesVal ?: _privacySettings.value.allowAffiliates
    )
    _privacySettings.value = updated
    preferences.savePrivacySettings(profileId, updated)
    return privResp?.isSuccessful == true &&
      privBody.isSemanticallySuccessful() &&
      privBody?.data != null
  }

  private suspend fun applyParentalSettingsFromApi(key: String, profileId: String): Boolean {
    val parentResp = safeApiCall("getParentalControl") {
      NextDnsNetworkClient.api.getParentalControl(key, profileId)
    } ?: return false
    val body = parentResp.body() ?: return false
    if (!parentResp.isSuccessful || body.hasApiErrors()) return false
    val pc = body.data ?: return false

    var servCatalog = _availableParentalServicesCatalog.value
    if (servCatalog.isEmpty()) {
      val fetched = NextDnsNetworkClient.fetchAvailableParentalServicesDirect()
      if (fetched != null && fetched.isNotEmpty()) {
        servCatalog = fetched
        _availableParentalServicesCatalog.value = fetched
      }
    }

    var catCatalog = _availableParentalCategoriesCatalog.value
    if (catCatalog.isEmpty()) {
      val fetched = NextDnsNetworkClient.fetchAvailableParentalCategoriesDirect()
      if (fetched != null && fetched.isNotEmpty()) {
        catCatalog = fetched
        _availableParentalCategoriesCatalog.value = fetched
      }
    }

    val activeServiceMap = pc.services?.associate { it.id.lowercase().trim() to (it.active != false) } ?: emptyMap()
    val activeCatMap = pc.categories?.associate { it.id.lowercase().trim() to (it.active != false) } ?: emptyMap()

    val updatedServices = if (servCatalog.isNotEmpty()) {
      servCatalog.map { s ->
        BlockedServiceEntry(
          id = s.id,
          name = s.id.replaceFirstChar { it.uppercase() },
          website = s.website,
          active = activeServiceMap[s.id.lowercase().trim()] == true
        )
      }
    } else {
      _parentalControlSettings.value.services.map { s ->
        val clean = s.id.lowercase().trim()
        if (activeServiceMap.containsKey(clean)) s.copy(active = activeServiceMap[clean] == true) else s
      }
    }

    val updatedCats = if (catCatalog.isNotEmpty()) {
      catCatalog.map { c ->
        BlockedCategoryEntry(
          id = c.id,
          name = c.id.replaceFirstChar { it.uppercase() },
          description = "",
          active = activeCatMap[c.id.lowercase().trim()] == true
        )
      }
    } else {
      _parentalControlSettings.value.categories.map { c ->
        val clean = c.id.lowercase().trim()
        if (activeCatMap.containsKey(clean)) c.copy(active = activeCatMap[clean] == true) else c
      }
    }

    val updated = ParentalControlSettings(
      services = updatedServices,
      categories = updatedCats,
      safeSearch = pc.safeSearch ?: _parentalControlSettings.value.safeSearch,
      youtubeRestrictedMode = pc.youtubeRestrictedMode ?: _parentalControlSettings.value.youtubeRestrictedMode,
      blockBypass = pc.blockBypass ?: _parentalControlSettings.value.blockBypass
    )
    _parentalControlSettings.value = updated
    preferences.saveParentalControlSettings(profileId, updated)
    return true
  }

  private suspend fun applyDenylistFromApi(key: String, profileId: String): Boolean {
    val denyResp = safeApiCall("getDenylist") {
      NextDnsNetworkClient.api.getDenylist(key, profileId)
    } ?: return false
    val body = denyResp.body() ?: return false
    if (!denyResp.isSuccessful || body.hasApiErrors()) return false
    val items = body.data ?: return false

    val list = items.map { AllowDenyItem(id = it.id, domain = it.id, active = it.active != false) }
    _denylist.value = list
    preferences.saveDenylist(profileId, list)
    return true
  }

  private suspend fun applyAllowlistFromApi(key: String, profileId: String): Boolean {
    val allowResp = safeApiCall("getAllowlist") {
      NextDnsNetworkClient.api.getAllowlist(key, profileId)
    } ?: return false
    val body = allowResp.body() ?: return false
    if (!allowResp.isSuccessful || body.hasApiErrors()) return false
    val items = body.data ?: return false

    val list = items.map { AllowDenyItem(id = it.id, domain = it.id, active = it.active != false) }
    _allowlist.value = list
    preferences.saveAllowlist(profileId, list)
    return true
  }

  private suspend fun applyConfigSettingsFromApi(key: String, profileId: String): Boolean {
    val cfgResp = safeApiCall("getSettings") {
      NextDnsNetworkClient.api.getSettings(key, profileId)
    } ?: return false
    val body = cfgResp.body() ?: return false
    if (!cfgResp.isSuccessful || body.hasApiErrors()) return false
    val s = body.data ?: return false

    val locName = when (s.logs?.location) {
      "ch" -> "İsviçre (CH)"
      "eu" -> "Avrupa Birliği (AB)"
      "us" -> "Amerika Birleşik Devletleri (ABD)"
      else -> _configSettings.value.logStorageLocation
    }
    val retName = LogRetentionCodec.toLabel(s.logs?.retention) ?: "Bilinmiyor"

    val updated = _configSettings.value.copy(
      logsEnabled = s.logs?.enabled ?: _configSettings.value.logsEnabled,
      logClientIps = if (s.logs?.drop?.ip != null) !s.logs.drop.ip else _configSettings.value.logClientIps,
      logDomains = if (s.logs?.drop?.domain != null) !s.logs.drop.domain else _configSettings.value.logDomains,
      logRetention = retName,
      logStorageLocation = locName,
      blockPage = s.blockPage?.enabled ?: _configSettings.value.blockPage,
      ednsClientSubnet = s.performance?.ecs ?: _configSettings.value.ednsClientSubnet,
      cacheBoost = s.performance?.cacheBoost ?: _configSettings.value.cacheBoost,
      cnameFlattening = s.performance?.cnameFlattening ?: _configSettings.value.cnameFlattening
    )
    _configSettings.value = updated
    preferences.saveConfigSettings(profileId, updated)
    return true
  }

  private suspend fun applyLogsFromApi(key: String, profileId: String) {
    val logsResp = safeApiCall("getLogs") {
      NextDnsNetworkClient.api.getLogs(key, profileId, limit = 100, raw = 1)
    } ?: return
    val body = logsResp.body() ?: return
    if (!logsResp.isSuccessful || body.hasApiErrors()) return

    logsStreamSeedId = body.meta?.stream?.id
    nextLogsCursor = body.meta?.pagination?.cursor

    val fetchedLogs = parseLogsResponse(body.data.orEmpty())
    _logs.value = fetchedLogs
    preferences.saveLogs(profileId, fetchedLogs)
  }

  private suspend fun applyDevicesAnalyticsFromApi(key: String, profileId: String) {
    val devResp = safeApiCall("getAnalyticsDevices") { NextDnsNetworkClient.api.getAnalyticsDevices(key, profileId) } ?: return
    val dtoList = devResp.body()?.data ?: return
    if (!devResp.isSuccessful) return

    val devItems = dtoList.map {
      val id = it.id ?: ""
      val name = it.name?.takeIf { n -> n.isNotBlank() } ?: it.id ?: "Bilinmeyen Cihaz"
      if (id.isNotBlank() && name.isNotBlank()) {
        _knownDeviceNameToId[name] = id
        _knownDeviceIdToName[id] = name
      }
      DeviceMetric(id = id, name = name, queries = it.queries ?: 0L)
    }
    if (devItems.isNotEmpty()) {
      updateKnownDevices(devItems.map { it.name })
      _analytics.value = _analytics.value.copy(topDevices = devItems)
    }
  }

  // =========================================================================
  // Logs Parsing
  // =========================================================================

  private fun parseLogsResponse(dtoList: List<DnsLogDto>): List<DnsLogEntry> {
    val parsedLogs = dtoList.map { l ->
      val devId = l.device?.id
      val devName = l.device?.name?.takeIf { it.isNotBlank() }
        ?: l.deviceName?.takeIf { it.isNotBlank() }
        ?: l.deviceNameSnake?.takeIf { it.isNotBlank() }
        ?: (if (!devId.isNullOrBlank()) _knownDeviceIdToName[devId] else null)
        ?: (if (!devId.isNullOrBlank()) devId else "Bilinmeyen Cihaz")

      if (!devId.isNullOrBlank() && !devName.isNullOrBlank() && devName != "Bilinmeyen Cihaz") {
        _knownDeviceNameToId[devName] = devId
        _knownDeviceIdToName[devId] = devName
      }

      val reasonObj = l.reasons?.firstOrNull()
      val blockReason = formatBlockReason(reasonObj?.id, reasonObj?.name)

      DnsLogEntry(
        id = UUID.randomUUID().toString(),
        timestamp = l.timestamp?.toString() ?: "",
        domain = l.domain?.takeIf { it.isNotBlank() } ?: l.root?.takeIf { it.isNotBlank() } ?: l.rootDomain?.takeIf { it.isNotBlank() } ?: "",
        clientIp = l.clientIp ?: l.clientIpSnake,
        deviceName = devName,
        blocked = l.status == "blocked",
        blockReason = if (l.status == "blocked") blockReason else null,
        protocol = l.protocol ?: "",
        responseTimeMs = l.responseTime ?: l.responseTimeSnake
      )
    }
    updateKnownDevices(parsedLogs.mapNotNull { it.deviceName })
    return parsedLogs
  }

  // =========================================================================
  // Profile Management
  // =========================================================================

  fun logout() {
    resetProfileScopedRuntimeState()
    preferences.clear()
    _apiKey.value = ""
    _activeProfileId.value = ""
    _profiles.value = emptyList()
    _denylist.value = emptyList()
    _allowlist.value = emptyList()
    _logs.value = emptyList()
    _accountInfo.value = NextDnsAccountInfo()
    _profileSetup.value = SetupDto()
    _securitySettings.value = SecuritySettings()
    _privacySettings.value = PrivacySettings()
    _parentalControlSettings.value = ParentalControlSettings()
    _configSettings.value = ConfigSettings()
    _apiStatus.value = ApiConnectionStatus.Disconnected
  }

  fun setActiveProfile(profileId: String) {
    resetProfileScopedRuntimeState()
    _activeProfileId.value = profileId
    preferences.activeProfileId = profileId
    loadLocalProfileData(profileId)
    val key = _apiKey.value
    if (key.isNotBlank()) {
      repoScope.launch {
        loadActiveProfileDataFromApi(key, profileId)
      }
    }
    repoScope.launch {
      runDiagnosticTest()
    }
  }

  suspend fun createProfileRemote(name: String): Result<NextDnsProfile> = withContext(Dispatchers.IO) {
    val key = _apiKey.value
    if (key.isBlank()) {
      return@withContext Result.failure(IllegalStateException("API anahtarı olmadan profil oluşturulamaz."))
    }

    val resp = safeApiCall("createProfile") {
      NextDnsNetworkClient.api.createProfile(key, NameRequest(name = name))
    } ?: return@withContext Result.failure(IllegalStateException("Profil oluşturma isteği tamamlanamadı."))

    val body = resp.body()
    if (!resp.isSuccessful || body.hasApiErrors()) {
      val detail = body?.errors?.firstOrNull()?.detail ?: "Profil NextDNS tarafından oluşturulamadı."
      return@withContext Result.failure(IllegalStateException(detail))
    }

    val newId = body?.data?.id?.takeIf { it.isNotBlank() }
      ?: return@withContext Result.failure(IllegalStateException("NextDNS profil kimliği döndürmedi."))

    val verified = safeApiCall("getCreatedProfile") {
      NextDnsNetworkClient.api.getProfile(key, newId)
    }
    val verifiedBody = verified?.body()
    val profileDto = if (verified?.isSuccessful == true && verifiedBody.isSemanticallySuccessful()) {
      verifiedBody?.data
    } else {
      null
    }

    val created = NextDnsProfile(
      id = newId,
      name = profileDto?.name?.takeIf { it.isNotBlank() } ?: name,
      fingerprint = profileDto?.fingerprint ?: ""
    )
    val updatedList = _profiles.value.filterNot { it.id == newId } + created
    _profiles.value = updatedList
    preferences.saveProfiles(updatedList)
    _activeProfileId.value = newId
    preferences.activeProfileId = newId
    loadLocalProfileData(newId)
    Result.success(created)
  }

  suspend fun deleteProfileRemote(profileId: String) = withContext(Dispatchers.IO) {
    val key = _apiKey.value
    if (key.isNotBlank()) {
      safeApiCall("deleteProfile") {
        NextDnsNetworkClient.api.deleteProfile(key, profileId)
      }
    }
    val updated = _profiles.value.filter { it.id != profileId }
    _profiles.value = updated
    preferences.saveProfiles(updated)
    if (_activeProfileId.value == profileId) {
      val next = updated.firstOrNull()?.id ?: ""
      _activeProfileId.value = next
      preferences.activeProfileId = next
      if (next.isNotBlank()) loadLocalProfileData(next)
    }
  }

  fun renameProfile(profileId: String, newName: String) {
    val updated = _profiles.value.map {
      if (it.id == profileId) it.copy(name = newName) else it
    }
    _profiles.value = updated
    preferences.saveProfiles(updated)
    val key = _apiKey.value
    if (key.isNotBlank()) {
      repoScope.launch {
        safeApiCall("renameProfile") {
          NextDnsNetworkClient.api.renameProfile(key, profileId, NameRequest(name = newName))
        }
      }
    }
  }

  // =========================================================================
  // Security Updates
  // =========================================================================

  fun updateSecurity(transform: (SecuritySettings) -> SecuritySettings) {
    val updated = transform(_securitySettings.value)
    _securitySettings.value = updated
    preferences.saveSecuritySettings(_activeProfileId.value, updated)
    pushSecurityToApi()
  }

  private fun pushSecurityToApi() {
    val key = _apiKey.value
    val pid = _activeProfileId.value
    if (key.isBlank() || pid.isBlank()) return

    repoScope.launch {
      safeApiCall("pushSecurityToApi") {
        val s = _securitySettings.value
        val req = SecurityUpdateRequest(
          threatIntelligenceFeeds = s.threatIntelligenceFeeds,
          aiThreatDetection = s.aiThreatDetection,
          googleSafeBrowsing = s.googleSafeBrowsing,
          cryptojacking = s.cryptojacking,
          dnsRebinding = s.dnsRebinding,
          idnHomographs = s.idnHomographs,
          typosquatting = s.typosquatting,
          dga = s.dga,
          nrd = s.nrd,
          ddns = s.ddns,
          parking = s.parkedDomains,
          csam = s.csam
        )
        NextDnsNetworkClient.api.updateSecurity(key, pid, req)
      }
    }
  }

  fun addBlockedTld(tld: String) {
    val updated = _securitySettings.value.copy(
      blockedTlds = (_securitySettings.value.blockedTlds + tld).distinct()
    )
    _securitySettings.value = updated
    preferences.saveSecuritySettings(_activeProfileId.value, updated)

    val key = _apiKey.value
    val pid = _activeProfileId.value
    if (key.isBlank() || pid.isBlank()) return

    repoScope.launch {
      safeApiCall("addBlockedTld") {
        NextDnsNetworkClient.api.addSecurityTld(key, pid, IdRequest(id = tld))
      }
    }
  }

  fun removeBlockedTld(tld: String) {
    val updated = _securitySettings.value.copy(
      blockedTlds = _securitySettings.value.blockedTlds.filter { it != tld }
    )
    _securitySettings.value = updated
    preferences.saveSecuritySettings(_activeProfileId.value, updated)

    val key = _apiKey.value
    val pid = _activeProfileId.value
    if (key.isBlank() || pid.isBlank()) return

    repoScope.launch {
      safeApiCall("removeBlockedTld") {
        NextDnsNetworkClient.api.removeSecurityTld(key, pid, tld)
      }
    }
  }

  // =========================================================================
  // Privacy Updates
  // =========================================================================

  fun updatePrivacy(transform: (PrivacySettings) -> PrivacySettings) {
    val updated = transform(_privacySettings.value)
    _privacySettings.value = updated
    preferences.savePrivacySettings(_activeProfileId.value, updated)
    pushPrivacyToApi()
  }

  private fun pushPrivacyToApi() {
    val key = _apiKey.value
    val pid = _activeProfileId.value
    if (key.isBlank() || pid.isBlank()) return

    repoScope.launch {
      safeApiCall("pushPrivacyToApi") {
        val p = _privacySettings.value
        val req = PrivacyUpdateRequest(
          disguisedTrackers = p.disguisedTrackers,
          allowAffiliateLinks = p.allowAffiliates
        )
        NextDnsNetworkClient.api.updatePrivacy(key, pid, req)
      }
    }
  }

  fun toggleBlocklist(blocklistId: String) {
    val updated = _privacySettings.value.blocklists.map {
      if (it.id == blocklistId) it.copy(active = !it.active) else it
    }
    val newSettings = _privacySettings.value.copy(blocklists = updated)
    _privacySettings.value = newSettings
    preferences.savePrivacySettings(_activeProfileId.value, newSettings)

    val key = _apiKey.value
    val pid = _activeProfileId.value
    val target = updated.find { it.id == blocklistId } ?: return
    if (key.isBlank() || pid.isBlank()) return

    repoScope.launch {
      safeApiCall("toggleBlocklist") {
        syncBlocklistRemote(key, pid, blocklistId, target.active)
      }
    }
  }

  fun toggleNativeTracking(nativeId: String) {
    val updated = _privacySettings.value.nativeTracking.map {
      if (it.id == nativeId) it.copy(active = !it.active) else it
    }
    val newSettings = _privacySettings.value.copy(nativeTracking = updated)
    _privacySettings.value = newSettings
    preferences.savePrivacySettings(_activeProfileId.value, newSettings)

    val key = _apiKey.value
    val pid = _activeProfileId.value
    val target = updated.find { it.id == nativeId } ?: return
    if (key.isBlank() || pid.isBlank()) return

    repoScope.launch {
      safeApiCall("toggleNativeTracking") {
        syncNativeTrackingRemote(key, pid, nativeId, target.active)
      }
    }
  }

  // =========================================================================
  // Parental Updates
  // =========================================================================

  fun updateParental(transform: (ParentalControlSettings) -> ParentalControlSettings) {
    val updated = transform(_parentalControlSettings.value)
    _parentalControlSettings.value = updated
    preferences.saveParentalControlSettings(_activeProfileId.value, updated)
    pushParentalToApi()
  }

  private fun pushParentalToApi() {
    val key = _apiKey.value
    val pid = _activeProfileId.value
    if (key.isBlank() || pid.isBlank()) return

    repoScope.launch {
      safeApiCall("pushParentalToApi") {
        val p = _parentalControlSettings.value
        val req = ParentalControlUpdateRequest(
          safeSearch = p.safeSearch,
          youtubeRestrictedMode = p.youtubeRestrictedMode,
          blockBypass = p.blockBypass
        )
        NextDnsNetworkClient.api.updateParentalControl(key, pid, req)
      }
    }
  }

  fun toggleParentalService(serviceId: String) {
    val updated = _parentalControlSettings.value.services.map {
      if (it.id == serviceId) it.copy(active = !it.active) else it
    }
    val newSettings = _parentalControlSettings.value.copy(services = updated)
    _parentalControlSettings.value = newSettings
    preferences.saveParentalControlSettings(_activeProfileId.value, newSettings)

    val key = _apiKey.value
    val pid = _activeProfileId.value
    val target = updated.find { it.id == serviceId } ?: return
    if (key.isBlank() || pid.isBlank()) return

    repoScope.launch {
      safeApiCall("toggleParentalService") {
        syncParentalServiceRemote(key, pid, serviceId, target.active)
      }
    }
  }

  fun toggleParentalCategory(categoryId: String) {
    val updated = _parentalControlSettings.value.categories.map {
      if (it.id == categoryId) it.copy(active = !it.active) else it
    }
    val newSettings = _parentalControlSettings.value.copy(categories = updated)
    _parentalControlSettings.value = newSettings
    preferences.saveParentalControlSettings(_activeProfileId.value, newSettings)

    val key = _apiKey.value
    val pid = _activeProfileId.value
    val target = updated.find { it.id == categoryId } ?: return
    if (key.isBlank() || pid.isBlank()) return

    repoScope.launch {
      safeApiCall("toggleParentalCategory") {
        syncParentalCategoryRemote(key, pid, categoryId, target.active)
      }
    }
  }

  // =========================================================================
  // Denylist & Allowlist
  // =========================================================================

  fun addToDenylist(domain: String) {
    addAllowDenyItem(
      flow = _denylist,
      saveLocal = { pid, list -> preferences.saveDenylist(pid, list) },
      domain = domain,
      callName = "addToDenylist"
    ) { key, pid ->
      NextDnsNetworkClient.api.addDenylist(key, pid, AllowDenyItemRequest(id = domain, active = true))
    }
  }

  fun removeFromDenylist(id: String) {
    removeAllowDenyItem(
      flow = _denylist,
      saveLocal = { pid, list -> preferences.saveDenylist(pid, list) },
      id = id,
      callName = "removeFromDenylist"
    ) { key, pid ->
      NextDnsNetworkClient.api.removeDenylist(key, pid, id)
    }
  }

  fun toggleDenylistItem(id: String) {
    toggleAllowDenyItemState(
      flow = _denylist,
      saveLocal = { pid, list -> preferences.saveDenylist(pid, list) },
      id = id,
      callName = "toggleDenylistItem"
    ) { key, pid, domain, active ->
      syncAllowDenyToggleRemote(
        key = key,
        pid = pid,
        domain = domain,
        active = active,
        toggleCall = { k, p, d, req -> NextDnsNetworkClient.api.toggleDenylist(k, p, d, req) },
        addCall = { k, p, req -> NextDnsNetworkClient.api.addDenylist(k, p, req) }
      )
    }
  }

  fun addToAllowlist(domain: String) {
    addAllowDenyItem(
      flow = _allowlist,
      saveLocal = { pid, list -> preferences.saveAllowlist(pid, list) },
      domain = domain,
      callName = "addToAllowlist"
    ) { key, pid ->
      NextDnsNetworkClient.api.addAllowlist(key, pid, AllowDenyItemRequest(id = domain, active = true))
    }
  }

  fun removeFromAllowlist(id: String) {
    removeAllowDenyItem(
      flow = _allowlist,
      saveLocal = { pid, list -> preferences.saveAllowlist(pid, list) },
      id = id,
      callName = "removeFromAllowlist"
    ) { key, pid ->
      NextDnsNetworkClient.api.removeAllowlist(key, pid, id)
    }
  }

  fun toggleAllowlistItem(id: String) {
    toggleAllowDenyItemState(
      flow = _allowlist,
      saveLocal = { pid, list -> preferences.saveAllowlist(pid, list) },
      id = id,
      callName = "toggleAllowlistItem"
    ) { key, pid, domain, active ->
      syncAllowDenyToggleRemote(
        key = key,
        pid = pid,
        domain = domain,
        active = active,
        toggleCall = { k, p, d, req -> NextDnsNetworkClient.api.toggleAllowlist(k, p, d, req) },
        addCall = { k, p, req -> NextDnsNetworkClient.api.addAllowlist(k, p, req) }
      )
    }
  }

  // =========================================================================
  // Generic List & Item Operations (De-duplicated)
  // =========================================================================

  private fun addAllowDenyItem(
    flow: MutableStateFlow<List<AllowDenyItem>>,
    saveLocal: (String, List<AllowDenyItem>) -> Unit,
    domain: String,
    callName: String,
    apiAction: suspend (key: String, pid: String) -> Unit
  ) {
    val current = flow.value.filter { it.domain != domain && it.id != domain }
    val updated = listOf(AllowDenyItem(id = domain, domain = domain, active = true)) + current
    flow.value = updated
    saveLocal(_activeProfileId.value, updated)

    val key = _apiKey.value
    val pid = _activeProfileId.value
    if (key.isBlank() || pid.isBlank()) return

    repoScope.launch {
      safeApiCall(callName) {
        apiAction(key, pid)
      }
    }
  }

  private fun removeAllowDenyItem(
    flow: MutableStateFlow<List<AllowDenyItem>>,
    saveLocal: (String, List<AllowDenyItem>) -> Unit,
    id: String,
    callName: String,
    apiAction: suspend (key: String, pid: String) -> Unit
  ) {
    val updated = flow.value.filter { it.id != id && it.domain != id }
    flow.value = updated
    saveLocal(_activeProfileId.value, updated)

    val key = _apiKey.value
    val pid = _activeProfileId.value
    if (key.isBlank() || pid.isBlank()) return

    repoScope.launch {
      safeApiCall(callName) {
        apiAction(key, pid)
      }
    }
  }

  private fun toggleAllowDenyItemState(
    flow: MutableStateFlow<List<AllowDenyItem>>,
    saveLocal: (String, List<AllowDenyItem>) -> Unit,
    id: String,
    callName: String,
    apiToggleAction: suspend (key: String, pid: String, domain: String, active: Boolean) -> Unit
  ) {
    val targetItem = flow.value.find { it.id == id || it.domain == id }
    val newActive = !(targetItem?.active ?: true)
    val updated = flow.value.map {
      if (it.id == id || it.domain == id) it.copy(active = newActive) else it
    }
    flow.value = updated
    saveLocal(_activeProfileId.value, updated)

    val key = _apiKey.value
    val pid = _activeProfileId.value
    val domain = targetItem?.domain ?: id
    if (key.isBlank() || pid.isBlank()) return

    repoScope.launch {
      safeApiCall(callName) {
        apiToggleAction(key, pid, domain, newActive)
      }
    }
  }

  private suspend fun syncBlocklistRemote(key: String, pid: String, blocklistId: String, active: Boolean) {
    if (active) {
      NextDnsNetworkClient.api.addBlocklist(key, pid, IdRequest(id = blocklistId))
    } else {
      NextDnsNetworkClient.api.removeBlocklist(key, pid, blocklistId)
    }
  }

  private suspend fun syncNativeTrackingRemote(key: String, pid: String, nativeId: String, active: Boolean) {
    if (active) {
      NextDnsNetworkClient.api.addNativeTracking(key, pid, IdRequest(id = nativeId))
    } else {
      NextDnsNetworkClient.api.removeNativeTracking(key, pid, nativeId)
    }
  }

  private suspend fun syncParentalServiceRemote(key: String, pid: String, serviceId: String, active: Boolean) {
    val patchResp = NextDnsNetworkClient.api.updateParentalService(key, pid, serviceId, ParentalActiveRequest(active = active))
    if (!patchResp.isSuccessful) {
      if (active) {
        NextDnsNetworkClient.api.addParentalService(key, pid, ParentalItemRequest(id = serviceId, active = true))
      } else {
        NextDnsNetworkClient.api.removeParentalService(key, pid, serviceId)
      }
    }
  }

  private suspend fun syncParentalCategoryRemote(key: String, pid: String, categoryId: String, active: Boolean) {
    val patchResp = NextDnsNetworkClient.api.updateParentalCategory(key, pid, categoryId, ParentalActiveRequest(active = active))
    if (!patchResp.isSuccessful) {
      if (active) {
        NextDnsNetworkClient.api.addParentalCategory(key, pid, ParentalItemRequest(id = categoryId, active = true))
      } else {
        NextDnsNetworkClient.api.removeParentalCategory(key, pid, categoryId)
      }
    }
  }

  private suspend fun syncAllowDenyToggleRemote(
    key: String,
    pid: String,
    domain: String,
    active: Boolean,
    toggleCall: suspend (String, String, String, AllowDenyActiveRequest) -> Response<*>,
    addCall: suspend (String, String, AllowDenyItemRequest) -> Response<*>
  ) {
    val patchResp = toggleCall(key, pid, domain, AllowDenyActiveRequest(active = active))
    if (!patchResp.isSuccessful) {
      addCall(key, pid, AllowDenyItemRequest(id = domain, active = active))
    }
  }

  // =========================================================================
  // Config & Logs
  // =========================================================================

  fun updateConfig(transform: (ConfigSettings) -> ConfigSettings) {
    val updated = transform(_configSettings.value)
    _configSettings.value = updated
    preferences.saveConfigSettings(_activeProfileId.value, updated)
    pushConfigToApi()
  }

  private fun pushConfigToApi() {
    val key = _apiKey.value
    val pid = _activeProfileId.value
    if (key.isBlank() || pid.isBlank()) return

    repoScope.launch {
      safeApiCall("pushConfigToApi") {
        val cfg = _configSettings.value
        val retSeconds = LogRetentionCodec.toSeconds(cfg.logRetention)
        val locCode = when (cfg.logStorageLocation) {
          "İsviçre (CH)" -> "ch"
          "Avrupa Birliği (AB)" -> "eu"
          else -> "us"
        }

        val logsDto = SettingsLogsDto(
          enabled = cfg.logsEnabled,
          retention = retSeconds,
          location = locCode,
          drop = SettingsLogsDropDto(ip = !cfg.logClientIps, domain = !cfg.logDomains)
        )
        val blockPageDto = SettingsBlockPageDto(enabled = cfg.blockPage)
        val perfDto = SettingsPerformanceDto(
          ecs = cfg.ednsClientSubnet,
          cacheBoost = cfg.cacheBoost,
          cnameFlattening = cfg.cnameFlattening
        )

        val updateReq = SettingsUpdateRequest(
          logs = logsDto,
          blockPage = blockPageDto,
          performance = perfDto,
          web3 = cfg.web3
        )

        val resp = NextDnsNetworkClient.api.updateSettings(key, pid, updateReq)
        if (!resp.isSuccessful) {
          NextDnsNetworkClient.api.updateSettingsPerformance(
            key, pid, SettingsPerformanceUpdateRequest(
              ecs = cfg.ednsClientSubnet,
              cacheBoost = cfg.cacheBoost,
              cnameFlattening = cfg.cnameFlattening
            )
          )
          NextDnsNetworkClient.api.updateSettingsLogs(
            key, pid, SettingsLogsUpdateRequest(
              enabled = cfg.logsEnabled,
              retention = retSeconds,
              location = locCode,
              drop = SettingsLogsDropDto(ip = !cfg.logClientIps, domain = !cfg.logDomains)
            )
          )
          NextDnsNetworkClient.api.updateSettingsBlockPage(
            key, pid, SettingsBlockPageUpdateRequest(enabled = cfg.blockPage)
          )
        }
      }
    }
  }

  fun clearLogs() {
    _logs.value = emptyList()
    preferences.saveLogs(_activeProfileId.value, emptyList())
    val key = _apiKey.value
    val pid = _activeProfileId.value
    if (key.isBlank() || pid.isBlank()) return

    repoScope.launch {
      safeApiCall("clearLogs") {
        NextDnsNetworkClient.api.clearLogs(key, pid)
      }
    }
  }

  suspend fun refreshLogsFromApi(limit: Int = 100) {
    val key = _apiKey.value
    val pid = _activeProfileId.value
    if (key.isBlank() || pid.isBlank()) return

    val logsResp = safeApiCall("refreshLogsFromApi") {
      NextDnsNetworkClient.api.getLogs(key, pid, limit = limit, raw = 1)
    } ?: return

    val body = logsResp.body() ?: return
    if (!logsResp.isSuccessful || body.hasApiErrors()) return

    logsStreamSeedId = body.meta?.stream?.id ?: logsStreamSeedId
    nextLogsCursor = body.meta?.pagination?.cursor

    val fetchedLogs = parseLogsResponse(body.data.orEmpty())
    if (fetchedLogs.isNotEmpty()) {
      val existing = _logs.value
      val merged = (fetchedLogs + existing).distinctBy {
        "${it.timestamp}_${it.domain}_${it.deviceName}_${it.blocked}"
      }.take(500)
      _logs.value = merged
      preferences.saveLogs(pid, merged)
    }
  }

  fun setLiveStreaming(enabled: Boolean) {
    _isLiveStreaming.value = enabled
    if (enabled) {
      startLogsStream()
    } else {
      stopLogsStream()
    }
  }

  fun startAnalyticsPolling() {
    analyticsPollingJob?.cancel()
    analyticsPollingJob = repoScope.launch {
      while (isActive) {
        fetchAnalytics(_apiKey.value, _activeProfileId.value, currentAnalyticsDevice, currentAnalyticsTime)
        delay(30000)
      }
    }
  }

  fun stopAnalyticsPolling() {
    analyticsPollingJob?.cancel()
    analyticsPollingJob = null
  }

  fun startLogsStream() {
    val key = _apiKey.value
    val pid = _activeProfileId.value
    if (key.isBlank() || pid.isBlank()) return

    stopLogsStream()
    _isLiveStreaming.value = true

    streamJob = repoScope.launch(Dispatchers.IO) {
      var lastId: String? = logsStreamSeedId

      if (lastId == null) {
        val seedResp = safeApiCall("seedLogsStream") {
          NextDnsNetworkClient.api.getLogs(key, pid, limit = 100, raw = 1)
        }
        val seedBody = seedResp?.body()
        if (seedResp?.isSuccessful == true && seedBody.isSemanticallySuccessful()) {
          val seededLogs = parseLogsResponse(seedBody?.data.orEmpty())
          _logs.value = seededLogs
          preferences.saveLogs(pid, seededLogs)
          logsStreamSeedId = seedBody?.meta?.stream?.id
          nextLogsCursor = seedBody?.meta?.pagination?.cursor
          lastId = logsStreamSeedId
        }
      }

      var reconnectDelayMs = 2_000L
      while (isActive) {
        try {
          val req = buildLogsStreamRequest(pid, key, lastId)
          NextDnsNetworkClient.client.newCall(req).execute().use { response ->
            if (!response.isSuccessful) {
              throw java.io.IOException("Logs stream HTTP ${response.code}")
            }
            val body = response.body
              ?: throw java.io.IOException("Logs stream body is empty")

            reconnectDelayMs = 2_000L
            consumeSseStream(body) { newId ->
              lastId = newId
              logsStreamSeedId = newId
            }
          }

          if (isActive) delay(500L)
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.e(TAG, "Stream error: ${e.message}")
          delay(reconnectDelayMs)
          reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(60_000L)
        }
      }
    }
  }

  private fun buildLogsStreamRequest(pid: String, key: String, lastId: String?): Request {
    var url = "https://api.nextdns.io/profiles/$pid/logs/stream"
    if (lastId != null) {
      url += "?id=$lastId"
    }
    return Request.Builder()
      .url(url)
      .header("X-Api-Key", key)
      .header("Accept", "text/event-stream")
      .build()
  }

  private suspend fun consumeSseStream(responseBody: okhttp3.ResponseBody, onNewId: (String) -> Unit) {
    val source = responseBody.source()
    while (currentCoroutineContext().isActive && !source.exhausted()) {
      val line = source.readUtf8Line() ?: break
      processSseLine(line, onNewId)
    }
  }

  private suspend fun processSseLine(line: String, onNewId: (String) -> Unit) {
    if (line.startsWith("id: ")) {
      onNewId(line.substring(4))
      return
    }
    if (!line.startsWith("data: ")) return

    val jsonStr = line.substring(6)
    val entry = parseSseLogEntry(jsonStr) ?: return
    emitLogEntry(entry)
  }

  private suspend fun parseSseLogEntry(jsonStr: String): DnsLogEntry? {
    if (jsonStr.isBlank()) return null
    val logDto = safeApiCall("sseJsonParse") {
      NextDnsNetworkClient.moshi.adapter(DnsLogDto::class.java).fromJson(jsonStr)
    } ?: return null

    val devId = logDto.device?.id
    val devName = logDto.device?.name?.takeIf { it.isNotBlank() }
      ?: logDto.deviceName?.takeIf { it.isNotBlank() }
      ?: logDto.deviceNameSnake?.takeIf { it.isNotBlank() }
      ?: (if (!devId.isNullOrBlank()) _knownDeviceIdToName[devId] else null)
      ?: (if (!devId.isNullOrBlank()) devId else "Bilinmeyen Cihaz")

    if (!devId.isNullOrBlank() && !devName.isNullOrBlank() && devName != "Bilinmeyen Cihaz") {
      _knownDeviceNameToId[devName] = devId
      _knownDeviceIdToName[devId] = devName
      updateKnownDevices(listOf(devName))
    }

    val reasonObj = logDto.reasons?.firstOrNull()
    val blockReason = formatBlockReason(reasonObj?.id, reasonObj?.name)

    return DnsLogEntry(
      id = UUID.randomUUID().toString(),
      timestamp = logDto.timestamp?.toString() ?: "",
      domain = logDto.domain?.takeIf { it.isNotBlank() } ?: logDto.root?.takeIf { it.isNotBlank() } ?: logDto.rootDomain?.takeIf { it.isNotBlank() } ?: "unknown.com",
      clientIp = logDto.clientIp ?: logDto.clientIpSnake,
      deviceName = devName,
      blocked = logDto.status == "blocked",
      blockReason = if (logDto.status == "blocked") blockReason else null,
      protocol = logDto.protocol ?: "DoH",
      responseTimeMs = logDto.responseTime ?: logDto.responseTimeSnake
    )
  }

  private suspend fun emitLogEntry(entry: DnsLogEntry) {
    withContext(Dispatchers.Main) {
      val current = _logs.value
      val isDuplicate = current.take(15).any {
        it.timestamp == entry.timestamp && it.domain == entry.domain && it.deviceName == entry.deviceName
      }
      if (!isDuplicate) {
        _logs.value = (listOf(entry) + current).take(500)
      }
    }
  }

  fun stopLogsStream() {
    streamJob?.cancel()
    streamJob = null
    _isLiveStreaming.value = false
  }

  // =========================================================================
  // Analytics Parsing Helper Functions
  // =========================================================================

  private fun parseStatusMetrics(
    response: Response<NextDnsApiResponse<List<AnalyticsStatusItem>>>?,
    fallbackTotal: Long = 0L,
    fallbackBlocked: Long = 0L
  ): Pair<Long, Long> {
    if (response == null || !response.isSuccessful) return Pair(fallbackTotal, fallbackBlocked)
    val statuses = response.body()?.data ?: emptyList()
    val blockedQueries = statuses.find { it.status == "blocked" }?.queries ?: 0L
    val allQueries = statuses.sumOf { it.queries ?: 0L }
    return Pair(allQueries, blockedQueries)
  }

  private fun parseTopDevices(
    response: Response<NextDnsApiResponse<List<AnalyticsDeviceItem>>>?
  ): List<DeviceMetric> {
    if (response == null || !response.isSuccessful) return _analytics.value.topDevices
    val devices = response.body()?.data?.map {
      val id = it.id ?: ""
      val name = it.name?.takeIf { n -> n.isNotBlank() } ?: it.id ?: "Bilinmeyen Cihaz"
      if (id.isNotBlank() && name.isNotBlank()) {
        _knownDeviceNameToId[name] = id
        _knownDeviceIdToName[id] = name
      }
      DeviceMetric(id = id, name = name, queries = it.queries ?: 0L)
    } ?: _analytics.value.topDevices
    updateKnownDevices(devices.map { it.name })
    return devices
  }

  private fun parseTopDomains(
    response: Response<NextDnsApiResponse<List<AnalyticsDomainItem>>>?,
    fallback: List<DomainMetric> = emptyList()
  ): List<DomainMetric> {
    if (response == null || !response.isSuccessful) return fallback
    return response.body()?.data?.mapNotNull {
      val dom = it.domain ?: it.root
      if (dom.isNullOrBlank()) null
      else DomainMetric(domain = dom, queries = it.queries ?: 0L)
    } ?: fallback
  }

  private fun parseBlockedReasons(
    response: Response<NextDnsApiResponse<List<AnalyticsReasonItem>>>?
  ): Map<String, Long> {
    if (response == null || !response.isSuccessful) return _analytics.value.topBlockedReasons
    val rMap = mutableMapOf<String, Long>()
    response.body()?.data?.forEach { item ->
      val formatted = formatBlockReason(item.id, item.name)
      val cur = rMap.getOrDefault(formatted, 0L)
      rMap[formatted] = cur + (item.queries ?: 0L)
    }
    return rMap.toList().sortedByDescending { it.second }.toMap()
  }

  private fun parseGafamMetrics(
    response: Response<NextDnsApiResponse<List<AnalyticsItemDto>>>?,
    totalQueries: Long
  ): Map<String, Pair<Double, Long>> {
    val gafamMetrics = mutableMapOf<String, Pair<Double, Long>>()
    if (response != null && response.isSuccessful) {
      val data = response.body()?.data ?: emptyList()
      data.forEach { item ->
        val count = item.queries ?: 0L
        val pct = if (totalQueries > 0) ((count.toDouble() / totalQueries) * 100.0).coerceIn(0.0, 100.0) else 0.0
        val companyName = item.company ?: item.name ?: item.id ?: "Diğer"
        gafamMetrics[companyName] = Pair(pct, count)
      }
    }
    return gafamMetrics
  }

  private fun parseCountryMetrics(
    response: Response<NextDnsApiResponse<List<AnalyticsItemDto>>>?,
    totalQueries: Long
  ): List<Pair<String, Double>> {
    val topCountries = mutableListOf<Pair<String, Double>>()
    if (response != null && response.isSuccessful) {
      response.body()?.data?.forEach { item ->
        val count = item.queries ?: 0L
        val pct = if (totalQueries > 0) ((count.toDouble() / totalQueries) * 100.0).coerceIn(0.0, 100.0) else 0.0
        val code = item.code ?: ""
        val name = if (code.length == 2) {
          Locale("", code).getDisplayName(Locale("tr"))
        } else {
          item.name ?: "Bilinmeyen"
        }
        topCountries.add(Pair(name, pct))
      }
    }
    return topCountries
  }

  private fun parseDnssecPercentage(
    response: Response<NextDnsApiResponse<List<AnalyticsItemDto>>>?,
    totalQueries: Long
  ): Double {
    if (response == null || !response.isSuccessful) return 0.0
    val items = response.body()?.data ?: emptyList()
    val validated = items.find { it.validated == true || it.id == "validated" || it.id == "true" }?.queries ?: 0L
    return if (totalQueries > 0) ((validated.toDouble() / totalQueries) * 100.0).coerceIn(0.0, 100.0) else 0.0
  }

  private fun parseEncryptionPercentage(
    response: Response<NextDnsApiResponse<List<AnalyticsItemDto>>>?,
    totalQueries: Long
  ): Double {
    if (response == null || !response.isSuccessful) return 0.0
    val items = response.body()?.data ?: emptyList()
    val encrypted = items.find { it.encrypted == true || it.id == "encrypted" || it.id == "true" }?.queries ?: 0L
    return if (totalQueries > 0) ((encrypted.toDouble() / totalQueries) * 100.0).coerceIn(0.0, 100.0) else 0.0
  }

  // =========================================================================
  // Main Analytics Fetching Function
  // =========================================================================

  suspend fun fetchAnalytics(key: String, profileId: String, device: String? = null, from: String? = null) {
    if (key.isBlank() || profileId.isBlank()) return

    currentAnalyticsDevice = device
    currentAnalyticsTime = from

    val devParam = if (device == "Tüm cihazlar" || device.isNullOrBlank()) {
      null
    } else {
      val trimmed = device.trim()
      _knownDeviceNameToId[trimmed]
        ?: _knownDeviceNameToId.entries.firstOrNull { it.key.equals(trimmed, ignoreCase = true) }?.value
        ?: _analytics.value.topDevices.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }?.id?.takeIf { it.isNotBlank() }
        ?: if (trimmed.equals("Bilinmeyen Cihaz", ignoreCase = true) || trimmed.equals("Bilinmeyen", ignoreCase = true) || trimmed == "__UNIDENTIFIED__") {
             "__UNIDENTIFIED__"
           } else {
             trimmed
           }
    }

    val fromParam = when {
      from.isNullOrBlank() -> "-30d"
      from.equals("Son 1 Saat", ignoreCase = true) || from.equals("1h", ignoreCase = true) || from.equals("-1h", ignoreCase = true) -> "-1h"
      from.equals("Son 24 Saat", ignoreCase = true) || from.equals("Son 24 saat", ignoreCase = true) || from.equals("24h", ignoreCase = true) || from.equals("-24h", ignoreCase = true) -> "-24h"
      from.equals("Son 7 Gün", ignoreCase = true) || from.equals("Son 7 gün", ignoreCase = true) || from.equals("7d", ignoreCase = true) || from.equals("-7d", ignoreCase = true) -> "-7d"
      from.equals("Son 30 Gün", ignoreCase = true) || from.equals("Son 30 gün", ignoreCase = true) || from.equals("30d", ignoreCase = true) || from.equals("-30d", ignoreCase = true) -> "-30d"
      from.equals("Son 3 Ay", ignoreCase = true) || from.equals("Son 3 ay", ignoreCase = true) || from.equals("Son 90 Gün", ignoreCase = true) || from.equals("Son 90 gün", ignoreCase = true) || from.equals("90d", ignoreCase = true) || from.equals("-90d", ignoreCase = true) -> "-90d"
      else -> "-30d"
    }

    val statusResp = safeApiCall("getAnalyticsStatus") { NextDnsNetworkClient.api.getAnalyticsStatus(key, profileId, devParam, fromParam) }
    val devicesResp = safeApiCall("getAnalyticsDevices") { NextDnsNetworkClient.api.getAnalyticsDevices(key, profileId, null, fromParam) }
    val allowedDomainsResp = safeApiCall("getAllowedDomains") { NextDnsNetworkClient.api.getAnalyticsDomains(key, profileId, devParam, fromParam, status = "default") }
    val blockedDomainsResp = safeApiCall("getBlockedDomains") { NextDnsNetworkClient.api.getAnalyticsDomains(key, profileId, devParam, fromParam, status = "blocked") }
    val rootDomainsResp = safeApiCall("getRootDomains") { NextDnsNetworkClient.api.getAnalyticsDomains(key, profileId, devParam, fromParam) }
    val reasonsResp = safeApiCall("getReasons") { NextDnsNetworkClient.api.getAnalyticsReasons(key, profileId, devParam, fromParam) }
    val companiesResp = safeApiCall("getCompanies") { NextDnsNetworkClient.api.getAnalyticsDestinations(key, profileId, devParam, fromParam, type = "gafam") }
    val destinationsResp = safeApiCall("getDestinations") { NextDnsNetworkClient.api.getAnalyticsDestinations(key, profileId, devParam, fromParam, type = "countries") }
    val dnssecResp = safeApiCall("getDnssec") { NextDnsNetworkClient.api.getAnalyticsDnssec(key, profileId, devParam, fromParam) }
    val encryptionResp = safeApiCall("getEncryption") { NextDnsNetworkClient.api.getAnalyticsEncryption(key, profileId, devParam, fromParam) }

    val (totalQueries, blockedQueries) = parseStatusMetrics(statusResp)
    val topDevices = parseTopDevices(devicesResp)
    val topAllowedDomains = parseTopDomains(allowedDomainsResp, emptyList())
    val topBlockedDomains = parseTopDomains(blockedDomainsResp, emptyList())
    val topDomains = parseTopDomains(rootDomainsResp, emptyList())
    val topBlockedReasons = parseBlockedReasons(reasonsResp)
    val gafamMetrics = parseGafamMetrics(companiesResp, totalQueries)
    val topCountries = parseCountryMetrics(destinationsResp, totalQueries)
    val dnssecPct = parseDnssecPercentage(dnssecResp, totalQueries)
    val encPct = parseEncryptionPercentage(encryptionResp, totalQueries)

    val blockRate = if (totalQueries > 0) (blockedQueries.toDouble() / totalQueries) * 100 else 0.0

    val updatedAnalytics = AnalyticsSummary(
      totalQueries = totalQueries,
      blockedQueries = blockedQueries,
      blockRate = blockRate,
      blockedPercentage = blockRate.toFloat(),
      topAllowedDomains = topAllowedDomains,
      topBlockedDomains = topBlockedDomains,
      topBlockedReasons = topBlockedReasons,
      topDevices = topDevices,
      topDomains = topDomains,
      gafamMetrics = gafamMetrics,
      encryptedDnsPercentage = encPct.toFloat(),
      dnssecPercentage = dnssecPct.toFloat(),
      topCountries = topCountries
    )

    _analytics.value = updatedAnalytics
  }

  fun formatBlockReason(rawId: String?, rawName: String?): String {
    val id = rawId?.trim()?.lowercase(Locale.ROOT) ?: ""
    val name = rawName?.trim() ?: ""

    // 1. Name based translations / cleanups
    if (name.isNotBlank()) {
      when {
        name.equals("NextDNS Ads & Trackers Blocklist", ignoreCase = true) -> return "NextDNS Reklam ve İzleyici Engelleme Listesi"
        name.equals("Disguised Third-Party Trackers", ignoreCase = true) || name.equals("Disguised Trackers", ignoreCase = true) -> return "Gizlenmiş Üçüncü Taraf İzleyiciler"
        name.equals("Block Bypass Methods", ignoreCase = true) || name.equals("Bypass Methods", ignoreCase = true) -> return "Atlatma Yöntemleri"
        name.equals("Denylist", ignoreCase = true) || name.equals("Blacklist", ignoreCase = true) -> return "Kara Liste"
        name.equals("Allowlist", ignoreCase = true) || name.equals("Whitelist", ignoreCase = true) -> return "Beyaz Liste"
        name.equals("Threat Intelligence Feeds", ignoreCase = true) -> return "Tehdit İstihbarat Kaynakları"
        name.equals("AI Threat Detection", ignoreCase = true) -> return "Yapay Zeka Tehdit Algılama"
        name.equals("Google Safe Browsing", ignoreCase = true) -> return "Google Güvenli Tarama"
        name.equals("Cryptojacking Protection", ignoreCase = true) || name.equals("Cryptojacking", ignoreCase = true) -> return "Kripto Madenciliği Koruması"
        name.equals("DNS Rebinding Protection", ignoreCase = true) || name.equals("DNS Rebinding", ignoreCase = true) -> return "DNS Yeniden Bağlama Koruması"
        name.equals("IDN Homograph Attacks Protection", ignoreCase = true) -> return "IDN Eşsesli Saldırı Koruması"
        name.equals("Typosquatting Protection", ignoreCase = true) -> return "Yazım Hatası Alan Adı Koruması"
        name.equals("Newly Registered Domains (NRD)", ignoreCase = true) || name.equals("Newly Registered Domains", ignoreCase = true) -> return "Yeni Kaydedilen Alan Adları (NRD)"
        name.equals("Dynamic DNS (DDNS)", ignoreCase = true) || name.equals("Dynamic DNS Hostnames", ignoreCase = true) -> return "Dinamik DNS Alan Adları (DDNS)"
        name.equals("Parked Domains", ignoreCase = true) -> return "Park Edilmiş Alan Adları"
        name.equals("Child Sexual Abuse Material (CSAM)", ignoreCase = true) -> return "Çocuk Cinsel İstismarı Materyalleri (CSAM)"
        name.equals("SafeSearch", ignoreCase = true) -> return "Güvenli Arama"
        name.equals("YouTube Restricted Mode", ignoreCase = true) -> return "YouTube Kısıtlı Modu"
        name.startsWith("Native Tracking (", ignoreCase = true) -> {
          val brand = name.substringAfter("(").substringBefore(")")
          return "Yerel İzleme ($brand)"
        }
        !name.startsWith("blocklist:", ignoreCase = true) && !name.startsWith("native:", ignoreCase = true) -> return name
      }
    }

    // 2. ID based lookups
    val cleanId = id.removePrefix("blocklist:").removePrefix("parentalcontrol:").trim()

    return when {
      cleanId == "denylist" || cleanId == "blacklist" -> "Kara Liste"
      cleanId == "allowlist" || cleanId == "whitelist" -> "Beyaz Liste"
      cleanId == "block-bypass" || cleanId == "bypass" -> "Atlatma Yöntemleri"
      cleanId == "disguised-trackers" || cleanId == "cname-flattening" -> "Gizlenmiş Üçüncü Taraf İzleyiciler"
      cleanId == "nextdns-recommended" -> "NextDNS Reklam ve İzleyici Engelleme Listesi"
      cleanId == "adguard-dns-filter" || cleanId == "adguard-mobile-filter" -> "AdGuard DNS filter"
      cleanId == "oisd" || cleanId == "oisd-full" || cleanId == "oisd-basic" -> "oisd"
      cleanId == "easylist" -> "EasyList"
      cleanId == "easyprivacy" -> "EasyPrivacy"
      cleanId == "stevenblack" -> "Steven Black"
      cleanId == "fanboy-annoyance" -> "Fanboy's Annoyance"
      cleanId == "notracking" -> "NoTracking"
      cleanId == "1hosts-pro" -> "1Hosts (Pro)"
      cleanId == "1hosts-lite" -> "1Hosts (Lite)"
      cleanId == "1hosts-mini" -> "1Hosts (Mini)"
      cleanId == "doh-dot-vpn-tor" -> "DoH/DoT/VPN/TOR"
      cleanId.startsWith("native:") || cleanId.startsWith("native-") -> {
        val brand = cleanId.removePrefix("native:").removePrefix("native-").trim()
        val formattedBrand = when (brand.lowercase(Locale.ROOT)) {
          "apple" -> "Apple"
          "xiaomi" -> "Xiaomi"
          "samsung" -> "Samsung"
          "huawei" -> "Huawei"
          "windows" -> "Windows"
          "roku" -> "Roku"
          "sonos" -> "Sonos"
          "alexa" -> "Amazon Alexa"
          "lg" -> "LG"
          else -> brand.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
        "Yerel İzleme ($formattedBrand)"
      }
      cleanId == "threat-intelligence-feeds" || cleanId == "threat-intelligence" -> "Tehdit İstihbarat Kaynakları"
      cleanId == "ai-threat-detection" || cleanId == "ai-threat" -> "Yapay Zeka Tehdit Algılama"
      cleanId == "google-safe-browsing" || cleanId == "safebrowsing" -> "Google Güvenli Tarama"
      cleanId == "cryptojacking" -> "Kripto Madenciliği Koruması"
      cleanId == "dns-rebinding" -> "DNS Yeniden Bağlama Koruması"
      cleanId == "idn-homographs" || cleanId == "homographs" -> "IDN Eşsesli Saldırı Koruması"
      cleanId == "typosquatting" -> "Yazım Hatası Alan Adı Koruması"
      cleanId == "dga" -> "DGA Koruması"
      cleanId == "nrd" -> "Yeni Kaydedilen Alan Adları (NRD)"
      cleanId == "ddns" -> "Dinamik DNS Alan Adları (DDNS)"
      cleanId == "parking" || cleanId == "parked-domains" -> "Park Edilmiş Alan Adları"
      cleanId == "csam" -> "Çocuk Cinsel İstismarı Materyalleri (CSAM)"
      cleanId == "safesearch" -> "Güvenli Arama"
      cleanId == "youtube-restricted-mode" || cleanId == "youtube-restricted" -> "YouTube Kısıtlı Modu"
      cleanId == "block-page" -> "Engelleme Sayfası"
      cleanId == "tlds" || cleanId == "blocked-tlds" -> "Engellenen Üst Seviye Alan Adları (TLD)"
      name.isNotBlank() -> name
      id.isNotBlank() -> id.replace("-", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
      else -> "Diğer"
    }
  }

  fun formatIsoDateWithRelative(rawTime: String?): String {
    if (rawTime.isNullOrBlank()) return ""
    val trimmed = rawTime.trim()

    try {
      val epochMillis: Long? = when {
        trimmed.toLongOrNull() != null -> {
          val num = trimmed.toLong()
          if (num < 10000000000L) num * 1000 else num
        }
        else -> {
          try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
              java.time.Instant.parse(trimmed).toEpochMilli()
            } else {
              val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.ROOT)
              sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
              sdf.parse(trimmed)?.time
            }
          } catch (_: Exception) {
            try {
              val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.ROOT)
              sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
              sdf.parse(trimmed)?.time
            } catch (_: Exception) {
              null
            }
          }
        }
      }

      if (epochMillis != null && epochMillis > 0) {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
          timeInMillis = epochMillis
        }
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val monthNames = arrayOf("Oca", "Şub", "Mar", "Nis", "May", "Haz", "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara")
        val monthStr = monthNames.getOrElse(cal.get(java.util.Calendar.MONTH)) { "" }
        val year = cal.get(java.util.Calendar.YEAR)
        val formattedDate = "$day $monthStr $year"

        val now = System.currentTimeMillis()
        val diffMs = now - epochMillis

        val relative = if (diffMs <= 0) {
          "Bugün"
        } else {
          val diffSec = diffMs / 1000
          val diffMin = diffSec / 60
          val diffHours = diffMin / 60
          val diffDays = diffHours / 24

          when {
            diffMin < 1 -> "Az önce"
            diffMin < 60 -> "$diffMin dk önce"
            diffHours < 24 -> "$diffHours saat önce"
            diffDays == 1L -> "Dün"
            diffDays < 30 -> "$diffDays gün önce"
            diffDays < 365 -> "${diffDays / 30} ay önce"
            else -> "${diffDays / 365} yıl önce"
          }
        }

        return "$formattedDate ($relative)"
      }
    } catch (_: Exception) {
      // ignore
    }

    return trimmed
  }

  fun determineBlocklistCategory(id: String, name: String, desc: String): String {
    val lower = "$id $name $desc".lowercase()
    return when {
      lower.contains("malware") || lower.contains("threat") || lower.contains("phishing") ||
        lower.contains("security") || lower.contains("güvenlik") || lower.contains("crypto") ||
        lower.contains("scam") || lower.contains("ransomware") || lower.contains("c2") -> "Güvenlik"
      lower.contains("privacy") || lower.contains("gizlilik") || lower.contains("telemetry") ||
        lower.contains("tracker") || lower.contains("tracking") || lower.contains("izle") ||
        lower.contains("facebook") || lower.contains("google") || lower.contains("smarttv") -> "Gizlilik"
      lower.contains("turk") || lower.contains("french") || lower.contains("german") ||
        lower.contains("polish") || lower.contains("persian") || lower.contains("arabic") ||
        lower.contains("korean") || lower.contains("chinese") || lower.contains("japan") ||
        lower.contains("regional") || lower.contains("bölge") || lower.contains("russian") ||
        lower.contains("czech") || lower.contains("vietnam") || lower.contains("spanish") ||
        lower.contains("israel") || lower.contains("lithuania") || lower.contains("indonesia") ||
        lower.contains("swedish") || lower.contains("finnish") || lower.contains("dutch") -> "Bölgesel"
      else -> "Genel"
    }
  }

  fun formatRelativeTime(rawTime: String?): String {
    return formatIsoDateWithRelative(rawTime)
  }
}

