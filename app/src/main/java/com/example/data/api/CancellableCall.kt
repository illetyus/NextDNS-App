package com.example.data.api

import java.io.IOException
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response

private suspend fun Call.awaitResponse(): Response = suspendCancellableCoroutine { continuation ->
  continuation.invokeOnCancellation { cancel() }
  enqueue(object : Callback {
    override fun onFailure(call: Call, error: IOException) {
      continuation.resumeWithException(error)
    }
    override fun onResponse(call: Call, response: Response) {
      continuation.resume(response) { _, resource, _ -> resource.close() }
    }
  })
}

/** Cancellation remains attached until the response body is consumed and closed. */
internal suspend fun <T> Call.withCancellableResponse(block: suspend (Response) -> T): T =
  withContext(Dispatchers.IO) {
    coroutineScope {
      val call = this@withCancellableResponse
      val cancellation = launch(start = CoroutineStart.UNDISPATCHED) {
        try { awaitCancellation() } finally { call.cancel() }
      }
      try {
        call.awaitResponse().use { response ->
          currentCoroutineContext().ensureActive()
          val result = block(response)
          currentCoroutineContext().ensureActive()
          result
        }
      } catch (error: IOException) {
        currentCoroutineContext().ensureActive()
        throw error
      } finally {
        cancellation.cancel()
      }
    }
  }
