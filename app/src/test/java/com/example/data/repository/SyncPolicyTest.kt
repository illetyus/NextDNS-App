package com.example.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncPolicyTest {

  @Test
  fun sectionPolling_resetsAfterSuccess() {
    val next = SyncPolicy.nextDelay(
      success = true,
      currentDelayMs = 120_000L,
      baseDelayMs = SyncPolicy.SECTION_POLL_MS,
      maxDelayMs = SyncPolicy.SECTION_MAX_BACKOFF_MS
    )

    assertEquals(30_000L, next)
  }

  @Test
  fun sectionPolling_backsOffAndCapsAtTwoMinutes() {
    val firstFailure = SyncPolicy.nextDelay(
      success = false,
      currentDelayMs = 30_000L,
      baseDelayMs = SyncPolicy.SECTION_POLL_MS,
      maxDelayMs = SyncPolicy.SECTION_MAX_BACKOFF_MS
    )
    val secondFailure = SyncPolicy.nextDelay(
      success = false,
      currentDelayMs = firstFailure,
      baseDelayMs = SyncPolicy.SECTION_POLL_MS,
      maxDelayMs = SyncPolicy.SECTION_MAX_BACKOFF_MS
    )
    val cappedFailure = SyncPolicy.nextDelay(
      success = false,
      currentDelayMs = secondFailure,
      baseDelayMs = SyncPolicy.SECTION_POLL_MS,
      maxDelayMs = SyncPolicy.SECTION_MAX_BACKOFF_MS
    )

    assertEquals(60_000L, firstFailure)
    assertEquals(120_000L, secondFailure)
    assertEquals(120_000L, cappedFailure)
  }

  @Test
  fun analyticsPolling_backsOffAndResets() {
    val firstFailure = SyncPolicy.nextDelay(
      success = false,
      currentDelayMs = SyncPolicy.ANALYTICS_POLL_MS,
      baseDelayMs = SyncPolicy.ANALYTICS_POLL_MS,
      maxDelayMs = SyncPolicy.ANALYTICS_MAX_BACKOFF_MS
    )
    val secondFailure = SyncPolicy.nextDelay(
      success = false,
      currentDelayMs = firstFailure,
      baseDelayMs = SyncPolicy.ANALYTICS_POLL_MS,
      maxDelayMs = SyncPolicy.ANALYTICS_MAX_BACKOFF_MS
    )
    val reset = SyncPolicy.nextDelay(
      success = true,
      currentDelayMs = secondFailure,
      baseDelayMs = SyncPolicy.ANALYTICS_POLL_MS,
      maxDelayMs = SyncPolicy.ANALYTICS_MAX_BACKOFF_MS
    )

    assertEquals(60_000L, firstFailure)
    assertEquals(120_000L, secondFailure)
    assertEquals(30_000L, reset)
  }

  @Test
  fun profilePolling_backsOffAndCapsAtFiveMinutes() {
    var delayMs = SyncPolicy.PROFILE_POLL_MS
    repeat(4) {
      delayMs = SyncPolicy.nextDelay(
        success = false,
        currentDelayMs = delayMs,
        baseDelayMs = SyncPolicy.PROFILE_POLL_MS,
        maxDelayMs = SyncPolicy.PROFILE_MAX_BACKOFF_MS
      )
    }

    assertEquals(300_000L, delayMs)
  }
}
