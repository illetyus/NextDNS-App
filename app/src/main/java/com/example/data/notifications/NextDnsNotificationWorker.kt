package com.example.data.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.api.AllowDenyRuleDto
import com.example.data.api.NextDnsApiResponse
import com.example.data.api.NextDnsNetworkClient
import com.example.data.api.hasApiErrors
import com.example.data.local.NextDnsPreferences
import retrofit2.Response

class NextDnsNotificationWorker(
  appContext: Context,
  params: WorkerParameters
) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    val preferences = NotificationPreferences(applicationContext)
    val settings = runCatching {
      preferences.currentSettings()
    }.getOrElse {
      return Result.retry()
    }

    if (!settings.anyEnabled) return Result.success()

    val securePreferences = NextDnsPreferences.getInstance(applicationContext)
    val apiKey = securePreferences.apiKey
    val profileId = securePreferences.activeProfileId

    if (apiKey.isBlank() || profileId.isBlank()) {
      return Result.success()
    }

    NotificationCenter.createChannels(applicationContext)

    val baselineOnly = inputData.getBoolean(KEY_BASELINE_ONLY, false)
    if (baselineOnly) {
      if (!settings.configChangeAlertsEnabled) return Result.success()

      val snapshot = fetchConfigSnapshot(apiKey, profileId)
        ?: return Result.retry()

      preferences.establishConfigBaseline(
        profileId = profileId,
        digest = ConfigSnapshotHasher.digest(snapshot)
      )
      return Result.success()
    }

    var shouldRetry = false
    val now = System.currentTimeMillis()

    if (settings.configChangeAlertsEnabled) {
      val snapshot = fetchConfigSnapshot(apiKey, profileId)
      if (snapshot == null) {
        shouldRetry = true
      } else {
        handleConfigSnapshot(
          preferences = preferences,
          profileId = profileId,
          digest = ConfigSnapshotHasher.digest(snapshot),
          now = now
        )
      }
    }

    if (settings.dailySummaryEnabled) {
      val today = NotificationPolicy.localDayKey(now)
      val lastSummaryDay = runCatching {
        preferences.lastSummaryDay()
      }.getOrNull()

      if (lastSummaryDay != today) {
        val summary = fetchDailySummary(apiKey, profileId)
        if (summary == null) {
          shouldRetry = true
        } else {
          NotificationCenter.postDailySummary(
            context = applicationContext,
            totalQueries = summary.totalQueries,
            blockedQueries = summary.blockedQueries
          )

          // Whether system notifications are enabled or not, don't queue a
          // stale daily summary for later delivery.
          preferences.markSummaryDay(today)
        }
      }
    }

    return if (shouldRetry) Result.retry() else Result.success()
  }

  private suspend fun handleConfigSnapshot(
    preferences: NotificationPreferences,
    profileId: String,
    digest: String,
    now: Long
  ) {
    val state = preferences.configState(profileId)

    if (state.observedDigest == null) {
      preferences.establishConfigBaseline(profileId, digest)
      return
    }

    if (state.observedDigest != digest) {
      preferences.markConfigObserved(profileId, digest)
    }

    if (state.lastNotifiedDigest == digest) return

    if (!NotificationCenter.canPost(
        applicationContext,
        NotificationCenter.CHANNEL_CONFIG_CHANGES
      )
    ) {
      preferences.suppressConfigBacklog(profileId, digest)
      return
    }

    if (
      NotificationPolicy.shouldNotifyConfigChange(
        currentDigest = digest,
        lastNotifiedDigest = state.lastNotifiedDigest,
        lastNotifiedAt = state.lastNotifiedAt,
        now = now
      ) &&
      NotificationCenter.postConfigChanged(applicationContext)
    ) {
      preferences.markConfigNotified(profileId, digest, now)
    }
  }

  private suspend fun fetchConfigSnapshot(
    apiKey: String,
    profileId: String
  ): NotificationConfigSnapshot? = runCatching {
    val api = NextDnsNetworkClient.api

    val security = api.getSecurity(apiKey, profileId)
      .requireDataOrNull() ?: return null

    val privacy = api.getPrivacy(apiKey, profileId)
      .requireDataOrNull() ?: return null

    val parental = api.getParentalControl(apiKey, profileId)
      .requireDataOrNull() ?: return null

    val settings = api.getSettings(apiKey, profileId)
      .requireDataOrNull() ?: return null

    val denylist = fetchAllRules { cursor ->
      api.getDenylist(
        apiKey = apiKey,
        profileId = profileId,
        limit = 500,
        cursor = cursor
      )
    } ?: return null

    val allowlist = fetchAllRules { cursor ->
      api.getAllowlist(
        apiKey = apiKey,
        profileId = profileId,
        limit = 500,
        cursor = cursor
      )
    } ?: return null

    NotificationConfigSnapshot(
      security = security,
      privacy = privacy,
      parental = parental,
      settings = settings,
      denylist = denylist,
      allowlist = allowlist
    )
  }.getOrNull()

  private suspend fun fetchAllRules(
    request: suspend (cursor: String?) -> Response<NextDnsApiResponse<List<AllowDenyRuleDto>>>
  ): List<AllowDenyRuleDto>? {
    val items = mutableListOf<AllowDenyRuleDto>()
    val seenCursors = mutableSetOf<String>()
    var cursor: String? = null

    do {
      val response = runCatching {
        request(cursor)
      }.getOrNull() ?: return null

      val body = response.body() ?: return null
      if (!response.isSuccessful || body.hasApiErrors()) return null

      items += body.data.orEmpty()

      val nextCursor = body.meta?.pagination?.cursor
      cursor = if (!nextCursor.isNullOrBlank() && seenCursors.add(nextCursor)) {
        nextCursor
      } else {
        null
      }
    } while (cursor != null)

    return items.distinctBy { it.id }
  }

  private suspend fun fetchDailySummary(
    apiKey: String,
    profileId: String
  ): DailySummary? = runCatching {
    val response = NextDnsNetworkClient.api.getAnalyticsStatus(
      apiKey = apiKey,
      profileId = profileId,
      from = "-1d",
      to = "now"
    )

    val body = response.body() ?: return null
    if (!response.isSuccessful || body.hasApiErrors()) return null

    val items = body.data.orEmpty()
    DailySummary(
      totalQueries = items.sumOf { it.queries ?: 0L },
      blockedQueries = items
        .filter { it.status.equals("blocked", ignoreCase = true) }
        .sumOf { it.queries ?: 0L }
    )
  }.getOrNull()

  private fun <T> Response<NextDnsApiResponse<T>>.requireDataOrNull(): T? {
    val body = body() ?: return null
    if (!isSuccessful || body.hasApiErrors()) return null
    return body.data
  }

  private data class DailySummary(
    val totalQueries: Long,
    val blockedQueries: Long
  )

  companion object {
    const val KEY_BASELINE_ONLY = "baseline_only"
  }
}
