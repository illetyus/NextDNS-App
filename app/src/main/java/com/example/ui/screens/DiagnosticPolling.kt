package com.example.ui.screens

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

internal suspend fun Lifecycle.pollDiagnostics(
  intervalMs: Long = 15_000L,
  check: suspend () -> Unit
) {
  require(intervalMs > 0L)
  repeatOnLifecycle(Lifecycle.State.RESUMED) {
    while (currentCoroutineContext().isActive) {
      check()
      delay(intervalMs)
    }
  }
}
