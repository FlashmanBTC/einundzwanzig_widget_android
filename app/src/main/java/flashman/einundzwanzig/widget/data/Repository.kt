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

// Phase 4 turns these into user settings
object Settings {
    const val currency = "EUR"
    const val feesHighToLow = false
}

enum class State { OK, CACHED, FAIL }

@Serializable
data class Item(val text: String, val state: State)

/** Everything the widget shows, ready to render. */
@Serializable
data class Snapshot(
    val updatedAt: Long,
    val currency: String,
    val height: Item,
    val fees: Item,
    val moscow: Item,
    val price: Item,
    val supply: Item,
    val hashrate: Item,
    val difficulty: Item,
    /** Age of the oldest cached value on display, null if everything is fresh. */
    val oldestCacheAt: Long?,
) {
    val items get() = listOf(height, fees, moscow, price, supply, hashrate, difficulty)
}

private val SNAPSHOT = stringPreferencesKey("snapshot")

object Repository {

    /** Last snapshot, or null before the first refresh. */
    suspend fun snapshot(context: Context): Snapshot? =
        context.store.data.first()[SNAPSHOT]?.let { runCatching { Json.decodeFromString<Snapshot>(it) }.getOrNull() }

    /** Fetches everything in parallel, falls back to cached values, stores and returns the new snapshot. */
    suspend fun refresh(context: Context, get: suspend (String) -> String? = Http::get): Snapshot {
        val prefs = context.store.data.first()
        val now = System.currentTimeMillis()
        val currency = Settings.currency
        val lastHeight = prefs.raw("height")?.let { Parsers.height(it) }

        // Each source returns its raw body once accepted, so the body itself can be cached
        val fresh = coroutineScope {
            val height = async { fetchFirst(Sources.height, get) { b -> b.takeIf { Parsers.height(it, lastHeight) != null } } }
            val fees = async { fetchFirst(Sources.fees, get) { b -> b.takeIf { Parsers.fees(it) != null } } }
            val price = async { fetchFirst(Sources.price, get) { b -> b.takeIf { Parsers.price(it, currency, now / 1000) != null } } }
            val hash = async { fetchFirst(Sources.hashrate, get) { b -> b.takeIf { Parsers.hashrate(it) != null } } }
            val diff = async { fetchFirst(Sources.difficulty, get) { b -> b.takeIf { Parsers.difficulty(it) != null } } }
            mapOf(
                "height" to height.await(), "fees" to fees.await(), "price" to price.await(),
                "hashrate" to hash.await(), "difficulty" to diff.await(),
            )
        }

        // Fresh body -> OK; otherwise the cached body if it is not too old -> CACHED; else FAIL
        val resolved = fresh.mapValues { (key, body) ->
            when {
                body != null -> Resolved(body, State.OK, now)
                prefs.raw(key) != null && now - prefs.ts(key) < CACHE_MAX_AGE_MS ->
                    Resolved(prefs.raw(key)!!, State.CACHED, prefs.ts(key))
                else -> Resolved(null, State.FAIL, 0)
            }
        }

        val h = resolved.getValue("height")
        val f = resolved.getValue("fees")
        val p = resolved.getValue("price")
        val hr = resolved.getValue("hashrate")
        val d = resolved.getValue("difficulty")

        val heightValue = h.body?.let { Parsers.height(it) }
        val priceValue = p.body?.let { Parsers.price(it, currency) }

        val snapshot = Snapshot(
            updatedAt = now,
            currency = currency,
            height = item(h, heightValue?.let(Format::height)),
            fees = item(f, f.body?.let(Parsers::fees)?.let { Format.fees(it, Settings.feesHighToLow) }),
            moscow = item(p, priceValue?.let(Format::moscowTime)),
            price = item(p, priceValue?.let(Format::price)),
            supply = item(h, heightValue?.let(Format::supply)),
            hashrate = item(hr, hr.body?.let(Parsers::hashrate)?.let(Format::hashrate)),
            difficulty = item(d, d.body?.let(Parsers::difficulty)?.let(Format::difficulty)),
            oldestCacheAt = resolved.values.filter { it.state == State.CACHED }.minOfOrNull { it.at },
        )

        context.store.edit { e ->
            fresh.forEach { (key, body) ->
                if (body != null) {
                    e[stringPreferencesKey("raw_$key")] = body
                    e[longPreferencesKey("ts_$key")] = now
                }
            }
            e[SNAPSHOT] = Json.encodeToString(Snapshot.serializer(), snapshot)
        }
        return snapshot
    }

    private class Resolved(val body: String?, val state: State, val at: Long)

    private fun item(r: Resolved, text: String?) =
        if (text == null) Item("⚠️ n/a", State.FAIL) else Item(text, r.state)

    private fun Preferences.raw(key: String) = this[stringPreferencesKey("raw_$key")]
    private fun Preferences.ts(key: String) = this[longPreferencesKey("ts_$key")] ?: 0L
}
