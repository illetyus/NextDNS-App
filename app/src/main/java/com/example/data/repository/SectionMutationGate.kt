package com.example.data.repository

import kotlinx.coroutines.sync.Mutex

internal class SectionMutationGate {
  private val mutexes: Map<SyncSection, Mutex> =
    SyncSection.entries.associateWith { Mutex() }

  fun tryAcquire(section: SyncSection): Boolean =
    mutexes.getValue(section).tryLock()

  fun release(section: SyncSection) {
    mutexes.getValue(section).unlock()
  }
}
