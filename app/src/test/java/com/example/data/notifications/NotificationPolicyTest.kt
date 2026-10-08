package com.example.data.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class NotificationPolicyTest {

  @Test
  fun configChange_sameDigestIsNeverNotifiedAgain() {
    assertFalse(
      NotificationPolicy.shouldNotifyConfigChange(
        currentDigest = "same",
        lastNotifiedDigest = "same",
        lastNotifiedAt = 0L,
        now = 10_000L
      )
    )
  }

  @Test
  fun configChange_newDigestRespectsCooldown() {
    val last = 1_000L

    assertFalse(
      NotificationPolicy.shouldNotifyConfigChange(
        currentDigest = "new",
        lastNotifiedDigest = "old",
        lastNotifiedAt = last,
        now = last + NotificationPolicy.CHANGE_COOLDOWN_MS - 1L
      )
    )

    assertTrue(
      NotificationPolicy.shouldNotifyConfigChange(
        currentDigest = "new",
        lastNotifiedDigest = "old",
        lastNotifiedAt = last,
        now = last + NotificationPolicy.CHANGE_COOLDOWN_MS
      )
    )
  }

  @Test
  fun configChange_firstNotificationDoesNotRequireCooldown() {
    assertTrue(
      NotificationPolicy.shouldNotifyConfigChange(
        currentDigest = "new",
        lastNotifiedDigest = "baseline",
        lastNotifiedAt = 0L,
        now = 5_000L
      )
    )
  }

  @Test
  fun localMutationSuppression_isBoundedAndExpires() {
    val now = 1_000_000L
    val until = NotificationPolicy.localMutationSuppressionUntil(now)

    assertEquals(
      NotificationPolicy.LOCAL_MUTATION_SUPPRESSION_MS,
      until - now
    )
    assertTrue(
      NotificationPolicy.isLocalMutationSuppressed(
        suppressUntil = until,
        now = until - 1L
      )
    )
    assertFalse(
      NotificationPolicy.isLocalMutationSuppressed(
        suppressUntil = until,
        now = until
      )
    )
  }

  @Test
  fun periodicBackgroundInterval_staysAboveWorkManagerMinimum() {
    assertTrue(NotificationPolicy.PERIODIC_INTERVAL_MINUTES >= 15L)
    assertEquals(30L, NotificationPolicy.PERIODIC_INTERVAL_MINUTES)
  }

  @Test
  fun dayKeyIsStableForExplicitTimezone() {
    val utc = TimeZone.getTimeZone("UTC")
    assertEquals(
      "2026-281",
      NotificationPolicy.localDayKey(
        epochMillis = 1_791_417_600_000L,
        timeZone = utc
      )
    )
  }

  @Test
  fun profileStorageSuffixDoesNotExposeProfileId() {
    val suffixA = NotificationPolicy.profileStorageSuffix("profile-secret-a")
    val suffixB = NotificationPolicy.profileStorageSuffix("profile-secret-b")

    assertNotEquals(suffixA, suffixB)
    assertFalse(suffixA.contains("profile"))
    assertEquals(16, suffixA.length)
  }
}
