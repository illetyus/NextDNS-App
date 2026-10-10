package com.example.ui.screens

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiagnosticPollingTest {
  @Test fun pauseStopsPollingAndCancelsInFlightCheckThenResumeStartsOneLoop() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    try {
      val owner = object : LifecycleOwner {
        override val lifecycle = LifecycleRegistry.createUnsafe(this)
      }
      var checks = 0
      var cancelled = 0
      var block = false
      owner.lifecycle.currentState = Lifecycle.State.CREATED
      val polling = launch {
        owner.lifecycle.pollDiagnostics {
          checks++
          if (block) try { awaitCancellation() } finally { cancelled++ }
        }
      }
      runCurrent()
      assertEquals(0, checks)
      owner.lifecycle.currentState = Lifecycle.State.RESUMED
      runCurrent()
      assertEquals(1, checks)
      advanceTimeBy(15_000); runCurrent()
      assertEquals(2, checks)
      block = true
      advanceTimeBy(15_000); runCurrent()
      assertEquals(3, checks)
      owner.lifecycle.currentState = Lifecycle.State.STARTED
      runCurrent()
      assertEquals(1, cancelled)
      advanceTimeBy(60_000); runCurrent()
      assertEquals(3, checks)
      block = false
      owner.lifecycle.currentState = Lifecycle.State.RESUMED
      runCurrent()
      assertEquals(4, checks)
      owner.lifecycle.currentState = Lifecycle.State.DESTROYED
      runCurrent()
      assertTrue(polling.isCompleted)
    } finally { Dispatchers.resetMain() }
  }
}
