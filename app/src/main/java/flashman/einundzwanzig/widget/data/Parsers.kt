package flashman.einundzwanzig.widget.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.math.floor
import kotlin.math.roundToLong

/** Prices older than this are stale (e.g. a node that is still syncing). */
const val MAX_PRICE_AGE_SEC = 60 * 60L

/** A height this far below the last one seen comes from a lagging node. */
const val MAX_HEIGHT_DROP = 2

/** Currencies offered in the settings - both price sources deliver all of them. */
val CURRENCIES = listOf("EUR", "USD", "CHF", "GBP", "CAD", "AUD", "JPY")

// Pure parsing and formatting - no Android dependencies, covered by unit tests.
// Each parser returns null when the response is broken, incomplete or stale,
// which makes fetchFirst() move on to the next source.
object Parsers {

    fun height(body: String, lastSeen: Long? = null): Long? {
        val h = body.trim().toLongOrNull() ?: return null
        if (h < 0) return null
        if (lastSeen != null && h < lastSeen - MAX_HEIGHT_DROP) return null
        return h
    }

    @Serializable
    data class Fees(val fast: Long, val halfHour: Long, val hour: Long)

    /** mempool.space { fastestFee, halfHourFee, hourFee } or blockstream.info { "1": rate, "3": rate, "6": rate } */
    fun fees(body: String): Fees? {
        val o = Json.parseToJsonElement(body).jsonObject
        o.num("fastestFee")?.let { fast ->
            return Fees(fast.roundToLong(), o.num("halfHourFee")?.roundToLong() ?: return null,
                o.num("hourFee")?.roundToLong() ?: return null)
        }
        val fast = o.num("1") ?: return null
        return Fees(fast.roundToLong(), o.num("3")?.roundToLong() ?: return null, o.num("6")?.roundToLong() ?: return null)
    }

    /**
     * Prices in all [CURRENCIES] from mempool { EUR: 95000, time: ... } or blockchain.info { EUR: { last: 95000 } }.
     * With [nowSec] set, a mempool response older than MAX_PRICE_AGE_SEC is rejected.
     * Null if no supported currency is present.
     */
    fun prices(body: String, nowSec: Long? = null): Map<String, Double>? {
        val o = Json.parseToJsonElement(body).jsonObject
        val time = o["time"]?.jsonPrimitive?.longOrNull
        if (nowSec != null && time != null && nowSec - time >= MAX_PRICE_AGE_SEC) return null
        val map = CURRENCIES.mapNotNull { c ->
            val entry = o[c] ?: return@mapNotNull null
            val price = if (entry is JsonPrimitive) entry.doubleOrNull else (entry as? JsonObject)?.num("last")
            if (price != null && price > 0) c to price else null
        }.toMap()
        return map.ifEmpty { null }
    }

    /** Most recent average hashrate in H/s from /mining/hashrate/1m. */
    fun hashrate(body: String): Double? {
        val arr = Json.parseToJsonElement(body).jsonObject["hashrates"]?.jsonArray ?: return null
        return arr.lastOrNull()?.jsonObject?.num("avgHashrate")
    }

    @Serializable
    data class Difficulty(val changePercent: Double, val remainingBlocks: Long)

    fun difficulty(body: String): Difficulty? {
        val o = Json.parseToJsonElement(body).jsonObject
        val remaining = o.num("remainingBlocks") ?: return null
        return Difficulty(o.num("difficultyChange") ?: return null, remaining.roundToLong())
    }

    private fun JsonObject.num(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull
}

object Format {

    /** 968662 -> "968 662" */
    fun height(h: Long): String {
        val s = h.toString()
        return if (s.length <= 3) s else s.dropLast(3) + " " + s.takeLast(3)
    }

    /** Low · medium · high, or reversed with [highToLow]. */
    fun fees(f: Parsers.Fees, highToLow: Boolean): String =
        if (highToLow) "${f.fast}·${f.halfHour}·${f.hour}" else "${f.hour}·${f.halfHour}·${f.fast}"

    /** Sats per 1 unit of fiat as HH:MM, e.g. 1344 sats -> "13:44". */
    fun moscowTime(price: Double): String {
        val sats = (100_000_000.0 / price).roundToLong().toString().padStart(4, '0')
        return sats.dropLast(2) + ":" + sats.takeLast(2)
    }

    fun price(price: Double): String = price.roundToLong().toString()

    /** Whole BTC mined up to [height]: 50 BTC per block, halving every 210000 blocks, genesis included. */
    fun supply(height: Long): String {
        var blocks = height + 1
        var sats = 0L
        var era = 0
        while (blocks > 0 && era < 64) {
            val n = minOf(blocks, 210_000L)
            sats += n * (5_000_000_000L shr era)
            blocks -= n
            era++
        }
        return (sats / 100_000_000L).toString()
    }

    /** H/s -> "880 EH/s" (whole exahashes, rounded down like the iOS widget). */
    fun hashrate(hs: Double): String = floor(hs / 1e18).toLong().toString() + " EH/s"

    /** "-3.2% / 1034 blk" */
    fun difficulty(d: Parsers.Difficulty): String =
        String.format(java.util.Locale.ROOT, "%.1f%% / %d blk", d.changePercent, d.remainingBlocks)
}
