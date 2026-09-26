package flashman.einundzwanzig.widget.data

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/** Timeout per request, same as the iOS widget. */
const val TIMEOUT_SEC = 6L

object Http {
    private val client = OkHttpClient.Builder()
        .callTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
        .build()

    /** Body of a 2xx response, or null on any error. Cancelling the coroutine cancels the call. */
    suspend fun get(url: String): String? = suspendCancellableCoroutine { cont ->
        val call = client.newCall(Request.Builder().url(url).build())
        cont.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) cont.resume(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.use { if (it.isSuccessful) runCatching { it.body.string() }.getOrNull() else null }
                if (cont.isActive) cont.resume(body)
            }
        })
    }
}
