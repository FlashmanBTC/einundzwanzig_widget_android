package flashman.einundzwanzig.widget.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A formatted value plus whether it is fresh, cached or missing. */
data class Item(val text: String, val state: State, val at: Long)

/** Turns the raw snapshot into what one widget shows, according to its settings. */
class Display(private val s: Snapshot, private val cfg: WidgetConfig) {

    val block: Item get() = item(s.height) { Format.height(it) }

    fun row(key: RowKey): Item = when (key) {
        RowKey.FEES -> item(s.fees) { Format.fees(it, cfg.feesHighToLow) }
        RowKey.MOSCOW -> item(s.prices) { p -> p[cfg.currency]?.let(Format::moscowTime) }
        RowKey.PRICE -> item(s.prices) { p -> p[cfg.currency]?.let(Format::price) }
        RowKey.SUPPLY -> item(s.height) { Format.supply(it) }
        RowKey.HASHRATE -> item(s.hashrate) { Format.hashrate(it) }
        RowKey.DIFFICULTY -> item(s.difficulty) { Format.difficulty(it) }
    }

    fun label(key: RowKey, theme: Theme): String = when {
        key == RowKey.PRICE -> "${cfg.currency}/BTC"
        theme == Theme.MONO -> key.monoLabel
        else -> key.classicLabel
    }

    /**
     * 🟢 all shown values fresh | 🟡 some | 🔴 none (cached values may still show),
     * plus the refresh time and the age of the oldest cached value on display.
     */
    fun status(shownRows: List<RowKey>): String {
        val items = (if (cfg.showBlock) listOf(block) else emptyList()) + shownRows.map(::row)
        val icon = when {
            items.all { it.state == State.OK } -> "🟢"
            items.none { it.state == State.OK } -> "🔴"
            else -> "🟡"
        }
        val time = SimpleDateFormat("HH:mm", Locale.getDefault())
        val oldestCache = items.filter { it.state == State.CACHED }.minOfOrNull { it.at }
        val cache = oldestCache?.let { " · cache " + time.format(Date(it)) } ?: ""
        return "$icon ${time.format(Date(s.updatedAt))}$cache"
    }

    private fun <T> item(src: Sourced<T>, format: (T) -> String?): Item {
        val text = src.value?.let(format)
        return if (text == null || src.state == State.FAIL) Item("⚠️ n/a", State.FAIL, 0) else Item(text, src.state, src.at)
    }
}
