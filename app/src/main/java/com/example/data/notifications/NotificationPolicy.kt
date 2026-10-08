package com.example.data.notifications

import java.security.MessageDigest
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object NotificationPolicy {
  const val PERIODIC_INTERVAL_MINUTES = 30L
  const val CHANGE_COOLDOWN_MS = 2L * 60L * 60L * 1000L

  fun shouldNotifyConfigChange(
    currentDigest: String,
    lastNotifiedDigest: String?,
    lastNotifiedAt: Long,
    now: Long
  ): Boolean {
    if (currentDigest == lastNotifiedDigest) return false
    if (lastNotifiedAt <= 0L) return true
    return now - lastNotifiedAt >= CHANGE_COOLDOWN_MS
  }

  fun localDayKey(
    epochMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault()
  ): String {
    val calendar = Calendar.getInstance(timeZone).apply {
      timeInMillis = epochMillis
    }
    return String.format(
      Locale.US,
      "%04d-%03d",
      calendar.get(Calendar.YEAR),
      calendar.get(Calendar.DAY_OF_YEAR)
    )
  }

  fun profileStorageSuffix(profileId: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
      .digest(profileId.toByteArray(Charsets.UTF_8))
    return digest.take(8).joinToString("") { "%02x".format(it) }
  }

  val periodicIntervalMillis: Long
    get() = TimeUnit.MINUTES.toMillis(PERIODIC_INTERVAL_MINUTES)
}
