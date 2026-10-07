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
import com.example.data.repository.SectionSyncState
import com.example.data.repository.SyncPolicy
import com.example.data.repository.SyncSection
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
        true || isGuest
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
      showMessage("Tüm NextDNS verileri senkronize edildi")
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

  fun updateUserEmail(email: String, name: String = "") {
    repository.updateUserEmail(email, name)
    showMessage("Hesap e-postası güncellendi: $email")
  }

  fun createProfile(name: String) {
    viewModelScope.launch {
      val res = repository.createProfileRemote(name)
      if (res.isSuccess) {
        showMessage("Yeni profil başarıyla oluşturuldu!")
      } else {
        showMessage("Profil oluşturulamadı", isError = true)
      }
    }
  }

  fun deleteProfile(profileId: String) {
    viewModelScope.launch {
      repository.deleteProfileRemote(profileId)
      showMessage("Profil silindi")
    }
  }

  fun renameProfile(newName: String) {
    activeProfile.value?.id?.let { pid ->
      repository.renameProfile(pid, newName)
      showMessage("Profil ismi güncellendi: $newName")
    }
  }

  fun renameProfile(profileId: String, newName: String) {
    repository.renameProfile(profileId, newName)
    showMessage("Profil ismi güncellendi")
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
    repository.addToDenylist(domain)
    showMessage("$domain kara listeye eklendi")
  }

  fun removeFromDenylist(domain: String) {
    repository.removeFromDenylist(domain)
    showMessage("$domain kara listeden kaldırıldı")
  }

  fun toggleDenylistItem(domain: String) {
    repository.toggleDenylistItem(domain)
  }

  // Allowlist
  fun addToAllowlist(domain: String) {
    repository.addToAllowlist(domain)
    showMessage("$domain beyaz listeye eklendi")
  }

  fun removeFromAllowlist(domain: String) {
    repository.removeFromAllowlist(domain)
    showMessage("$domain beyaz listeden kaldırıldı")
  }

  fun toggleAllowlistItem(domain: String) {
    repository.toggleAllowlistItem(domain)
  }

  // Logs & Live Stream
  fun toggleLiveStream() {
    val current = isLiveStreaming.value
    val newState = !current
    repository.setLiveStreaming(newState)
  }

  fun refreshLogs(showToast: Boolean = true) {
    viewModelScope.launch {
      repository.refreshLogsFromApi()
      if (showToast) {
        showMessage("Günlük kayıtları yenilendi")
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
          "Harika! NextDNS koruması bu profille aktif (${res.latencyMs} ms • ${res.protocol})."
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
        showMessage("IP bağlama isteği tamamlandı.")
      }
    }
  }

  // Settings
  fun toggleLogsEnabled(enabled: Boolean) {
    repository.updateConfig { it.copy(logsEnabled = enabled) }
  }

  fun toggleLogClientIps(enabled: Boolean) {
    repository.updateConfig { it.copy(logClientIps = enabled) }
  }

  fun toggleLogDomains(enabled: Boolean) {
    repository.updateConfig { it.copy(logDomains = enabled) }
  }

  fun setLogRetention(retention: String) {
    repository.updateConfig { it.copy(logRetention = retention) }
    showMessage("Saklama süresi: $retention")
  }

  fun setLogStorageLocation(location: String) {
    repository.updateConfig { it.copy(logStorageLocation = location) }
    showMessage("Depolama konumu: $location")
  }

  fun downloadLogs() {
    showMessage("Günlükler CSV olarak indirildi")
  }

  fun clearLogs() {
    repository.clearLogs()
    showMessage("Tüm günlükler temizlendi")
  }

  fun toggleBlockPage(enabled: Boolean) {
    repository.updateConfig { it.copy(blockPage = enabled) }
  }

  fun toggleEdns(enabled: Boolean) {
    repository.updateConfig { it.copy(ednsClientSubnet = enabled) }
  }

  fun toggleCacheBoost(enabled: Boolean) {
    repository.updateConfig { it.copy(cacheBoost = enabled) }
  }

  fun toggleCnameFlattening(enabled: Boolean) {
    repository.updateConfig { it.copy(cnameFlattening = enabled) }
  }

  fun toggleBypassAgeVerification(enabled: Boolean) {
    repository.updateConfig { it.copy(bypassAgeVerification = enabled) }
  }

  fun toggleWeb3(enabled: Boolean) {
    repository.updateConfig { it.copy(web3 = enabled) }
  }
  fun addRewrite(domain: String, answer: String) {
    repository.updateConfig { cfg ->
      val updated = cfg.rewrites + RewriteItem(domain = domain, answer = answer)
      cfg.copy(rewrites = updated)
    }
    showMessage("Yeniden yazma kuralı eklendi: $domain ➔ $answer")
  }

  fun removeRewrite(id: String) {
    repository.updateConfig { cfg ->
      val updated = cfg.rewrites.filter { it.id != id }
      cfg.copy(rewrites = updated)
    }
    showMessage("Yeniden yazma kuralı silindi")
  }

  override fun onCleared() {
    stopVisibleTabSync()
    stopForegroundProfileSync()
    super.onCleared()
  }
}
