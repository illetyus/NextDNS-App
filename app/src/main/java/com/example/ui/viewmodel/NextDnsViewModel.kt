package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.ApiConnectionStatus
import com.example.data.repository.NextDnsRepository
import com.example.data.repository.ParentalFlag
import com.example.data.repository.PrivacyFlag
import com.example.data.repository.SecurityFlag
import com.example.data.repository.SettingsPerformanceFlag
import com.example.data.repository.SectionSyncState
import com.example.data.repository.SyncPolicy
import com.example.data.repository.SyncSection
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.util.UUID

enum class NavTab(val title: String, val iconName: String) {
  SETUP("Kurulum", "dns"),
  SECURITY("Güvenlik", "security"),
  PRIVACY("Gizlilik", "visibility_off"),
  PARENTAL("Ebeveyn Kontrolü", "family_restroom"),
  DENYLIST("Kara Liste", "block"),
  ALLOWLIST("Beyaz Liste", "check_circle"),
  ANALYTICS("Analizler", "insights"),
  LOGS("Günlükler", "format_list_bulleted"),
  SETTINGS("Ayarlar", "settings"),
  ACCOUNT("Hesap", "account_circle")
}

private fun NavTab.toSyncSection(): SyncSection? = when (this) {
  NavTab.SETUP -> SyncSection.SETUP
  NavTab.SECURITY -> SyncSection.SECURITY
  NavTab.PRIVACY -> SyncSection.PRIVACY
  NavTab.PARENTAL -> SyncSection.PARENTAL
  NavTab.DENYLIST -> SyncSection.DENYLIST
  NavTab.ALLOWLIST -> SyncSection.ALLOWLIST
  NavTab.SETTINGS -> SyncSection.SETTINGS
  NavTab.ACCOUNT -> SyncSection.ACCOUNT
  NavTab.ANALYTICS, NavTab.LOGS -> null
}

data class UiMessage(
  val id: Long = System.currentTimeMillis(),
  val text: String,
  val isError: Boolean = false
)

class NextDnsViewModel(
  private val repository: NextDnsRepository = NextDnsRepository()
) : ViewModel() {

  private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
  val themeMode = _themeMode.asStateFlow()
  
  fun setThemeMode(mode: ThemeMode) {
      _themeMode.value = mode
  }
  
  // Initialize theme mode from DataStore
  init {
      viewModelScope.launch {
          // This would ideally be passed in.
          // For now, let's keep it simple as requested
      }
  }

  val apiKey = repository.apiKey
  val apiStatus = repository.apiStatus
  val profiles = repository.profiles
  val activeProfileId = repository.activeProfileId
  val securitySettings = repository.securitySettings
  val privacySettings = repository.privacySettings
  val parentalControlSettings = repository.parentalControlSettings
  val denylist = repository.denylist
  val allowlist = repository.allowlist
  val logs = repository.logs
  val analytics = repository.analytics
  val analyticsLastSuccessAt = repository.analyticsLastSuccessAt
  val analyticsErrorMessage = repository.analyticsErrorMessage
  val allKnownDevices = repository.allKnownDevices
  val configSettings = repository.configSettings
  val testResult = repository.testResult
  val isLiveStreaming = repository.isLiveStreaming
  val isSyncing = repository.isSyncing
  val sectionSyncStates = repository.sectionSyncStates

  val availableBlocklistsCatalog = repository.availableBlocklistsCatalog
  val availableNativesCatalog = repository.availableNativesCatalog
  val availableParentalServicesCatalog = repository.availableParentalServicesCatalog
  val availableParentalCategoriesCatalog = repository.availableParentalCategoriesCatalog
  val availableTldsCatalog = repository.availableTldsCatalog
  val accountInfo = repository.accountInfo
  val profileSetup = repository.profileSetup

  private val _isAnalyticsLoading = MutableStateFlow(false)
  val isAnalyticsLoading = _isAnalyticsLoading.asStateFlow()

  private val _isGuestMode = MutableStateFlow(false)
  val isGuestMode = _isGuestMode.asStateFlow()

  private val _isInitializing = MutableStateFlow(true)
  val isInitializing = _isInitializing.asStateFlow()

  val isLoggedIn: StateFlow<Boolean?> = combine(apiStatus, _isGuestMode) { status, isGuest ->
    Log.d("AUTH_DEBUG", "status: $status, isGuest: $isGuest")
    when (status) {
      is ApiConnectionStatus.Connected -> {
        _isInitializing.value = false
        true
      }
      is ApiConnectionStatus.Disconnected -> {
        _isInitializing.value = false
        isGuest
      }
      else -> {
        // Connecting, Error, etc.
        null
      }
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val activeProfile: StateFlow<NextDnsProfile?> = combine(profiles, activeProfileId) { profs, id ->
    profs.find { it.id == id } ?: profs.firstOrNull()
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  private val _currentTab = MutableStateFlow(NavTab.SETUP)
  val currentTab = _currentTab.asStateFlow()

  val currentSectionSyncState: StateFlow<SectionSyncState?> =
    combine(currentTab, sectionSyncStates) { tab, states ->
      tab.toSyncSection()?.let { states[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  private var visibleSectionSyncJob: Job? = null
  private var foregroundProfileSyncJob: Job? = null

  private val _uiMessage = MutableStateFlow<UiMessage?>(null)
  val uiMessage = _uiMessage.asStateFlow()

  private val _isDiagnosticRunning = MutableStateFlow(false)
  val isDiagnosticRunning = _isDiagnosticRunning.asStateFlow()

  fun syncAllData() {
    viewModelScope.launch {
      repository.loadActiveProfileDataFromApi(apiKey.value, activeProfileId.value)

      val requiredSections = listOf(
        SyncSection.SECURITY,
        SyncSection.PRIVACY,
        SyncSection.PARENTAL,
        SyncSection.DENYLIST,
        SyncSection.ALLOWLIST,
        SyncSection.SETTINGS
      )
      val states = sectionSyncStates.value
      val failedSections = requiredSections.filter { section ->
        states[section]?.errorMessage != null ||
          states[section]?.lastSuccessAt == null
      }

      if (failedSections.isEmpty()) {
        showMessage("NextDNS profil ayarları sunucudan doğrulandı.")
      } else {
        showMessage(
          "Bazı NextDNS bölümleri doğrulanamadı; ekrandaki güncellik durumunu kontrol edin.",
          isError = true
        )
      }
    }
  }

  fun selectTab(tab: NavTab) {
    _currentTab.value = tab
  }

  fun startVisibleTabSync(tab: NavTab) {
    visibleSectionSyncJob?.cancel()
    val section = tab.toSyncSection() ?: return

    visibleSectionSyncJob = viewModelScope.launch {
      repository.refreshSection(section)

      val intervalMs: Long = when (tab) {
        NavTab.SETUP, NavTab.ACCOUNT -> return@launch
        else -> SyncPolicy.SECTION_POLL_MS
      }

      var nextDelayMs = intervalMs
      while (true) {
        delay(nextDelayMs)
        val success = repository.refreshSection(section)
        nextDelayMs = SyncPolicy.nextDelay(
          success = success,
          currentDelayMs = nextDelayMs,
          baseDelayMs = intervalMs,
          maxDelayMs = SyncPolicy.SECTION_MAX_BACKOFF_MS
        )
      }
    }
  }

  fun stopVisibleTabSync() {
    visibleSectionSyncJob?.cancel()
    visibleSectionSyncJob = null
  }

  fun startForegroundProfileSync() {
    foregroundProfileSyncJob?.cancel()
    foregroundProfileSyncJob = viewModelScope.launch {
      var nextDelayMs = SyncPolicy.PROFILE_POLL_MS

      repository.refreshProfilesFromApi()

      while (true) {
        delay(nextDelayMs)
        val success = repository.refreshProfilesFromApi()
        nextDelayMs = SyncPolicy.nextDelay(
          success = success,
          currentDelayMs = nextDelayMs,
          baseDelayMs = SyncPolicy.PROFILE_POLL_MS,
          maxDelayMs = SyncPolicy.PROFILE_MAX_BACKOFF_MS
        )
      }
    }
  }

  fun stopForegroundProfileSync() {
    foregroundProfileSyncJob?.cancel()
    foregroundProfileSyncJob = null
  }

  fun dismissMessage() {
    _uiMessage.value = null
  }

  fun showMessage(msg: String, isError: Boolean = false) {
    _uiMessage.value = UiMessage(text = msg, isError = isError)
  }

  private fun reportMutationResult(
    result: Result<Unit>,
    successMessage: String? = null
  ) {
    if (result.isSuccess) {
      if (!successMessage.isNullOrBlank()) showMessage(successMessage)
    } else {
      showMessage(
        result.exceptionOrNull()?.message ?: "NextDNS işlemi tamamlanamadı.",
        isError = true
      )
    }
  }

  fun saveApiKey(key: String) {
    loginWithApiKey(key)
  }

  fun enterGuestMode() {
    continueAsGuest()
  }

  fun continueAsGuest() {
    _isGuestMode.value = true
    showMessage("Demo / Misafir Modunda başlatıldı")
  }

  fun loginWithApiKey(key: String) {
    viewModelScope.launch {
      val result = repository.loginWithApiKey(key)
      if (result.isSuccess) {
        val count = result.getOrNull() ?: 1
        _isGuestMode.value = false
        showMessage("NextDNS API bağlantısı başarılı! ($count profil senkronize edildi)")
      } else {
        val err = result.exceptionOrNull()?.localizedMessage ?: "Bağlantı kurulamadı"
        showMessage(err, isError = true)
      }
    }
  }

  fun logout() {
    _isGuestMode.value = false
    repository.logout()
    showMessage("Oturum kapatıldı. API Giriş ekranına dönüldü.")
  }

  fun switchProfile(profileId: String) {
    repository.setActiveProfile(profileId)
    showMessage("Aktif Profil Değiştirildi: $profileId")
  }

  fun createProfile(name: String) {
    viewModelScope.launch {
      val result = repository.createProfileRemote(name)
      if (result.isSuccess) {
        showMessage("Yeni profil başarıyla oluşturuldu!")
      } else {
        showMessage(
          result.exceptionOrNull()?.message ?: "Profil oluşturulamadı",
          isError = true
        )
      }
    }
  }

  fun deleteProfile(profileId: String) {
    viewModelScope.launch {
      reportMutationResult(
        repository.deleteProfileRemote(profileId),
        successMessage = "Profil silindi"
      )
    }
  }

  fun renameProfile(newName: String) {
    activeProfile.value?.id?.let { pid ->
      viewModelScope.launch {
        reportMutationResult(
          repository.renameProfile(pid, newName),
          successMessage = "Profil ismi güncellendi: $newName"
        )
      }
    }
  }

  fun renameProfile(profileId: String, newName: String) {
    viewModelScope.launch {
      reportMutationResult(
        repository.renameProfile(profileId, newName),
        successMessage = "Profil ismi güncellendi"
      )
    }
  }

  // Security
  fun toggleSecurityFeature(feature: String, enabled: Boolean) {
    val flag = when (feature) {
      "threatIntelligenceFeeds" -> SecurityFlag.THREAT_INTELLIGENCE_FEEDS
      "aiThreatDetection" -> SecurityFlag.AI_THREAT_DETECTION
      "googleSafeBrowsing" -> SecurityFlag.GOOGLE_SAFE_BROWSING
      "cryptojacking" -> SecurityFlag.CRYPTOJACKING
      "dnsRebinding" -> SecurityFlag.DNS_REBINDING
      "idnHomographs" -> SecurityFlag.IDN_HOMOGRAPHS
      "typosquatting" -> SecurityFlag.TYPOSQUATTING
      "dga" -> SecurityFlag.DGA
      "nrd" -> SecurityFlag.NRD
      "ddns" -> SecurityFlag.DDNS
      "parkedDomains" -> SecurityFlag.PARKING
      "csam" -> SecurityFlag.CSAM
      else -> {
        showMessage("Bilinmeyen güvenlik ayarı: $feature", isError = true)
        return
      }
    }

    viewModelScope.launch {
      reportMutationResult(repository.setSecurityFlag(flag, enabled))
    }
  }

  fun addBlockedTld(tld: String) {
    viewModelScope.launch {
      reportMutationResult(
        repository.addBlockedTld(tld),
        successMessage = ".$tld uzantısı engellendi"
      )
    }
  }

  fun removeBlockedTld(tld: String) {
    viewModelScope.launch {
      reportMutationResult(
        repository.removeBlockedTld(tld),
        successMessage = ".$tld engeli kaldırıldı"
      )
    }
  }

  // Privacy
  fun toggleBlocklist(blocklistId: String) {
    viewModelScope.launch {
      reportMutationResult(
        repository.toggleBlocklist(blocklistId),
        successMessage = "Engelleme listesi güncellendi"
      )
    }
  }

  fun toggleNativeTracking(nativeId: String) {
    viewModelScope.launch {
      reportMutationResult(
        repository.toggleNativeTracking(nativeId),
        successMessage = "Yerel izleme koruması güncellendi"
      )
    }
  }

  fun toggleDisguisedTrackers(enabled: Boolean) {
    viewModelScope.launch {
      reportMutationResult(
        repository.setPrivacyFlag(PrivacyFlag.DISGUISED_TRACKERS, enabled)
      )
    }
  }

  fun toggleAllowAffiliates(enabled: Boolean) {
    viewModelScope.launch {
      reportMutationResult(
        repository.setPrivacyFlag(PrivacyFlag.ALLOW_AFFILIATE_LINKS, enabled)
      )
    }
  }

  // Parental Control
  fun toggleParentalService(serviceId: String) {
    viewModelScope.launch {
      reportMutationResult(
        repository.toggleParentalService(serviceId),
        successMessage = "Ebeveyn kontrolü kuralı güncellendi"
      )
    }
  }

  fun toggleParentalCategory(categoryId: String) {
    viewModelScope.launch {
      reportMutationResult(
        repository.toggleParentalCategory(categoryId),
        successMessage = "Kategori engeli güncellendi"
      )
    }
  }

  fun setSafeSearch(enabled: Boolean) {
    viewModelScope.launch {
      reportMutationResult(
        repository.setParentalFlag(ParentalFlag.SAFE_SEARCH, enabled)
      )
    }
  }

  fun setYoutubeRestricted(enabled: Boolean) {
    viewModelScope.launch {
      reportMutationResult(
        repository.setParentalFlag(ParentalFlag.YOUTUBE_RESTRICTED_MODE, enabled)
      )
    }
  }

  fun setBlockBypass(enabled: Boolean) {
    viewModelScope.launch {
      reportMutationResult(
        repository.setParentalFlag(ParentalFlag.BLOCK_BYPASS, enabled)
      )
    }
  }

  // Denylist
  fun addToDenylist(domain: String) {
    viewModelScope.launch {
      reportMutationResult(
        repository.addToDenylist(domain),
        successMessage = "$domain kara listeye eklendi"
      )
    }
  }

  fun removeFromDenylist(domain: String) {
    viewModelScope.launch {
      reportMutationResult(
        repository.removeFromDenylist(domain),
        successMessage = "$domain kara listeden kaldırıldı"
      )
    }
  }

  fun toggleDenylistItem(domain: String) {
    viewModelScope.launch {
      reportMutationResult(repository.toggleDenylistItem(domain))
    }
  }

  // Allowlist
  fun addToAllowlist(domain: String) {
    viewModelScope.launch {
      reportMutationResult(
        repository.addToAllowlist(domain),
        successMessage = "$domain beyaz listeye eklendi"
      )
    }
  }

  fun removeFromAllowlist(domain: String) {
    viewModelScope.launch {
      reportMutationResult(
        repository.removeFromAllowlist(domain),
        successMessage = "$domain beyaz listeden kaldırıldı"
      )
    }
  }

  fun toggleAllowlistItem(domain: String) {
    viewModelScope.launch {
      reportMutationResult(repository.toggleAllowlistItem(domain))
    }
  }

  // Logs & Live Stream
  fun toggleLiveStream() {
    val current = isLiveStreaming.value
    val newState = !current
    repository.setLiveStreaming(newState)
  }

  fun refreshLogs(showToast: Boolean = true) {
    viewModelScope.launch {
      val success = repository.refreshLogsFromApi()
      if (showToast) {
        if (success) {
          showMessage("Günlük kayıtları yenilendi")
        } else {
          showMessage("Günlük kayıtları yenilenemedi.", isError = true)
        }
      }
    }
  }

  fun refreshAnalytics(device: String?, time: String?) {
    viewModelScope.launch {
      _isAnalyticsLoading.value = true
      try {
        repository.fetchAnalytics(apiKey.value, activeProfileId.value, device, time)
      } finally {
        _isAnalyticsLoading.value = false
      }
    }
  }

  fun startAnalyticsPolling() {
      repository.startAnalyticsPolling()
  }

  fun stopAnalyticsPolling() {
      repository.stopAnalyticsPolling()
  }

  fun startLogsStream() {
      repository.startLogsStream()
  }

  fun stopLogsStream() {
      repository.stopLogsStream()
  }

  // Diagnostics
  fun runDiagnostic(showToast: Boolean = false) {
    viewModelScope.launch {
      _isDiagnosticRunning.value = true
      val activePid = activeProfile.value?.id
      val res = repository.runDiagnosticTest(activePid)
      _isDiagnosticRunning.value = false
      if (showToast) {
        val currentPid = activePid ?: ""
        val isUsingNextDns = res.status.equals("ok", ignoreCase = true) || res.status.equals("using-nextdns", ignoreCase = true)
        val msg = if (isUsingNextDns) {
          val details = buildList {
            if (res.latencyMs > 0) add("${res.latencyMs} ms")
            res.protocol.takeIf { it.isNotBlank() }?.let(::add)
          }.joinToString(" • ")
          if (details.isBlank()) {
            "NextDNS koruması bu profille aktif."
          } else {
            "NextDNS koruması bu profille aktif ($details)."
          }
        } else {
          "Bağlantı kontrol edildi: Bu cihaz şu anda NextDNS kullanmıyor."
        }
        showMessage(msg)
      }
    }
  }

  fun linkIpAddress(profileId: String) {
    viewModelScope.launch {
      val success = repository.linkCurrentIp(profileId)
      if (success) {
        showMessage("IP adresi başarıyla profile bağlandı.")
      } else {
        showMessage("IP adresi profile bağlanamadı.", isError = true)
      }
    }
  }

  // Settings
  fun toggleLogsEnabled(enabled: Boolean) {
    viewModelScope.launch {
      reportMutationResult(repository.setLogsEnabled(enabled))
    }
  }

  fun toggleLogClientIps(enabled: Boolean) {
    viewModelScope.launch {
      reportMutationResult(repository.setLogClientIps(enabled))
    }
  }

  fun toggleLogDomains(enabled: Boolean) {
    viewModelScope.launch {
      reportMutationResult(repository.setLogDomains(enabled))
    }
  }

  fun setLogRetention(retention: String) {
    viewModelScope.launch {
      reportMutationResult(
        repository.setLogRetention(retention),
        successMessage = "Saklama süresi: $retention"
      )
    }
  }

  fun setLogStorageLocation(location: String) {
    viewModelScope.launch {
      reportMutationResult(
        repository.setLogStorageLocation(location),
        successMessage = "Depolama konumu: $location"
      )
    }
  }

  suspend fun exportLogs(outputStream: OutputStream): Result<Unit> {
    val result = repository.exportLogs(outputStream)
    reportMutationResult(
      result,
      successMessage = "Günlükler CSV olarak kaydedildi"
    )
    return result
  }

  fun clearLogs() {
    viewModelScope.launch {
      reportMutationResult(
        repository.clearLogs(),
        successMessage = "Tüm günlükler temizlendi"
      )
    }
  }

  fun toggleBlockPage(enabled: Boolean) {
    viewModelScope.launch {
      reportMutationResult(repository.setBlockPage(enabled))
    }
  }

  fun toggleEdns(enabled: Boolean) {
    viewModelScope.launch {
      reportMutationResult(
        repository.setPerformanceFlag(SettingsPerformanceFlag.ECS, enabled)
      )
    }
  }

  fun toggleCacheBoost(enabled: Boolean) {
    viewModelScope.launch {
      reportMutationResult(
        repository.setPerformanceFlag(SettingsPerformanceFlag.CACHE_BOOST, enabled)
      )
    }
  }

  fun toggleCnameFlattening(enabled: Boolean) {
    viewModelScope.launch {
      reportMutationResult(
        repository.setPerformanceFlag(SettingsPerformanceFlag.CNAME_FLATTENING, enabled)
      )
    }
  }

  fun toggleWeb3(enabled: Boolean) {
    viewModelScope.launch {
      reportMutationResult(repository.setWeb3(enabled))
    }
  }

  override fun onCleared() {
    stopVisibleTabSync()
    stopForegroundProfileSync()
    super.onCleared()
  }
}
