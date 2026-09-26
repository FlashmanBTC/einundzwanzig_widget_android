package flashman.einundzwanzig.widget.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParsersTest {

    @Test fun `height parses plain text`() = assertEquals(968662L, Parsers.height("968662\n"))
    @Test fun `height rejects garbage`() = assertNull(Parsers.height("<html>503</html>"))
    @Test fun `height rejects a lagging node`() = assertNull(Parsers.height("968482", lastSeen = 968662))
    @Test fun `height tolerates a small reorg`() = assertEquals(968660L, Parsers.height("968660", lastSeen = 968662))

    @Test fun `fees from mempool`() = assertEquals(
        Parsers.Fees(3, 2, 1),
        Parsers.fees("""{"fastestFee":3,"halfHourFee":2,"hourFee":1,"economyFee":1,"minimumFee":1}"""),
    )

    @Test fun `fees from blockstream`() = assertEquals(
        Parsers.Fees(5, 3, 2),
        Parsers.fees("""{"1":4.8,"2":4.1,"3":3.2,"6":2.1,"144":1.0}"""),
    )

    @Test fun `fees reject error body`() = assertNull(runCatching { Parsers.fees("""{"error":"Service Temporarily Unavailable"}""") }.getOrNull())

    private val mempoolPrices = """{"time":1790301218,"USD":84554,"EUR":74379,"CHF":69850,"JPY":13425330}"""

    @Test fun `prices from mempool`() = assertEquals(
        mapOf("EUR" to 74379.0, "USD" to 84554.0, "CHF" to 69850.0, "JPY" to 13425330.0),
        Parsers.prices(mempoolPrices),
    )
    @Test fun `prices from blockchain info`() = assertEquals(
        mapOf("EUR" to 73813.5, "USD" to 84000.0),
        Parsers.prices("""{"EUR":{"15m":73813.5,"last":73813.5,"symbol":"€"},"USD":{"last":84000},"XYZ":{"last":1}}"""),
    )
    @Test fun `prices without a supported currency`() = assertNull(Parsers.prices("""{"XYZ":1}"""))
    @Test fun `prices accept fresh mempool data`() = assertEquals(74379.0, Parsers.prices(mempoolPrices, 1790301218 + 600)!!["EUR"]!!, 0.0)
    // The syncing node on 2026-09-26 served a 30 h old price with HTTP 200
    @Test fun `prices reject stale mempool data`() = assertNull(Parsers.prices(mempoolPrices, 1790301218 + 30 * 3600))

    @Test fun `hashrate takes the latest entry`() = assertEquals(
        8.8e20, Parsers.hashrate("""{"hashrates":[{"timestamp":1,"avgHashrate":8.1e20},{"timestamp":2,"avgHashrate":8.8e20}]}""")!!, 0.0,
    )
    @Test fun `hashrate rejects empty list`() = assertNull(Parsers.hashrate("""{"hashrates":[]}"""))

    @Test fun `difficulty parses`() = assertEquals(
        Parsers.Difficulty(-3.21, 1034),
        Parsers.difficulty("""{"progressPercent":48.7,"difficultyChange":-3.21,"remainingBlocks":1034}"""),
    )
}

class FormatTest {
    @Test fun height() = assertEquals("968 662", Format.height(968662))
    @Test fun `fees low to high`() = assertEquals("1·2·3", Format.fees(Parsers.Fees(3, 2, 1), highToLow = false))
    @Test fun `fees high to low`() = assertEquals("3·2·1", Format.fees(Parsers.Fees(3, 2, 1), highToLow = true))

    // v10 showed "000013:44" in its fallback path
    @Test fun `moscow time without leading zeros`() = assertEquals("13:44", Format.moscowTime(100_000_000.0 / 1344))
    @Test fun `moscow time for JPY`() = assertEquals("00:07", Format.moscowTime(13_425_330.0))
    @Test fun `moscow time for a low price`() = assertEquals("123:46", Format.moscowTime(100_000_000.0 / 12346))

    @Test fun `supply at genesis`() = assertEquals("50", Format.supply(0))
    @Test fun `supply at the end of the first era`() = assertEquals("10500000", Format.supply(209_999))
    // Same value the iOS widget v11 calculated for this height
    @Test fun `supply today`() = assertEquals("20089571", Format.supply(968_662))

    @Test fun hashrate() = assertEquals("880 EH/s", Format.hashrate(8.8093e20))
    @Test fun difficulty() = assertEquals("-3.2% / 1034 blk", Format.difficulty(Parsers.Difficulty(-3.21, 1034)))
}
