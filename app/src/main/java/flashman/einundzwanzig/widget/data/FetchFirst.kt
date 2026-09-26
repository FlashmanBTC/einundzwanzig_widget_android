package flashman.einundzwanzig.widget.data

import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Start the next fallback if the current source has not answered after this time. */
const val HEDGE_MS = 2_000L

/**
 * Tries [urls] in order until one returns a body that [parse] accepts (non-null).
 *
 * The next URL starts as soon as the current one fails, or after [hedgeMs] if it is
 * merely slow, so a hanging primary costs ~2 s instead of a full timeout. The first
 * accepted result wins and all other requests are cancelled.
 *
 * [get] returns the response body, or null on any error (timeout, HTTP status, ...).
 */
suspend fun <T : Any> fetchFirst(
    urls: List<String>,
    get: suspend (String) -> String?,
    hedgeMs: Long = HEDGE_MS,
    parse: (String) -> T?,
): T? = coroutineScope {
    // Every finished request reports here; null = failed or rejected
    val results = Channel<T?>(Channel.UNLIMITED)
    var started = 0
    var finished = 0

    fun startNext() {
        val url = urls[started++]
        launch {
            val value = get(url)?.let { body -> runCatching { parse(body) }.getOrNull() }
            results.send(value)
        }
    }

    var winner: T? = null
    if (urls.isNotEmpty()) startNext()
    while (finished < started) {
        // While fallbacks remain, wait at most hedgeMs before starting the next one
        val msg = if (started < urls.size) withTimeoutOrNull(hedgeMs) { Result(results.receive()) }
                  else Result(results.receive())
        if (msg == null) {
            startNext()
            continue
        }
        finished++
        if (msg.value != null) {
            winner = msg.value
            break
        }
        if (started < urls.size) startNext()
    }
    coroutineContext.cancelChildren()
    winner
}

// Distinguishes "a request finished with null" from "the hedge timeout expired"
private class Result<T>(val value: T?)
