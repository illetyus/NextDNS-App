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

class NextDnsRepository(
  private val preferences: NextDnsPreferences = NextDnsApp.preferences
) {
  private val TAG = "NextDnsRepo"
  private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var streamJob: Job? = null
  private var analyticsPollingJob: Job? = null

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

  private val _configSettings = MutableStateFlow(ConfigSettings())
  val configSettings = _configSettings.asStateFlow()

  private val _testResult = MutableStateFlow(DiagnosticTestResult())
  val testResult = _testResult.asStateFlow()

  private val _isLiveStreaming = MutableStateFlow(false)
  val isLiveStreaming = _isLiveStreaming.asStateFlow()

  private val _isSyncing = MutableStateFlow(false)
  val isSyncing = _isSyncing.asStateFlow()

  init {
    repoScope.launch {
      runDiagnosticTest()
      val savedKey = preferences.apiKey
      val savedPid = preferences.activeProfileId
      if (savedKey.isNotBlank()) {
        loginWithApiKey(savedKey, restoreProfileId = savedPid)
      } else if (savedPid.isNotBlank()) {
        loadLocalProfileData(savedPid)
      } else {
        loadDemoData()
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

  // =========================================================================
  // Demo & Local Data
  // =========================================================================

  private fun loadDemoData() {
    val demoProfiles = listOf(
      NextDnsProfile(id = "82a32a", name = "Hasiggome"),
      NextDnsProfile(id = "91b42c", name = "Ev Ağı"),
      NextDnsProfile(id = "44d81e", name = "Telefonum")
    )
    _profiles.value = demoProfiles
    preferences.saveProfiles(demoProfiles)

    val pid = "82a32a"
    _activeProfileId.value = pid
    preferences.activeProfileId = pid

    loadLocalProfileData(pid)
  }

  fun loadLocalProfileData(profileId: String) {
    _securitySettings.value = preferences.getSecuritySettings(profileId) ?: SecuritySettings()
    _privacySettings.value = preferences.getPrivacySettings(profileId) ?: PrivacySettings()
    _parentalControlSettings.value = preferences.getParentalControlSettings(profileId) ?: ParentalControlSettings()
    _denylist.value = preferences.getDenylist(profileId) ?: listOf(
      AllowDenyItem(id = "trendyol", domain = "*.trendyol.com", active = true),
      AllowDenyItem(id = "hizliresim", domain = "*.hizliresim.com", active = true),
      AllowDenyItem(id = "netflix", domain = "*.netflix.com", active = true),
      AllowDenyItem(id = "douyin", domain = "*.douyin.com", active = true),
      AllowDenyItem(id = "aliexpress", domain = "*.aliexpress.com", active = true)
    )
    _allowlist.value = preferences.getAllowlist(profileId) ?: listOf(
      AllowDenyItem(id = "adguard", domain = "*.local.adguard.org", active = true),
      AllowDenyItem(id = "github", domain = "*.github.com", active = true),
      AllowDenyItem(id = "spotify", domain = "*.spotify.com", active = true)
    )
    _configSettings.value = preferences.getConfigSettings(profileId) ?: ConfigSettings()
    _logs.value = preferences.getLogs(profileId) ?: listOf(
      DnsLogEntry(id = "1", timestamp = "2 sn önce", domain = "local.adguard.org", deviceName = "Hasiggome-PC", blocked = false, protocol = "DoH"),
      DnsLogEntry(id = "2", timestamp = "5 sn önce", domain = "sdkconfig.ad.intl.xiaomi.com", deviceName = "Hasiggome-Mobile", blocked = true, blockReason = "AdGuard DNS filter", protocol = "DoH"),
      DnsLogEntry(id = "3", timestamp = "12 sn önce", domain = "play.google.com", deviceName = "Hasiggome-Mobile", blocked = false, protocol = "DoH"),
      DnsLogEntry(id = "4", timestamp = "25 sn önce", domain = "api-adservices.apple.com", deviceName = "Hasiggome-PC", blocked = true, blockReason = "Yerel İzleme Koruması", protocol = "DoH"),
      DnsLogEntry(id = "5", timestamp = "40 sn önce", domain = "connectivitycheck.gstatic.com", deviceName = "Hasiggome-PC", blocked = false, protocol = "DoH")
    )
  }

  // =========================================================================
  // Diagnostic Tests
  // =========================================================================

  suspend fun runDiagnosticTest(): DiagnosticTestResult = withContext(Dispatchers.IO) {
    val fallback = DiagnosticTestResult(
      status = "using-nextdns",
      profileId = _activeProfileId.value.ifBlank { "82a32a" },
      clientIp = "37.130.67.187"
    )

    val startT = System.currentTimeMillis()
    val response = safeApiCall("runDiagnosticTest") {
      NextDnsNetworkClient.testApi.testConnection()
    } ?: run {
      _testResult.value = fallback
      return@withContext fallback
    }

    val body = response.body()
    if (!response.isSuccessful || body == null) {
      _testResult.value = fallback
      return@withContext fallback
    }

    val latency = (System.currentTimeMillis() - startT).toInt()
    val result = DiagnosticTestResult(
      status = body.status ?: "using-nextdns",
      protocol = body.protocol ?: "DoH",
      profileId = body.profile ?: _activeProfileId.value.ifBlank { "82a32a" },
      clientIp = body.client ?: "37.130.67.187",
      serverPoP = body.server ?: "ist-1",
      latencyMs = latency,
      isEncrypted = true
    )
    _testResult.value = result
    result
  }

  // =========================================================================
  // Authentication & Profile Fetching
  // =========================================================================

  suspend fun loginWithApiKey(key: String, restoreProfileId: String? = null): Result<Int> = withContext(Dispatchers.IO) {
    _apiStatus.value = ApiConnectionStatus.Connecting
    val response = safeApiCall("loginWithApiKey") {
      NextDnsNetworkClient.api.getProfiles(key)
    }

    _apiKey.value = key
    preferences.apiKey = key

    val apiProfiles = response?.body()?.data
    if (response == null || !response.isSuccessful || apiProfiles == null) {
      loadDemoData()
      _apiStatus.value = ApiConnectionStatus.Connected(3)
      return@withContext Result.success(3)
    }

    val mapped = if (apiProfiles.isNotEmpty()) {
      apiProfiles.map { NextDnsProfile(id = it.id, name = it.name, fingerprint = it.fingerprint ?: "") }
    } else {
      listOf(NextDnsProfile(id = "82a32a", name = "Hasiggome"))
    }

    _profiles.value = mapped
    preferences.saveProfiles(mapped)

    val targetProfileId = if (!restoreProfileId.isNullOrBlank() && mapped.any { it.id == restoreProfileId }) {
      restoreProfileId
    } else {
      mapped.first().id
    }

    _activeProfileId.value = targetProfileId
    preferences.activeProfileId = targetProfileId
    _apiStatus.value = ApiConnectionStatus.Connected(mapped.size)

    loadLocalProfileData(targetProfileId)
    loadActiveProfileDataFromApi(key, targetProfileId)
    Result.success(mapped.size)
  }

  suspend fun loadActiveProfileDataFromApi(key: String = _apiKey.value, profileId: String = _activeProfileId.value) = withContext(Dispatchers.IO) {
    if (key.isBlank() || profileId.isBlank()) return@withContext
    _isSyncing.value = true

    try {
      applySecuritySettingsFromApi(key, profileId)
      applyPrivacySettingsFromApi(key, profileId)
      applyParentalSettingsFromApi(key, profileId)
      applyDenylistFromApi(key, profileId)
      applyAllowlistFromApi(key, profileId)
      applyConfigSettingsFromApi(key, profileId)
      applyLogsFromApi(key, profileId)
      applyDevicesAnalyticsFromApi(key, profileId)
    } finally {
      fetchAnalytics(key, profileId, null, null)
      _isSyncing.value = false
    }
  }

  // =========================================================================
  // Settings Loading Sub-Routines (Guard Clause & Single Responsibility)
  // =========================================================================

  private suspend fun applySecuritySettingsFromApi(key: String, profileId: String) {
    val secResp = safeApiCall("applySecuritySettings") { NextDnsNetworkClient.api.getSecurity(key, profileId) } ?: return
    val d = secResp.body()?.data ?: return
    if (!secResp.isSuccessful) return

    val tldList = d.tlds?.map { it.id } ?: _securitySettings.value.blockedTlds
    val updated = SecuritySettings(
      threatIntelligenceFeeds = d.threatIntelligenceFeeds ?: true,
      aiThreatDetection = d.aiThreatDetection ?: true,
      googleSafeBrowsing = d.googleSafeBrowsing ?: true,
      cryptojacking = d.cryptojacking ?: true,
      dnsRebinding = d.dnsRebinding ?: true,
      idnHomographs = d.idnHomographs ?: true,
      typosquatting = d.typosquatting ?: true,
      dga = d.dga ?: true,
      nrd = d.nrd ?: true,
      ddns = d.ddns ?: false,
      parkedDomains = d.parking ?: true,
      csam = d.csam ?: true,
      blockedTlds = tldList
    )
    _securitySettings.value = updated
    preferences.saveSecuritySettings(profileId, updated)
  }

  private suspend fun applyPrivacySettingsFromApi(key: String, profileId: String) {
    var activeBlocklistDtos: List<BlocklistDto>? = null
    var activeNativeDtos: List<NativeTrackingDto>? = null
    var disguisedTrackersVal: Boolean? = null
    var allowAffiliatesVal: Boolean? = null

    val bResp = safeApiCall("getProfileBlocklists") { NextDnsNetworkClient.api.getProfileBlocklists(key, profileId) }
    if (bResp?.isSuccessful == true) {
      activeBlocklistDtos = bResp.body()?.data
    }

    val nResp = safeApiCall("getProfileNatives") { NextDnsNetworkClient.api.getProfileNatives(key, profileId) }
    if (nResp?.isSuccessful == true) {
      activeNativeDtos = nResp.body()?.data
    }

    val privResp = safeApiCall("getPrivacy") { NextDnsNetworkClient.api.getPrivacy(key, profileId) }
    if (privResp?.isSuccessful == true && privResp.body()?.data != null) {
      val p = privResp.body()!!.data!!
      if (activeBlocklistDtos == null) activeBlocklistDtos = p.blocklists
      if (activeNativeDtos == null) activeNativeDtos = p.natives
      disguisedTrackersVal = p.disguisedTrackers
      allowAffiliatesVal = p.allowAffiliateLinks
    }

    val baseCatalog = _privacySettings.value.blocklists.ifEmpty { PrivacySettings.defaultBlocklistCatalog() }
    val updatedBlocklists = if (activeBlocklistDtos != null) {
      val activeMap = activeBlocklistDtos.associateBy { it.id.lowercase().trim() }
      val catalogMerged = baseCatalog.map { catItem ->
        val cleanId = catItem.id.lowercase().trim()
        val remote = activeMap[cleanId] ?: activeBlocklistDtos.find { it.name?.equals(catItem.name, ignoreCase = true) == true }
        if (remote != null) {
          catItem.copy(
            id = remote.id,
            active = true,
            name = remote.name?.takeIf { it.isNotBlank() } ?: catItem.name,
            description = remote.description?.takeIf { it.isNotBlank() } ?: catItem.description,
            entriesCount = remote.entries ?: catItem.entriesCount,
            website = remote.website ?: catItem.website
          )
        } else {
          catItem.copy(active = false)
        }
      }.toMutableList()

      val catalogIds = catalogMerged.map { it.id.lowercase().trim() }.toSet()
      activeBlocklistDtos.forEach { dto ->
        if (!catalogIds.contains(dto.id.lowercase().trim())) {
          catalogMerged.add(
            BlocklistEntry(
              id = dto.id,
              name = dto.name ?: dto.id,
              description = dto.description ?: "Özel/Harici liste.",
              entriesCount = dto.entries ?: 0L,
              active = true,
              website = dto.website ?: "",
              category = "Özel"
            )
          )
        }
      }
      catalogMerged
    } else {
      baseCatalog
    }

    val baseNatives = _privacySettings.value.nativeTracking.ifEmpty { PrivacySettings.defaultNativeTracking() }
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
  }

  private suspend fun applyParentalSettingsFromApi(key: String, profileId: String) {
    val parentResp = safeApiCall("getParentalControl") { NextDnsNetworkClient.api.getParentalControl(key, profileId) } ?: return
    val pc = parentResp.body()?.data ?: return
    if (!parentResp.isSuccessful) return

    val activeServiceMap = pc.services?.associate { it.id.lowercase().trim() to (it.active != false) } ?: emptyMap()
    val activeCatMap = pc.categories?.associate { it.id.lowercase().trim() to (it.active != false) } ?: emptyMap()

    val currentServices = _parentalControlSettings.value.services
    val currentCats = _parentalControlSettings.value.categories

    val updatedServices = currentServices.map { s ->
      val clean = s.id.lowercase().trim()
      if (activeServiceMap.containsKey(clean)) s.copy(active = activeServiceMap[clean] == true) else s
    }
    val updatedCats = currentCats.map { c ->
      val clean = c.id.lowercase().trim()
      if (activeCatMap.containsKey(clean)) c.copy(active = activeCatMap[clean] == true) else c
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
  }

  private suspend fun applyDenylistFromApi(key: String, profileId: String) {
    val denyResp = safeApiCall("getDenylist") { NextDnsNetworkClient.api.getDenylist(key, profileId) } ?: return
    val items = denyResp.body()?.data ?: return
    if (!denyResp.isSuccessful) return

    val list = items.map { AllowDenyItem(id = it.id, domain = it.id, active = it.active != false) }
    _denylist.value = list
    preferences.saveDenylist(profileId, list)
  }

  private suspend fun applyAllowlistFromApi(key: String, profileId: String) {
    val allowResp = safeApiCall("getAllowlist") { NextDnsNetworkClient.api.getAllowlist(key, profileId) } ?: return
    val items = allowResp.body()?.data ?: return
    if (!allowResp.isSuccessful) return

    val list = items.map { AllowDenyItem(id = it.id, domain = it.id, active = it.active != false) }
    _allowlist.value = list
    preferences.saveAllowlist(profileId, list)
  }

  private suspend fun applyConfigSettingsFromApi(key: String, profileId: String) {
    val cfgResp = safeApiCall("getSettings") { NextDnsNetworkClient.api.getSettings(key, profileId) } ?: return
    val s = cfgResp.body()?.data ?: return
    if (!cfgResp.isSuccessful) return

    val locName = when (s.logs?.location) {
      "ch" -> "İsviçre (CH)"
      "eu" -> "Avrupa Birliği (AB)"
      else -> "Amerika Birleşik Devletleri (ABD)"
    }
    val retName = when (s.logs?.retention) {
      6 -> "6 saat"
      24 -> "1 gün"
      168 -> "1 hafta"
      720 -> "1 ay"
      2160 -> "3 ay"
      4320 -> "6 ay"
      8760 -> "1 yıl"
      else -> "2 yıl"
    }

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
  }

  private suspend fun applyLogsFromApi(key: String, profileId: String) {
    val logsResp = safeApiCall("getLogs") { NextDnsNetworkClient.api.getLogs(key, profileId) } ?: return
    val dtoList = logsResp.body()?.data ?: return
    if (!logsResp.isSuccessful) return

    val fetchedLogs = parseLogsResponse(dtoList)
    if (fetchedLogs.isNotEmpty()) {
      _logs.value = fetchedLogs
      preferences.saveLogs(profileId, fetchedLogs)
    }
  }

  private suspend fun applyDevicesAnalyticsFromApi(key: String, profileId: String) {
    val devResp = safeApiCall("getAnalyticsDevices") { NextDnsNetworkClient.api.getAnalyticsDevices(key, profileId) } ?: return
    val dtoList = devResp.body()?.data ?: return
    if (!devResp.isSuccessful) return

    val devItems = dtoList.map {
      DeviceMetric(name = it.name ?: it.id ?: "Cihaz", queries = it.queries ?: 0L)
    }
    if (devItems.isNotEmpty()) {
      _analytics.value = _analytics.value.copy(topDevices = devItems)
    }
  }

  // =========================================================================
  // Logs Parsing
  // =========================================================================

  private fun parseLogsResponse(dtoList: List<DnsLogDto>): List<DnsLogEntry> {
    return dtoList.map { l ->
      DnsLogEntry(
        id = UUID.randomUUID().toString(),
        timestamp = l.timestamp?.toString() ?: "",
        domain = l.domain ?: "unknown.com",
        deviceName = l.device?.name ?: l.deviceName ?: "Bilinmeyen Cihaz",
        blocked = l.status == "blocked",
        blockReason = l.reasons?.firstOrNull()?.name,
        protocol = l.protocol ?: "DoH",
        responseTimeMs = l.responseTime ?: 14
      )
    }
  }

  // =========================================================================
  // Profile Management
  // =========================================================================

  fun logout() {
    preferences.clear()
    _apiKey.value = ""
    _activeProfileId.value = ""
    _profiles.value = emptyList()
    _apiStatus.value = ApiConnectionStatus.Disconnected
  }

  fun setActiveProfile(profileId: String) {
    _activeProfileId.value = profileId
    preferences.activeProfileId = profileId
    loadLocalProfileData(profileId)
  }

  suspend fun createProfileRemote(name: String): Result<NextDnsProfile> = withContext(Dispatchers.IO) {
    val key = _apiKey.value
    val newId = UUID.randomUUID().toString().take(6)

    if (key.isNotBlank()) {
      val resp = safeApiCall("createProfile") {
        NextDnsNetworkClient.api.createProfile(key, NameRequest(name = name))
      }
      val p = resp?.body()?.data
      if (resp?.isSuccessful == true && p != null) {
        val created = NextDnsProfile(id = p.id, name = p.name)
        val updatedList = _profiles.value + created
        _profiles.value = updatedList
        preferences.saveProfiles(updatedList)
        _activeProfileId.value = p.id
        preferences.activeProfileId = p.id
        loadLocalProfileData(p.id)
        return@withContext Result.success(created)
      }
    }

    val fallback = NextDnsProfile(id = newId, name = name)
    val updatedList = _profiles.value + fallback
    _profiles.value = updatedList
    preferences.saveProfiles(updatedList)
    _activeProfileId.value = newId
    preferences.activeProfileId = newId
    loadLocalProfileData(newId)
    Result.success(fallback)
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
        if (target.active) {
          NextDnsNetworkClient.api.addBlocklist(key, pid, IdRequest(id = blocklistId))
        } else {
          NextDnsNetworkClient.api.removeBlocklist(key, pid, blocklistId)
        }
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
        if (target.active) {
          NextDnsNetworkClient.api.addNativeTracking(key, pid, IdRequest(id = nativeId))
        } else {
          NextDnsNetworkClient.api.removeNativeTracking(key, pid, nativeId)
        }
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
        if (target.active) {
          val patchResp = NextDnsNetworkClient.api.updateParentalService(key, pid, serviceId, ParentalActiveRequest(active = true))
          if (!patchResp.isSuccessful) {
            NextDnsNetworkClient.api.addParentalService(key, pid, ParentalItemRequest(id = serviceId, active = true))
          }
        } else {
          val patchResp = NextDnsNetworkClient.api.updateParentalService(key, pid, serviceId, ParentalActiveRequest(active = false))
          if (!patchResp.isSuccessful) {
            NextDnsNetworkClient.api.removeParentalService(key, pid, serviceId)
          }
        }
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
        if (target.active) {
          val patchResp = NextDnsNetworkClient.api.updateParentalCategory(key, pid, categoryId, ParentalActiveRequest(active = true))
          if (!patchResp.isSuccessful) {
            NextDnsNetworkClient.api.addParentalCategory(key, pid, ParentalItemRequest(id = categoryId, active = true))
          }
        } else {
          val patchResp = NextDnsNetworkClient.api.updateParentalCategory(key, pid, categoryId, ParentalActiveRequest(active = false))
          if (!patchResp.isSuccessful) {
            NextDnsNetworkClient.api.removeParentalCategory(key, pid, categoryId)
          }
        }
      }
    }
  }

  // =========================================================================
  // Denylist & Allowlist
  // =========================================================================

  fun addToDenylist(domain: String) {
    val current = _denylist.value.filter { it.domain != domain && it.id != domain }
    val updated = listOf(AllowDenyItem(id = domain, domain = domain, active = true)) + current
    _denylist.value = updated
    preferences.saveDenylist(_activeProfileId.value, updated)

    val key = _apiKey.value
    val pid = _activeProfileId.value
    if (key.isBlank() || pid.isBlank()) return

    repoScope.launch {
      safeApiCall("addToDenylist") {
        NextDnsNetworkClient.api.addDenylist(key, pid, AllowDenyItemRequest(id = domain, active = true))
      }
    }
  }

  fun removeFromDenylist(id: String) {
    val updated = _denylist.value.filter { it.id != id && it.domain != id }
    _denylist.value = updated
    preferences.saveDenylist(_activeProfileId.value, updated)

    val key = _apiKey.value
    val pid = _activeProfileId.value
    if (key.isBlank() || pid.isBlank()) return

    repoScope.launch {
      safeApiCall("removeFromDenylist") {
        NextDnsNetworkClient.api.removeDenylist(key, pid, id)
      }
    }
  }

  fun toggleDenylistItem(id: String) {
    val targetItem = _denylist.value.find { it.id == id || it.domain == id }
    val newActive = !(targetItem?.active ?: true)
    val updated = _denylist.value.map {
      if (it.id == id || it.domain == id) it.copy(active = newActive) else it
    }
    _denylist.value = updated
    preferences.saveDenylist(_activeProfileId.value, updated)

    val key = _apiKey.value
    val pid = _activeProfileId.value
    val domain = targetItem?.domain ?: id
    if (key.isBlank() || pid.isBlank()) return

    repoScope.launch {
      safeApiCall("toggleDenylistItem") {
        val patchResp = NextDnsNetworkClient.api.toggleDenylist(key, pid, domain, AllowDenyActiveRequest(active = newActive))
        if (!patchResp.isSuccessful) {
          NextDnsNetworkClient.api.addDenylist(key, pid, AllowDenyItemRequest(id = domain, active = newActive))
        }
      }
    }
  }

  fun addToAllowlist(domain: String) {
    val current = _allowlist.value.filter { it.domain != domain && it.id != domain }
    val updated = listOf(AllowDenyItem(id = domain, domain = domain, active = true)) + current
    _allowlist.value = updated
    preferences.saveAllowlist(_activeProfileId.value, updated)

    val key = _apiKey.value
    val pid = _activeProfileId.value
    if (key.isBlank() || pid.isBlank()) return

    repoScope.launch {
      safeApiCall("addToAllowlist") {
        NextDnsNetworkClient.api.addAllowlist(key, pid, AllowDenyItemRequest(id = domain, active = true))
      }
    }
  }

  fun removeFromAllowlist(id: String) {
    val updated = _allowlist.value.filter { it.id != id && it.domain != id }
    _allowlist.value = updated
    preferences.saveAllowlist(_activeProfileId.value, updated)

    val key = _apiKey.value
    val pid = _activeProfileId.value
    if (key.isBlank() || pid.isBlank()) return

    repoScope.launch {
      safeApiCall("removeFromAllowlist") {
        NextDnsNetworkClient.api.removeAllowlist(key, pid, id)
      }
    }
  }

  fun toggleAllowlistItem(id: String) {
    val targetItem = _allowlist.value.find { it.id == id || it.domain == id }
    val newActive = !(targetItem?.active ?: true)
    val updated = _allowlist.value.map {
      if (it.id == id || it.domain == id) it.copy(active = newActive) else it
    }
    _allowlist.value = updated
    preferences.saveAllowlist(_activeProfileId.value, updated)

    val key = _apiKey.value
    val pid = _activeProfileId.value
    val domain = targetItem?.domain ?: id
    if (key.isBlank() || pid.isBlank()) return

    repoScope.launch {
      safeApiCall("toggleAllowlistItem") {
        val patchResp = NextDnsNetworkClient.api.toggleAllowlist(key, pid, domain, AllowDenyActiveRequest(active = newActive))
        if (!patchResp.isSuccessful) {
          NextDnsNetworkClient.api.addAllowlist(key, pid, AllowDenyItemRequest(id = domain, active = newActive))
        }
      }
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
        val retHours = when (cfg.logRetention) {
          "6 saat" -> 6
          "1 gün" -> 24
          "1 hafta" -> 168
          "1 ay" -> 720
          "3 ay" -> 2160
          "6 ay" -> 4320
          "1 yıl" -> 8760
          else -> 17520
        }
        val locCode = when (cfg.logStorageLocation) {
          "İsviçre (CH)" -> "ch"
          "Avrupa Birliği (AB)" -> "eu"
          else -> "us"
        }

        val logsDto = SettingsLogsDto(
          enabled = cfg.logsEnabled,
          retention = retHours,
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
              retention = retHours,
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

  suspend fun refreshLogsFromApi() {
    val key = _apiKey.value
    val pid = _activeProfileId.value
    if (key.isBlank() || pid.isBlank()) return

    val logsResp = safeApiCall("refreshLogsFromApi") {
      NextDnsNetworkClient.api.getLogs(key, pid)
    } ?: return

    val dtoList = logsResp.body()?.data ?: return
    if (!logsResp.isSuccessful) return

    val fetchedLogs = parseLogsResponse(dtoList)
    _logs.value = fetchedLogs
    preferences.saveLogs(pid, fetchedLogs)
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
        fetchAnalytics(_apiKey.value, _activeProfileId.value)
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

    streamJob = repoScope.launch(Dispatchers.IO) {
      var lastId: String? = null
      while (isActive) {
        try {
          var url = "https://api.nextdns.io/profiles/$pid/logs/stream"
          if (lastId != null) {
            url += "?id=$lastId"
          }

          val req = Request.Builder()
            .url(url)
            .header("X-Api-Key", key)
            .header("Accept", "text/event-stream")
            .build()

          NextDnsNetworkClient.client.newCall(req).execute().use { response ->
            if (!response.isSuccessful) return@use

            val source = response.body?.source() ?: return@use

            while (isActive && !source.exhausted()) {
              val line = source.readUtf8Line() ?: break

              if (line.startsWith("id: ")) {
                lastId = line.substring(4)
              } else if (line.startsWith("data: ")) {
                val jsonStr = line.substring(6)
                if (jsonStr.isNotBlank()) {
                  val logDto = safeApiCall("sseJsonParse") {
                    NextDnsNetworkClient.moshi.adapter(DnsLogDto::class.java).fromJson(jsonStr)
                  }
                  if (logDto != null) {
                    val entry = DnsLogEntry(
                      id = UUID.randomUUID().toString(),
                      timestamp = logDto.timestamp?.toString() ?: "",
                      domain = logDto.domain ?: "unknown.com",
                      clientIp = logDto.clientIp,
                      deviceName = logDto.device?.name ?: logDto.deviceName ?: "Bilinmeyen Cihaz",
                      blocked = logDto.status == "blocked",
                      blockReason = logDto.reasons?.firstOrNull()?.name,
                      protocol = logDto.protocol ?: "DoH",
                      responseTimeMs = null
                    )
                    withContext(Dispatchers.Main) {
                      _logs.value = listOf(entry) + _logs.value.take(199)
                    }
                  }
                }
              }
            }
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.e(TAG, "Stream error: ${e.message}")
          delay(2000)
        }
      }
    }
  }

  fun stopLogsStream() {
    streamJob?.cancel()
    streamJob = null
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
    val totalQueries = if (allQueries > 0) allQueries else 100L
    return Pair(totalQueries, blockedQueries)
  }

  private fun parseTopDevices(
    response: Response<NextDnsApiResponse<List<AnalyticsDeviceItem>>>?
  ): List<DeviceMetric> {
    if (response == null || !response.isSuccessful) return _analytics.value.topDevices
    return response.body()?.data?.map {
      DeviceMetric(name = it.name ?: it.id ?: "Bilinmeyen", queries = it.queries ?: 0)
    } ?: _analytics.value.topDevices
  }

  private fun parseTopDomains(
    response: Response<NextDnsApiResponse<List<AnalyticsDomainItem>>>?,
    fallback: List<DomainMetric> = emptyList()
  ): List<DomainMetric> {
    if (response == null || !response.isSuccessful) return fallback
    return response.body()?.data?.map {
      DomainMetric(domain = it.domain ?: "Bilinmeyen", queries = it.queries ?: 0)
    } ?: fallback
  }

  private fun parseBlockedReasons(
    response: Response<NextDnsApiResponse<List<AnalyticsReasonItem>>>?
  ): Map<String, Long> {
    if (response == null || !response.isSuccessful) return _analytics.value.topBlockedReasons
    val rMap = mutableMapOf<String, Long>()
    response.body()?.data?.forEach { item ->
      rMap[item.id ?: item.name ?: "Diğer"] = item.queries ?: 0L
    }
    return rMap
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
        val pct = if (totalQueries > 0) (count.toDouble() / totalQueries) * 100.0 else 0.0
        val companyName = item.company ?: item.name ?: item.id ?: "Diğer"
        gafamMetrics[companyName] = Pair(pct, count)
      }
    }
    return if (gafamMetrics.isNotEmpty()) gafamMetrics else _analytics.value.gafamMetrics
  }

  private fun parseCountryMetrics(
    response: Response<NextDnsApiResponse<List<AnalyticsItemDto>>>?,
    totalQueries: Long
  ): List<Pair<String, Double>> {
    val topCountries = mutableListOf<Pair<String, Double>>()
    if (response != null && response.isSuccessful) {
      response.body()?.data?.forEach { item ->
        val count = item.queries ?: 0L
        val pct = if (totalQueries > 0) (count.toDouble() / totalQueries) * 100.0 else 0.0
        val code = item.code ?: ""
        val name = if (code.length == 2) {
          Locale("", code).getDisplayName(Locale("tr"))
        } else {
          "Bilinmeyen"
        }
        topCountries.add(Pair(name, pct))
      }
    }
    return if (topCountries.isNotEmpty()) topCountries else _analytics.value.topCountries
  }

  private fun parseDnssecPercentage(
    response: Response<NextDnsApiResponse<List<AnalyticsItemDto>>>?,
    totalQueries: Long
  ): Double {
    if (response == null || !response.isSuccessful) return _analytics.value.dnssecPercentage.toDouble()
    val items = response.body()?.data ?: emptyList()
    val validated = items.find { it.validated == true || it.id == "validated" || it.id == "true" }?.queries ?: 0L
    return if (totalQueries > 0) (validated.toDouble() / totalQueries) * 100.0 else 0.0
  }

  private fun parseEncryptionPercentage(
    response: Response<NextDnsApiResponse<List<AnalyticsItemDto>>>?,
    totalQueries: Long
  ): Double {
    if (response == null || !response.isSuccessful) return _analytics.value.encryptedDnsPercentage.toDouble()
    val items = response.body()?.data ?: emptyList()
    val encrypted = items.find { it.encrypted == true || it.id == "encrypted" || it.id == "true" }?.queries ?: 0L
    return if (totalQueries > 0) (encrypted.toDouble() / totalQueries) * 100.0 else 0.0
  }

  // =========================================================================
  // Main Analytics Fetching Function
  // =========================================================================

  suspend fun fetchAnalytics(key: String, profileId: String, device: String? = null, from: String? = null) {
    if (key.isBlank() || profileId.isBlank()) return

    val devParam = if (device == "Tüm cihazlar" || device.isNullOrBlank()) null else device
    val fromParam = when (from) {
      "Son 1 Saat" -> "-1h"
      "Son 24 Saat" -> "-24h"
      "Son 7 Gün" -> "-7d"
      "Son 30 Gün" -> "-30d"
      "Son 90 Gün" -> "-90d"
      else -> "-30d"
    }

    val statusResp = safeApiCall("getAnalyticsStatus") { NextDnsNetworkClient.api.getAnalyticsStatus(key, profileId, devParam, fromParam) }
    val devicesResp = safeApiCall("getAnalyticsDevices") { NextDnsNetworkClient.api.getAnalyticsDevices(key, profileId, devParam, fromParam) }
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
    val topAllowedDomains = parseTopDomains(allowedDomainsResp, _analytics.value.topAllowedDomains)
    val topBlockedDomains = parseTopDomains(blockedDomainsResp, _analytics.value.topBlockedDomains)
    val topDomains = parseTopDomains(rootDomainsResp, _analytics.value.topDomains)
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
      encryptedDnsPercentage = if (encryptionResp?.isSuccessful == true) encPct.toFloat() else _analytics.value.encryptedDnsPercentage,
      dnssecPercentage = if (dnssecResp?.isSuccessful == true) dnssecPct.toFloat() else _analytics.value.dnssecPercentage,
      topCountries = topCountries
    )

    _analytics.value = updatedAnalytics
  }
}
