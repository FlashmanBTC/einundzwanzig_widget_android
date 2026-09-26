package flashman.einundzwanzig.widget.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.serialization.Serializable

/** Values that can be shown as rows below the block height. */
@Serializable
enum class RowKey(val monoLabel: String, val classicLabel: String) {
    FEES("FEES  L·M·H", "Mempool Fees"),
    MOSCOW("MOSCOW", "Moscow Time"),
    PRICE("PRICE", "Price"),
    SUPPLY("SUPPLY", "Supply"),
    HASHRATE("HASHRATE", "Hashrate"),
    DIFFICULTY("DIFFICULTY", "Difficulty adjustment"),
}

@Serializable
enum class Theme { MONO, CLASSIC }

/** Settings of one placed widget. Every widget has its own. */
@Serializable
data class WidgetConfig(
    val showBlock: Boolean = true,
    /** Shown rows in display order; rows that do not fit the widget are left out from the end. */
    val rows: List<RowKey> = RowKey.entries.toList(),
    val currency: String = "EUR",
    val feesHighToLow: Boolean = false,
    val theme: Theme = Theme.MONO,
)

object WidgetConfigs {
    private fun key(appWidgetId: Int) = stringPreferencesKey("widget_$appWidgetId")

    fun read(prefs: Preferences, appWidgetId: Int): WidgetConfig =
        prefs[key(appWidgetId)]?.let { runCatching { json.decodeFromString<WidgetConfig>(it) }.getOrNull() }
            ?: WidgetConfig()

    fun exists(prefs: Preferences, appWidgetId: Int) = prefs[key(appWidgetId)] != null

    suspend fun save(context: Context, appWidgetId: Int, config: WidgetConfig) {
        context.store.edit { it[key(appWidgetId)] = json.encodeToString(WidgetConfig.serializer(), config) }
    }

    suspend fun delete(context: Context, appWidgetIds: IntArray) {
        context.store.edit { e -> appWidgetIds.forEach { e.remove(key(it)) } }
    }
}
