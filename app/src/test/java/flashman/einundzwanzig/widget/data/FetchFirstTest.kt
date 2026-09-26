package flashman.einundzwanzig.widget.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Runs on virtual time: delay() and the hedge timeout cost no real time
class FetchFirstTest {

    private val urls = listOf("primary", "fallback1", "fallback2")

    /** Fake network: each URL answers after a delay with a body, or null for an error. */
    private fun net(vararg answers: Pair<String, Pair<Long, String?>>): Pair<suspend (String) -> String?, MutableList<String>> {
        val calls = mutableListOf<String>()
        val map = answers.toMap()
        val get: suspend (String) -> String? = { url ->
            calls += url
            val (ms, body) = map.getValue(url)
            delay(ms)
            body
        }
        return get to calls
    }

    @Test fun `primary answers - no fallback is started`() = runTest {
        val (get, calls) = net("primary" to (100L to "p"), "fallback1" to (100L to "f1"), "fallback2" to (100L to "f2"))
        assertEquals("p", fetchFirst(urls, get) { it })
        assertEquals(listOf("primary"), calls)
    }

    @Test fun `primary fails - fallback starts at once`() = runTest {
        val (get, calls) = net("primary" to (100L to null), "fallback1" to (100L to "f1"), "fallback2" to (100L to "f2"))
        assertEquals("f1", fetchFirst(urls, get) { it })
        assertEquals(listOf("primary", "fallback1"), calls)
        assertEquals(200L, currentTime)
    }

    @Test fun `primary hangs - fallback starts after the hedge delay`() = runTest {
        val (get, _) = net("primary" to (6_000L to "p"), "fallback1" to (300L to "f1"), "fallback2" to (300L to "f2"))
        assertEquals("f1", fetchFirst(urls, get) { it })
        assertEquals(HEDGE_MS + 300, currentTime)
    }

    @Test fun `slow primary still wins if it answers first`() = runTest {
        val (get, _) = net("primary" to (2_500L to "p"), "fallback1" to (1_000L to "f1"), "fallback2" to (1_000L to "f2"))
        assertEquals("p", fetchFirst(urls, get) { it })
    }

    @Test fun `rejected body counts as failure`() = runTest {
        val (get, _) = net("primary" to (100L to "stale"), "fallback1" to (100L to "good"), "fallback2" to (100L to "x"))
        assertEquals("good", fetchFirst(urls, get) { it.takeIf { b -> b != "stale" } })
    }

    @Test fun `parser exception counts as failure`() = runTest {
        val (get, _) = net("primary" to (100L to "boom"), "fallback1" to (100L to "ok"), "fallback2" to (100L to "x"))
        assertEquals("ok", fetchFirst(urls, get) { if (it == "boom") error("bad json") else it })
    }

    @Test fun `all fail - null`() = runTest {
        val (get, calls) = net("primary" to (100L to null), "fallback1" to (100L to null), "fallback2" to (100L to null))
        assertNull(fetchFirst(urls, get) { it })
        assertEquals(urls, calls)
    }

    @Test fun `empty list - null`() = runTest {
        assertNull(fetchFirst(emptyList(), { "x" }) { it })
    }
}
