package com.example.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SectionMutationGateTest {

  @Test
  fun sameSection_allowsOnlyOneActiveMutation() {
    val gate = SectionMutationGate()

    assertTrue(gate.tryAcquire(SyncSection.SECURITY))
    assertFalse(gate.tryAcquire(SyncSection.SECURITY))

    gate.release(SyncSection.SECURITY)

    assertTrue(gate.tryAcquire(SyncSection.SECURITY))
    gate.release(SyncSection.SECURITY)
  }

  @Test
  fun differentSections_canMutateIndependently() {
    val gate = SectionMutationGate()

    assertTrue(gate.tryAcquire(SyncSection.SECURITY))
    assertTrue(gate.tryAcquire(SyncSection.PRIVACY))

    gate.release(SyncSection.PRIVACY)
    gate.release(SyncSection.SECURITY)
  }
}
