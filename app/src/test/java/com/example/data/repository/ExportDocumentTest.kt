package com.example.data.repository

import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ExportDocumentTest {
  @Test fun deniedProviderReturnsFailureWithoutWriting() = runBlocking {
    var wrote = false
    val result = exportToDocument({ throw SecurityException("Fixture denied") }) {
      wrote = true; Result.success(Unit)
    }
    assertTrue(result.isFailure)
    assertFalse(wrote)
  }

  @Test fun nullProviderStreamReturnsFailure() = runBlocking {
    assertTrue(exportToDocument({ null }) { fail("Must not write"); Result.success(Unit) }.isFailure)
  }

  @Test fun closeFailurePreventsSuccess() = runBlocking {
    val output = object : ByteArrayOutputStream() {
      override fun close() { throw IOException("Fixture close failure") }
    }
    assertTrue(exportToDocument({ output }) { it.write(1); Result.success(Unit) }.isFailure)
  }

  @Test fun cancellationPropagatesAndClosesProviderStream() = runBlocking {
    var closed = false
    val output = object : ByteArrayOutputStream() {
      override fun close() { closed = true; super.close() }
    }
    try {
      exportToDocument({ output }) { throw CancellationException("Fixture cancelled") }
      fail("Cancellation must propagate")
    } catch (_: CancellationException) { }
    assertTrue(closed)
  }

  @Test fun successIncludesProviderCloseAndExpectedBytes() = runBlocking {
    var closed = false
    val output = object : ByteArrayOutputStream() {
      override fun close() { closed = true; super.close() }
    }
    assertTrue(exportToDocument({ output }) { it.write("csv".toByteArray()); Result.success(Unit) }.isSuccess)
    assertTrue(closed)
    assertEquals("csv", output.toString("UTF-8"))
  }
}
