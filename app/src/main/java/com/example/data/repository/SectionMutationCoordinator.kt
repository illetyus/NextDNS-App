package com.example.data.repository

import kotlinx.coroutines.sync.Mutex

internal class SectionMutationCoordinator {
  private val locks: Map<SyncSection, Mutex> =
    SyncSection.values().associateWith { Mutex() }

  fun tryEnter(section: SyncSection): Boolean =
    locks.getValue(section).tryLock()

  fun exit(section: SyncSection) {
    locks.getValue(section).unlock()
  }
}
