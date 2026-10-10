package com.example.data.repository

import java.io.IOException
import java.io.OutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Success includes opening, writing, flushing and closing the provider's stream. */
internal suspend fun exportToDocument(
  openOutput: () -> OutputStream?,
  write: suspend (OutputStream) -> Result<Unit>
): Result<Unit> = withContext(Dispatchers.IO) {
  try {
    val output = openOutput() ?: return@withContext Result.failure(IOException("No document output stream"))
    output.use { write(it) }
  } catch (cancel: CancellationException) {
    throw cancel
  } catch (error: Exception) {
    Result.failure(error)
  }
}
