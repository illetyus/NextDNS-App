package com.example.data.repository

object SyncPolicy {
  const val SECTION_POLL_MS = 30_000L
  const val SECTION_MAX_BACKOFF_MS = 120_000L
  const val PROFILE_POLL_MS = 60_000L
  const val PROFILE_MAX_BACKOFF_MS = 300_000L

  fun nextDelay(
    success: Boolean,
    currentDelayMs: Long,
    baseDelayMs: Long,
    maxDelayMs: Long
  ): Long = if (success) {
    baseDelayMs
  } else {
    (currentDelayMs * 2).coerceAtMost(maxDelayMs)
  }
}
