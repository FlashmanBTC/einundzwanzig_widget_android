package flashman.einundzwanzig.widget.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayTest {

    private val snap = Snapshot(
        updatedAt = 0,
        height = Sourced(968662L, State.OK, 0),
        fees = Sourced(Parsers.Fees(3, 2, 1), State.OK, 0),
        prices = Sourced(mapOf("EUR" to 74379.0, "USD" to 84554.0), State.CACHED, 0),
        hashrate = Sourced(8.8e20, State.OK, 0),
        difficulty = Sourced(),
    )

    @Test fun `price follows the widget currency`() {
        assertEquals("84554", Display(snap, WidgetConfig(currency = "USD")).row(RowKey.PRICE).text)
        assertEquals("USD/BTC", Display(snap, WidgetConfig(currency = "USD")).label(RowKey.PRICE, Theme.MONO))
    }

    @Test fun `currency missing in the source is n a`() =
        assertEquals(State.FAIL, Display(snap, WidgetConfig(currency = "JPY")).row(RowKey.PRICE).state)

    @Test fun `fee order per widget`() {
        assertEquals("1·2·3", Display(snap, WidgetConfig(feesHighToLow = false)).row(RowKey.FEES).text)
        assertEquals("3·2·1", Display(snap, WidgetConfig(feesHighToLow = true)).row(RowKey.FEES).text)
    }

    @Test fun `cached state is kept`() = assertEquals(State.CACHED, Display(snap, WidgetConfig()).row(RowKey.MOSCOW).state)

    @Test fun `missing source is n a`() = assertEquals("⚠️ n/a", Display(snap, WidgetConfig()).row(RowKey.DIFFICULTY).text)

    @Test fun `status only counts shown values`() {
        val d = Display(snap, WidgetConfig())
        assertTrue(d.status(listOf(RowKey.FEES, RowKey.HASHRATE)).startsWith("🟢"))
        assertTrue(d.status(listOf(RowKey.FEES, RowKey.PRICE)).startsWith("🟡"))
        assertTrue(Display(snap, WidgetConfig(showBlock = false)).status(listOf(RowKey.DIFFICULTY)).startsWith("🔴"))
    }

    @Test fun `old stored json with unknown fields still loads`() {
        val stored = """{"updatedAt":1,"height":{"value":5,"state":"OK","at":1},"somethingNew":true}"""
        assertEquals(5L, json.decodeFromString<Snapshot>(stored).height.value)
    }
}
