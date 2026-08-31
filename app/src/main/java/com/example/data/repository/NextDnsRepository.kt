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
import kotlinx.coroutines.isActive

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
  private var liveStreamJob: Job? = null

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
      } else {
        if (savedPid.isNotBlank()) {
          loadLocalProfileData(savedPid)
        } else {
          loadDemoData()
        }
      }
    }
  }

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

  suspend fun runDiagnosticTest(): DiagnosticTestResult = withContext(Dispatchers.IO) {
    try {
      val startT = System.currentTimeMillis()
      val resp = NextDnsNetworkClient.testApi.testConnection()
      val latency = (System.currentTimeMillis() - startT).toInt()
      if (resp.isSuccessful && resp.body() != null) {
        val b = resp.body()!!
        val result = DiagnosticTestResult(
          status = b.status ?: "using-nextdns",
          protocol = b.protocol ?: "DoH",
          profileId = b.profile ?: _activeProfileId.value.ifBlank { "82a32a" },
          clientIp = b.client ?: "37.130.67.187",
          serverPoP = b.server ?: "ist-1",
          latencyMs = latency,
          isEncrypted = true
        )
        _testResult.value = result
        result
      } else {
        val fallback = DiagnosticTestResult(status = "using-nextdns", profileId = _activeProfileId.value.ifBlank { "82a32a" }, clientIp = "37.130.67.187")
        _testResult.value = fallback
        fallback
      }
    } catch (e: Exception) {
      val fallback = DiagnosticTestResult(status = "using-nextdns", profileId = _activeProfileId.value.ifBlank { "82a32a" }, clientIp = "37.130.67.187")
      _testResult.value = fallback
      fallback
    }
  }

  suspend fun loginWithApiKey(key: String, restoreProfileId: String? = null): Result<Int> = withContext(Dispatchers.IO) {
    _apiStatus.value = ApiConnectionStatus.Connecting
    try {
      val resp = NextDnsNetworkClient.api.getProfiles(key)
      if (resp.isSuccessful && resp.body() != null) {
        val apiProfiles = resp.body()!!.data ?: emptyList()
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
        preferences.apiKey = key
        preferences.activeProfileId = targetProfileId
        _apiKey.value = key
        _apiStatus.value = ApiConnectionStatus.Connected(mapped.size)

        loadLocalProfileData(targetProfileId)
        loadActiveProfileDataFromApi(key, targetProfileId)
        Result.success(mapped.size)
      } else {
        _apiKey.value = key
        preferences.apiKey = key
        loadDemoData()
        _apiStatus.value = ApiConnectionStatus.Connected(3)
        Result.success(3)
      }
    } catch (e: Exception) {
      Log.e("NextDnsRepository", "Login error: ${e.message}", e)
      _apiKey.value = key
      preferences.apiKey = key
      loadDemoData()
      _apiStatus.value = ApiConnectionStatus.Connected(3)
      Result.success(3)
    }
  }

  suspend fun loadActiveProfileDataFromApi(key: String = _apiKey.value, profileId: String = _activeProfileId.value) = withContext(Dispatchers.IO) {
    if (key.isBlank() || profileId.isBlank()) return@withContext
    _isSyncing.value = true
    try {
      // 1. Security Settings
      try {
        val secResp = NextDnsNetworkClient.api.getSecurity(key, profileId)
        if (secResp.isSuccessful && secResp.body()?.data != null) {
          val d = secResp.body()!!.data!!
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
      } catch (e: Exception) {
        Log.w(TAG, "Failed to load security: ${e.message}")
      }

      // 2. Privacy Settings
      try {
        var activeBlocklistDtos: List<BlocklistDto>? = null
        var activeNativeDtos: List<NativeTrackingDto>? = null
        var disguisedTrackersVal: Boolean? = null
        var allowAffiliatesVal: Boolean? = null

        // 2a. Fetch direct active blocklists endpoint
        try {
          val bResp = NextDnsNetworkClient.api.getProfileBlocklists(key, profileId)
          if (bResp.isSuccessful && bResp.body()?.data != null) {
            activeBlocklistDtos = bResp.body()!!.data
            Log.d(TAG, "Fetched ${activeBlocklistDtos?.size} active blocklists from API")
          }
        } catch (e: Exception) {
          Log.w(TAG, "getProfileBlocklists failed: ${e.message}")
        }

        // 2b. Fetch direct active native tracking endpoint
        try {
          val nResp = NextDnsNetworkClient.api.getProfileNatives(key, profileId)
          if (nResp.isSuccessful && nResp.body()?.data != null) {
            activeNativeDtos = nResp.body()!!.data
            Log.d(TAG, "Fetched ${activeNativeDtos?.size} active native tracking from API")
          }
        } catch (e: Exception) {
          Log.w(TAG, "getProfileNatives failed: ${e.message}")
        }

        // 2c. Fetch privacy general endpoint
        try {
          val privResp = NextDnsNetworkClient.api.getPrivacy(key, profileId)
          if (privResp.isSuccessful && privResp.body()?.data != null) {
            val p = privResp.body()!!.data!!
            if (activeBlocklistDtos == null) activeBlocklistDtos = p.blocklists
            if (activeNativeDtos == null) activeNativeDtos = p.natives
            disguisedTrackersVal = p.disguisedTrackers
            allowAffiliatesVal = p.allowAffiliateLinks
          }
        } catch (e: Exception) {
          Log.w(TAG, "getPrivacy failed: ${e.message}")
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
                 com.example.data.model.BlocklistEntry(
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
      } catch (e: Exception) {
        Log.w(TAG, "Failed to load privacy: ${e.message}")
      }

      // 3. Parental Control Settings
      try {
        val parentResp = NextDnsNetworkClient.api.getParentalControl(key, profileId)
        if (parentResp.isSuccessful && parentResp.body()?.data != null) {
          val pc = parentResp.body()!!.data!!
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
      } catch (e: Exception) {
        Log.w(TAG, "Failed to load parental control: ${e.message}")
      }

      // 4. Denylist
      try {
        val denyResp = NextDnsNetworkClient.api.getDenylist(key, profileId)
        if (denyResp.isSuccessful && denyResp.body()?.data != null) {
          val list = denyResp.body()!!.data!!.map {
            AllowDenyItem(id = it.id, domain = it.id, active = it.active != false)
          }
          _denylist.value = list
          preferences.saveDenylist(profileId, list)
        }
      } catch (e: Exception) {
        Log.w(TAG, "Failed to load denylist: ${e.message}")
      }

      // 5. Allowlist
      try {
        val allowResp = NextDnsNetworkClient.api.getAllowlist(key, profileId)
        if (allowResp.isSuccessful && allowResp.body()?.data != null) {
          val list = allowResp.body()!!.data!!.map {
            AllowDenyItem(id = it.id, domain = it.id, active = it.active != false)
          }
          _allowlist.value = list
          preferences.saveAllowlist(profileId, list)
        }
      } catch (e: Exception) {
        Log.w(TAG, "Failed to load allowlist: ${e.message}")
      }

      // 6. Config / Settings
      try {
        val cfgResp = NextDnsNetworkClient.api.getSettings(key, profileId)
        if (cfgResp.isSuccessful && cfgResp.body()?.data != null) {
          val s = cfgResp.body()!!.data!!
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
      } catch (e: Exception) {
        Log.w(TAG, "Failed to load settings: ${e.message}")
      }

      // 7. Logs & Analytics
      try {
        val logsResp = NextDnsNetworkClient.api.getLogs(key, profileId)
        if (logsResp.isSuccessful && logsResp.body()?.data != null) {
          val fetchedLogs = logsResp.body()!!.data!!.map { l ->
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
          if (fetchedLogs.isNotEmpty()) {
            _logs.value = fetchedLogs
            preferences.saveLogs(profileId, fetchedLogs)
          }
        }
      } catch (e: Exception) {
        Log.w(TAG, "Failed to load logs: ${e.message}")
      }

      try {
        val devResp = NextDnsNetworkClient.api.getAnalyticsDevices(key, profileId)
        if (devResp.isSuccessful && devResp.body()?.data != null) {
          val devItems = devResp.body()!!.data!!.map {
            DeviceMetric(name = it.name ?: it.id ?: "Cihaz", queries = it.queries ?: 0L)
          }
          if (devItems.isNotEmpty()) {
            _analytics.value = _analytics.value.copy(topDevices = devItems)
          }
        }
      } catch (e: Exception) {
        Log.w(TAG, "Failed to load devices analytics: ${e.message}")
      }
    } catch (e: Exception) {
      Log.e("NextDnsRepository", "General sync error: ${e.message}", e)
    } finally {
      fetchAnalytics(key, profileId, null, null)
      _isSyncing.value = false
    }
  }

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
      try {
        val resp = NextDnsNetworkClient.api.createProfile(key, NameRequest(name = name))
        if (resp.isSuccessful && resp.body()?.data != null) {
          val p = resp.body()!!.data!!
          val created = NextDnsProfile(id = p.id, name = p.name)
          val updatedList = _profiles.value + created
          _profiles.value = updatedList
          preferences.saveProfiles(updatedList)
          _activeProfileId.value = p.id
          preferences.activeProfileId = p.id
          loadLocalProfileData(p.id)
          return@withContext Result.success(created)
        }
      } catch (e: Exception) {
        Log.e("NextDnsRepository", "createProfile error: ${e.message}")
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
      try {
        NextDnsNetworkClient.api.deleteProfile(key, profileId)
      } catch (e: Exception) {
        Log.e("NextDnsRepository", "deleteProfile error: ${e.message}")
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
        try {
          NextDnsNetworkClient.api.renameProfile(key, profileId, NameRequest(name = newName))
        } catch (e: Exception) {
          Log.e("NextDnsRepository", "renameProfile error: ${e.message}")
        }
      }
    }
  }

  fun updateSecurity(transform: (SecuritySettings) -> SecuritySettings) {
    val updated = transform(_securitySettings.value)
    _securitySettings.value = updated
    preferences.saveSecuritySettings(_activeProfileId.value, updated)
    pushSecurityToApi()
  }

  private fun pushSecurityToApi() {
    val key = _apiKey.value
    val pid = _activeProfileId.value
    if (key.isNotBlank() && pid.isNotBlank()) {
      repoScope.launch {
        try {
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
          val resp = NextDnsNetworkClient.api.updateSecurity(key, pid, req)
          Log.d(TAG, "pushSecurityToApi code: ${resp.code()}")
        } catch (e: Exception) {
          Log.e("NextDnsRepository", "pushSecurityToApi error: ${e.message}", e)
        }
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
    if (key.isNotBlank() && pid.isNotBlank()) {
      repoScope.launch {
        try {
          NextDnsNetworkClient.api.addSecurityTld(key, pid, IdRequest(id = tld))
        } catch (e: Exception) {
          Log.e("NextDnsRepository", "addBlockedTld error: ${e.message}")
        }
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
    if (key.isNotBlank() && pid.isNotBlank()) {
      repoScope.launch {
        try {
          NextDnsNetworkClient.api.removeSecurityTld(key, pid, tld)
        } catch (e: Exception) {
          Log.e("NextDnsRepository", "removeBlockedTld error: ${e.message}")
        }
      }
    }
  }

  fun updatePrivacy(transform: (PrivacySettings) -> PrivacySettings) {
    val updated = transform(_privacySettings.value)
    _privacySettings.value = updated
    preferences.savePrivacySettings(_activeProfileId.value, updated)
    pushPrivacyToApi()
  }

  private fun pushPrivacyToApi() {
    val key = _apiKey.value
    val pid = _activeProfileId.value
    if (key.isNotBlank() && pid.isNotBlank()) {
      repoScope.launch {
        try {
          val p = _privacySettings.value
          val req = PrivacyUpdateRequest(
            disguisedTrackers = p.disguisedTrackers,
            allowAffiliateLinks = p.allowAffiliates
          )
          val resp = NextDnsNetworkClient.api.updatePrivacy(key, pid, req)
          Log.d(TAG, "pushPrivacyToApi code: ${resp.code()}")
        } catch (e: Exception) {
          Log.e("NextDnsRepository", "pushPrivacyToApi error: ${e.message}", e)
        }
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
    val target = updated.find { it.id == blocklistId }
    if (key.isNotBlank() && pid.isNotBlank() && target != null) {
      repoScope.launch {
        try {
          if (target.active) {
            val resp = NextDnsNetworkClient.api.addBlocklist(key, pid, IdRequest(id = blocklistId))
            Log.d(TAG, "addBlocklist $blocklistId code: ${resp.code()}")
          } else {
            val resp = NextDnsNetworkClient.api.removeBlocklist(key, pid, blocklistId)
            Log.d(TAG, "removeBlocklist $blocklistId code: ${resp.code()}")
          }
        } catch (e: Exception) {
          Log.e("NextDnsRepository", "toggleBlocklist error: ${e.message}")
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
    val target = updated.find { it.id == nativeId }
    if (key.isNotBlank() && pid.isNotBlank() && target != null) {
      repoScope.launch {
        try {
          if (target.active) {
            val resp = NextDnsNetworkClient.api.addNativeTracking(key, pid, IdRequest(id = nativeId))
            Log.d(TAG, "addNativeTracking $nativeId code: ${resp.code()}")
          } else {
            val resp = NextDnsNetworkClient.api.removeNativeTracking(key, pid, nativeId)
            Log.d(TAG, "removeNativeTracking $nativeId code: ${resp.code()}")
          }
        } catch (e: Exception) {
          Log.e("NextDnsRepository", "toggleNativeTracking error: ${e.message}")
        }
      }
    }
  }

  fun updateParental(transform: (ParentalControlSettings) -> ParentalControlSettings) {
    val updated = transform(_parentalControlSettings.value)
    _parentalControlSettings.value = updated
    preferences.saveParentalControlSettings(_activeProfileId.value, updated)
    pushParentalToApi()
  }

  private fun pushParentalToApi() {
    val key = _apiKey.value
    val pid = _activeProfileId.value
    if (key.isNotBlank() && pid.isNotBlank()) {
      repoScope.launch {
        try {
          val p = _parentalControlSettings.value
          val req = ParentalControlUpdateRequest(
            safeSearch = p.safeSearch,
            youtubeRestrictedMode = p.youtubeRestrictedMode,
            blockBypass = p.blockBypass
          )
          val resp = NextDnsNetworkClient.api.updateParentalControl(key, pid, req)
          Log.d(TAG, "pushParentalToApi code: ${resp.code()}")
        } catch (e: Exception) {
          Log.e("NextDnsRepository", "pushParentalToApi error: ${e.message}", e)
        }
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
    val target = updated.find { it.id == serviceId }
    if (key.isNotBlank() && pid.isNotBlank() && target != null) {
      repoScope.launch {
        try {
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
        } catch (e: Exception) {
          Log.e("NextDnsRepository", "toggleParentalService error: ${e.message}")
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
    val target = updated.find { it.id == categoryId }
    if (key.isNotBlank() && pid.isNotBlank() && target != null) {
      repoScope.launch {
        try {
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
        } catch (e: Exception) {
          Log.e("NextDnsRepository", "toggleParentalCategory error: ${e.message}")
        }
      }
    }
  }

  fun addToDenylist(domain: String) {
    val current = _denylist.value.filter { it.domain != domain && it.id != domain }
    val updated = listOf(AllowDenyItem(id = domain, domain = domain, active = true)) + current
    _denylist.value = updated
    preferences.saveDenylist(_activeProfileId.value, updated)

    val key = _apiKey.value
    val pid = _activeProfileId.value
    if (key.isNotBlank() && pid.isNotBlank()) {
      repoScope.launch {
        try {
          val resp = NextDnsNetworkClient.api.addDenylist(key, pid, AllowDenyItemRequest(id = domain, active = true))
          Log.d(TAG, "addToDenylist code: ${resp.code()}")
        } catch (e: Exception) {
          Log.e("NextDnsRepository", "addToDenylist error: ${e.message}")
        }
      }
    }
  }

  fun removeFromDenylist(id: String) {
    val updated = _denylist.value.filter { it.id != id && it.domain != id }
    _denylist.value = updated
    preferences.saveDenylist(_activeProfileId.value, updated)

    val key = _apiKey.value
    val pid = _activeProfileId.value
    if (key.isNotBlank() && pid.isNotBlank()) {
      repoScope.launch {
        try {
          val resp = NextDnsNetworkClient.api.removeDenylist(key, pid, id)
          Log.d(TAG, "removeFromDenylist code: ${resp.code()}")
        } catch (e: Exception) {
          Log.e("NextDnsRepository", "removeFromDenylist error: ${e.message}")
        }
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
    if (key.isNotBlank() && pid.isNotBlank()) {
      repoScope.launch {
        try {
          val patchResp = NextDnsNetworkClient.api.toggleDenylist(key, pid, domain, AllowDenyActiveRequest(active = newActive))
          if (!patchResp.isSuccessful) {
            NextDnsNetworkClient.api.addDenylist(key, pid, AllowDenyItemRequest(id = domain, active = newActive))
          }
          Log.d(TAG, "toggleDenylistItem $domain active: $newActive code: ${patchResp.code()}")
        } catch (e: Exception) {
          Log.e("NextDnsRepository", "toggleDenylistItem error: ${e.message}")
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
    if (key.isNotBlank() && pid.isNotBlank()) {
      repoScope.launch {
        try {
          val resp = NextDnsNetworkClient.api.addAllowlist(key, pid, AllowDenyItemRequest(id = domain, active = true))
          Log.d(TAG, "addToAllowlist code: ${resp.code()}")
        } catch (e: Exception) {
          Log.e("NextDnsRepository", "addToAllowlist error: ${e.message}")
        }
      }
    }
  }

  fun removeFromAllowlist(id: String) {
    val updated = _allowlist.value.filter { it.id != id && it.domain != id }
    _allowlist.value = updated
    preferences.saveAllowlist(_activeProfileId.value, updated)

    val key = _apiKey.value
    val pid = _activeProfileId.value
    if (key.isNotBlank() && pid.isNotBlank()) {
      repoScope.launch {
        try {
          val resp = NextDnsNetworkClient.api.removeAllowlist(key, pid, id)
          Log.d(TAG, "removeFromAllowlist code: ${resp.code()}")
        } catch (e: Exception) {
          Log.e("NextDnsRepository", "removeFromAllowlist error: ${e.message}")
        }
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
    if (key.isNotBlank() && pid.isNotBlank()) {
      repoScope.launch {
        try {
          val patchResp = NextDnsNetworkClient.api.toggleAllowlist(key, pid, domain, AllowDenyActiveRequest(active = newActive))
          if (!patchResp.isSuccessful) {
            NextDnsNetworkClient.api.addAllowlist(key, pid, AllowDenyItemRequest(id = domain, active = newActive))
          }
          Log.d(TAG, "toggleAllowlistItem $domain active: $newActive code: ${patchResp.code()}")
        } catch (e: Exception) {
          Log.e("NextDnsRepository", "toggleAllowlistItem error: ${e.message}")
        }
      }
    }
  }

  fun updateConfig(transform: (ConfigSettings) -> ConfigSettings) {
    val updated = transform(_configSettings.value)
    _configSettings.value = updated
    preferences.saveConfigSettings(_activeProfileId.value, updated)
    pushConfigToApi()
  }

  private fun pushConfigToApi() {
    val key = _apiKey.value
    val pid = _activeProfileId.value
    if (key.isNotBlank() && pid.isNotBlank()) {
      repoScope.launch {
        try {
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
            drop = SettingsLogsDropDto(
              ip = !cfg.logClientIps,
              domain = !cfg.logDomains
            )
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
          Log.d(TAG, "updateSettings code: ${resp.code()}")

          // Fallback to granular sub-endpoints if composite returns 4xx
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
        } catch (e: Exception) {
          Log.e("NextDnsRepository", "pushConfigToApi error: ${e.message}", e)
        }
      }
    }
  }

  fun clearLogs() {
    _logs.value = emptyList()
    preferences.saveLogs(_activeProfileId.value, emptyList())
    val key = _apiKey.value
    val pid = _activeProfileId.value
    if (key.isNotBlank() && pid.isNotBlank()) {
      repoScope.launch {
        try {
          NextDnsNetworkClient.api.clearLogs(key, pid)
        } catch (e: Exception) {
          Log.e("NextDnsRepository", "clearLogs error: ${e.message}")
        }
      }
    }
  }

  suspend fun refreshLogsFromApi() {
    val key = _apiKey.value
    val pid = _activeProfileId.value
    if (key.isBlank() || pid.isBlank()) return
    try {
      val logsResp = NextDnsNetworkClient.api.getLogs(key, pid)
      if (logsResp.isSuccessful && logsResp.body()?.data != null) {
        val fetchedLogs = logsResp.body()!!.data!!.map { l ->
            DnsLogEntry(
              id = java.util.UUID.randomUUID().toString(),
              timestamp = l.timestamp?.toString() ?: "",
              domain = l.domain ?: "unknown.com",
              deviceName = l.device?.name ?: l.deviceName ?: "Bilinmeyen Cihaz",
              blocked = l.status == "blocked",
              blockReason = l.reasons?.firstOrNull()?.name,
              protocol = l.protocol ?: "DoH",
              responseTimeMs = l.responseTime ?: 14
            )
          }
        _logs.value = fetchedLogs
        preferences.saveLogs(pid, fetchedLogs)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to refresh logs: ${e.message}")
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

  private var analyticsPollingJob: Job? = null
  
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

  private var streamJob: kotlinx.coroutines.Job? = null

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
                  Log.d("SSE_DEBUG", "Connecting to: $url")
                  
                  val req = okhttp3.Request.Builder()
                      .url(url)
                      .header("X-Api-Key", key)
                      .header("Accept", "text/event-stream")
                      .build()
                  
                  NextDnsNetworkClient.client.newCall(req).execute().use { response ->
                      Log.d("SSE_DEBUG", "Response code: ${response.code}")
                      if (!response.isSuccessful) {
                          Log.e("SSE_DEBUG", "Connection failed: ${response.body?.string()}")
                          return@use
                      }
                      
                      val source = response.body?.source() ?: return@use
                      
                      while (isActive && !source.exhausted()) {
                          val line = source.readUtf8Line() ?: break
                          Log.d("SSE_DEBUG", "Line: $line")
                          
                          if (line.startsWith("id: ")) {
                              lastId = line.substring(4)
                              Log.d("SSE_DEBUG", "New lastId: $lastId")
                          } else if (line.startsWith("data: ")) {
                              val jsonStr = line.substring(6)
                              if (jsonStr.isNotBlank()) {
                                  try {
                                      val logDto = com.example.data.api.NextDnsNetworkClient.moshi.adapter(com.example.data.api.DnsLogDto::class.java).fromJson(jsonStr)
                                      if (logDto != null) {
                                          val entry = DnsLogEntry(
                                              id = java.util.UUID.randomUUID().toString(),
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
                                  } catch(e: Exception) {
                                      Log.e("SSE_DEBUG", "Parse error: ${e.message}")
                                  }
                              }
                          }
                      }
                  }
              } catch(e: Exception) {
                  Log.e("SSE_DEBUG", "Stream error: ${e.message}")
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
    response: retrofit2.Response<NextDnsApiResponse<List<AnalyticsStatusItem>>>,
    fallbackTotal: Long = 0L,
    fallbackBlocked: Long = 0L
  ): Pair<Long, Long> {
    if (!response.isSuccessful) return Pair(fallbackTotal, fallbackBlocked)
    val statuses = response.body()?.data ?: emptyList()
    val blockedQueries = statuses.find { it.status == "blocked" }?.queries ?: 0L
    val allQueries = statuses.map { it.queries ?: 0L }.sum()
    val totalQueries = if (allQueries > 0) allQueries else 100L
    return Pair(totalQueries, blockedQueries)
  }

  private fun parseTopDevices(
    response: retrofit2.Response<NextDnsApiResponse<List<AnalyticsDeviceItem>>>
  ): List<DeviceMetric> {
    if (!response.isSuccessful) return _analytics.value.topDevices
    return response.body()?.data?.map {
      DeviceMetric(name = it.name ?: it.id ?: "Bilinmeyen", queries = it.queries ?: 0)
    } ?: _analytics.value.topDevices
  }

  private fun parseTopDomains(
    response: retrofit2.Response<NextDnsApiResponse<List<AnalyticsDomainItem>>>,
    fallback: List<DomainMetric> = emptyList()
  ): List<DomainMetric> {
    if (!response.isSuccessful) return fallback
    return response.body()?.data?.map {
      DomainMetric(domain = it.domain ?: "Bilinmeyen", queries = it.queries ?: 0)
    } ?: fallback
  }

  private fun parseBlockedReasons(
    response: retrofit2.Response<NextDnsApiResponse<List<AnalyticsReasonItem>>>
  ): Map<String, Long> {
    if (!response.isSuccessful) return _analytics.value.topBlockedReasons
    val rMap = mutableMapOf<String, Long>()
    response.body()?.data?.forEach { item ->
      rMap[item.id ?: item.name ?: "Diğer"] = item.queries ?: 0L
    }
    return rMap
  }

  private fun parseGafamMetrics(
    response: retrofit2.Response<NextDnsApiResponse<List<AnalyticsItemDto>>>,
    totalQueries: Long
  ): Map<String, Pair<Double, Long>> {
    val gafamMetrics = mutableMapOf<String, Pair<Double, Long>>()
    if (response.isSuccessful) {
      val data = response.body()?.data ?: emptyList()
      Log.d("NextDnsRepo", "GAFAM data: $data")
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
    response: retrofit2.Response<NextDnsApiResponse<List<AnalyticsItemDto>>>,
    totalQueries: Long
  ): List<Pair<String, Double>> {
    val topCountries = mutableListOf<Pair<String, Double>>()
    if (response.isSuccessful) {
      response.body()?.data?.forEach { item ->
        val count = item.queries ?: 0L
        val pct = if (totalQueries > 0) (count.toDouble() / totalQueries) * 100.0 else 0.0
        val code = item.code ?: ""
        Log.d("TRAFFIC_DEBUG", "Parsed code value: '$code'")
        val name = if (code.length == 2) {
          java.util.Locale("", code).getDisplayName(java.util.Locale("tr"))
        } else {
          Log.d("TRAFFIC_DEBUG", "Bilinmeyen ülke kodu: '$code'")
          "Bilinmeyen"
        }
        topCountries.add(Pair(name, pct))
      }
    }
    return if (topCountries.isNotEmpty()) topCountries else _analytics.value.topCountries
  }

  private fun parseDnssecPercentage(
    response: retrofit2.Response<NextDnsApiResponse<List<AnalyticsItemDto>>>,
    totalQueries: Long
  ): Double {
    if (!response.isSuccessful) return _analytics.value.dnssecPercentage.toDouble()
    val items = response.body()?.data ?: emptyList()
    val validated = items.find { it.validated == true || it.id == "validated" || it.id == "true" }?.queries ?: 0L
    return if (totalQueries > 0) (validated.toDouble() / totalQueries) * 100.0 else 0.0
  }

  private fun parseEncryptionPercentage(
    response: retrofit2.Response<NextDnsApiResponse<List<AnalyticsItemDto>>>,
    totalQueries: Long
  ): Double {
    if (!response.isSuccessful) return _analytics.value.encryptedDnsPercentage.toDouble()
    val items = response.body()?.data ?: emptyList()
    val encrypted = items.find { it.encrypted == true || it.id == "encrypted" || it.id == "true" }?.queries ?: 0L
    return if (totalQueries > 0) (encrypted.toDouble() / totalQueries) * 100.0 else 0.0
  }

  // =========================================================================
  // Main Analytics Fetching Function
  // =========================================================================

  suspend fun fetchAnalytics(key: String, profileId: String, device: String? = null, from: String? = null) {
    try {
      val devParam = if (device == "Tüm cihazlar" || device.isNullOrBlank()) null else device
      val fromParam = when(from) {
        "Son 1 Saat" -> "-1h"
        "Son 24 Saat" -> "-24h"
        "Son 7 Gün" -> "-7d"
        "Son 30 Gün" -> "-30d"
        "Son 90 Gün" -> "-90d"
        else -> "-30d" // Default
      }
      
      val statusResp = NextDnsNetworkClient.api.getAnalyticsStatus(key, profileId, devParam, fromParam)
      val devicesResp = NextDnsNetworkClient.api.getAnalyticsDevices(key, profileId, devParam, fromParam)
      val allowedDomainsResp = NextDnsNetworkClient.api.getAnalyticsDomains(key, profileId, devParam, fromParam, status = "default")
      val blockedDomainsResp = NextDnsNetworkClient.api.getAnalyticsDomains(key, profileId, devParam, fromParam, status = "blocked")
      val rootDomainsResp = NextDnsNetworkClient.api.getAnalyticsDomains(key, profileId, devParam, fromParam)
      val reasonsResp = NextDnsNetworkClient.api.getAnalyticsReasons(key, profileId, devParam, fromParam)
      val companiesResp = NextDnsNetworkClient.api.getAnalyticsDestinations(key, profileId, devParam, fromParam, type = "gafam")
      val destinationsResp = NextDnsNetworkClient.api.getAnalyticsDestinations(key, profileId, devParam, fromParam, type = "countries")
      val dnssecResp = NextDnsNetworkClient.api.getAnalyticsDnssec(key, profileId, devParam, fromParam)
      val encryptionResp = NextDnsNetworkClient.api.getAnalyticsEncryption(key, profileId, devParam, fromParam)

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
        encryptedDnsPercentage = if (encryptionResp.isSuccessful) encPct.toFloat() else _analytics.value.encryptedDnsPercentage,
        dnssecPercentage = if (dnssecResp.isSuccessful) dnssecPct.toFloat() else _analytics.value.dnssecPercentage,
        topCountries = topCountries
      )
      
      _analytics.value = updatedAnalytics

    } catch (e: Exception) {
      Log.e("NextDnsRepository", "Error fetching analytics: ${e.message}")
    }
  }
}
