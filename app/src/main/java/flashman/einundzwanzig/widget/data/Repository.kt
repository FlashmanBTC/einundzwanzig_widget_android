package flashman.einundzwanzig.widget.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Last known values are shown (greyed) for at most this long. */
const val CACHE_MAX_AGE_MS = 24 * 60 * 60 * 1000L

val Context.store by preferencesDataStore(name = "einundzwanzig")

/** Lenient: an app update may add fields, old stored JSON must still load. */
val json = Json { ignoreUnknownKeys = true }

enum class State { OK, CACHED, FAIL }

/** One data source's value; [at] is when it was fetched (older than the snapshot if cached). */
@Serializable
data class Sourced<T>(val value: T? = null, val state: State = State.FAIL, val at: Long = 0)

/**
 * Raw values of all sources. Formatting happens per widget, because every widget
 * has its own currency, fee order and rows (see WidgetConfig).
 */
@Serializable
data class Snapshot(
    val updatedAt: Long,
    val height: Sourced<Long> = Sourced(),
    val fees: Sourced<Parsers.Fees> = Sourced(),
    val prices: Sourced<Map<String, Double>> = Sourced(),
    val hashrate: Sourced<Double> = Sourced(),
    val difficulty: Sourced<Parsers.Difficulty> = Sourced(),
)

private val SNAPSHOT = stringPreferencesKey("snapshot_v2")

object Repository {

    fun snapshot(prefs: Preferences): Snapshot? =
        prefs[SNAPSHOT]?.let { runCatching { json.decodeFromString<Snapshot>(it) }.getOrNull() }

    /** Fetches everything in parallel, falls back to cached values, stores and returns the new snapshot. */
    suspend fun refresh(context: Context, get: suspend (String) -> String? = Http::get): Snapshot {
        val prefs = context.store.data.first()
        val now = System.currentTimeMillis()
        val lastHeight = prefs.raw("height")?.let { Parsers.height(it) }

        // Each source returns its raw body once accepted, so the body itself can be cached
        val fresh = coroutineScope {
            val height = async { fetchFirst(Sources.height, get) { b -> b.takeIf { Parsers.height(it, lastHeight) != null } } }
            val fees = async { fetchFirst(Sources.fees, get) { b -> b.takeIf { Parsers.fees(it) != null } } }
            val price = async { fetchFirst(Sources.price, get) { b -> b.takeIf { Parsers.prices(it, now / 1000) != null } } }
            val hash = async { fetchFirst(Sources.hashrate, get) { b -> b.takeIf { Parsers.hashrate(it) != null } } }
            val diff = async { fetchFirst(Sources.difficulty, get) { b -> b.takeIf { Parsers.difficulty(it) != null } } }
            mapOf(
                "height" to height.await(), "fees" to fees.await(), "price" to price.await(),
                "hashrate" to hash.await(), "difficulty" to diff.await(),
            )
        }

        // Fresh body -> OK; otherwise the cached body if it is not too old -> CACHED; else FAIL
        fun <T> resolve(key: String, parse: (String) -> T?): Sourced<T> {
            fresh[key]?.let { body -> return Sourced(parse(body), State.OK, now) }
            val cached = prefs.raw(key)
            val at = prefs.ts(key)
            if (cached != null && now - at < CACHE_MAX_AGE_MS) {
                parse(cached)?.let { return Sourced(it, State.CACHED, at) }
            }
            return Sourced()
        }

        val snapshot = Snapshot(
            updatedAt = now,
            height = resolve("height") { Parsers.height(it) },
            fees = resolve("fees", Parsers::fees),
            prices = resolve("price") { Parsers.prices(it) },
            hashrate = resolve("hashrate", Parsers::hashrate),
            difficulty = resolve("difficulty", Parsers::difficulty),
        )

        context.store.edit { e ->
            fresh.forEach { (key, body) ->
                if (body != null) {
                    e[stringPreferencesKey("raw_$key")] = body
                    e[longPreferencesKey("ts_$key")] = now
                }
            }
            e[SNAPSHOT] = json.encodeToString(Snapshot.serializer(), snapshot)
            // Replaced by snapshot_v2 in 0.4.0
            e.remove(stringPreferencesKey("snapshot"))
        }
        return snapshot
    }

    private fun Preferences.raw(key: String) = this[stringPreferencesKey("raw_$key")]
    private fun Preferences.ts(key: String) = this[longPreferencesKey("ts_$key")] ?: 0L
}
