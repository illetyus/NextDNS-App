package com.example.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SectionMutationCoordinatorTest {

  @Test
  fun sameSection_allowsOnlyOneActiveWriter() {
    val coordinator = SectionMutationCoordinator()

    assertTrue(coordinator.tryEnter(SyncSection.SECURITY))
    assertFalse(coordinator.tryEnter(SyncSection.SECURITY))

    coordinator.exit(SyncSection.SECURITY)

    assertTrue(coordinator.tryEnter(SyncSection.SECURITY))
    coordinator.exit(SyncSection.SECURITY)
  }

  @Test
  fun differentSections_canMutateIndependently() {
    val coordinator = SectionMutationCoordinator()

    assertTrue(coordinator.tryEnter(SyncSection.SECURITY))
    assertTrue(coordinator.tryEnter(SyncSection.PRIVACY))

    coordinator.exit(SyncSection.SECURITY)
    coordinator.exit(SyncSection.PRIVACY)
  }
}
