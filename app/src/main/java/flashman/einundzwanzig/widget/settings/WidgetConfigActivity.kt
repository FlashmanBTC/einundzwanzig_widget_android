package flashman.einundzwanzig.widget.settings

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import flashman.einundzwanzig.widget.data.CURRENCIES
import flashman.einundzwanzig.widget.data.RowKey
import flashman.einundzwanzig.widget.data.Theme
import flashman.einundzwanzig.widget.data.WidgetConfig
import flashman.einundzwanzig.widget.data.WidgetConfigs
import flashman.einundzwanzig.widget.data.store
import flashman.einundzwanzig.widget.widget.EinundzwanzigWidget
import flashman.einundzwanzig.widget.work.RefreshWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Settings of one widget. Opens when the widget is placed and again via
 * "reconfigure" (long-press on Android 12+) or from the app's main screen.
 */
class WidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Backing out of the first configuration cancels placing the widget
        setResult(RESULT_CANCELED, resultIntent(appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        lifecycleScope.launch {
            val initial = WidgetConfigs.read(store.data.first(), appWidgetId)
            setContent {
                AppTheme {
                    ConfigScreen(initial, onSave = { save(appWidgetId, it) })
                }
            }
        }
    }

    private fun save(appWidgetId: Int, config: WidgetConfig) {
        lifecycleScope.launch {
            WidgetConfigs.save(this@WidgetConfigActivity, appWidgetId, config)
            val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(appWidgetId)
            EinundzwanzigWidget().update(this@WidgetConfigActivity, glanceId)
            RefreshWorker.schedule(this@WidgetConfigActivity)
            RefreshWorker.refreshNow(this@WidgetConfigActivity)
            setResult(RESULT_OK, resultIntent(appWidgetId))
            finish()
        }
    }

    private fun resultIntent(appWidgetId: Int) = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfigScreen(initial: WidgetConfig, onSave: (WidgetConfig) -> Unit) {
    var showBlock by remember { mutableStateOf(initial.showBlock) }
    // All rows in display order; the enabled ones first, as configured
    var order by remember { mutableStateOf(initial.rows + RowKey.entries.filter { it !in initial.rows }) }
    var enabled by remember { mutableStateOf(initial.rows.toSet()) }
    var currency by remember { mutableStateOf(initial.currency) }
    var highToLow by remember { mutableStateOf(initial.feesHighToLow) }
    var theme by remember { mutableStateOf(initial.theme) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Widget settings", color = ACCENT, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))

        Section("Theme")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Theme.entries.forEach { t ->
                FilterChip(selected = theme == t, onClick = { theme = t }, label = { Text(t.name.lowercase().replaceFirstChar(Char::uppercase)) })
            }
        }

        Section("Block height")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Show block height", color = TEXT, modifier = Modifier.weight(1f))
            Switch(checked = showBlock, onCheckedChange = { showBlock = it })
        }

        Section("Values")
        Text("Tick what to show, use the arrows to change the order.", color = DIM, fontSize = 13.sp)
        order.forEachIndexed { i, key ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(
                    checked = key in enabled,
                    onCheckedChange = { on -> enabled = if (on) enabled + key else enabled - key },
                )
                Text(key.classicLabel.replace("Price", "Price ($currency)"), color = TEXT, modifier = Modifier.weight(1f))
                TextButton(onClick = { order = order.swap(i, i - 1) }, enabled = i > 0) { Text("↑", fontSize = 18.sp) }
                TextButton(onClick = { order = order.swap(i, i + 1) }, enabled = i < order.lastIndex) { Text("↓", fontSize = 18.sp) }
            }
        }

        Section("Currency")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CURRENCIES.forEach { c -> FilterChip(selected = currency == c, onClick = { currency = c }, label = { Text(c) }) }
        }

        Section("Fee order")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !highToLow, onClick = { highToLow = false }, label = { Text("Low → high") })
            FilterChip(selected = highToLow, onClick = { highToLow = true }, label = { Text("High → low") })
        }

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = {
                scope.launch {
                    onSave(WidgetConfig(showBlock, order.filter { it in enabled }, currency, highToLow, theme))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(20.dp))
    Text(title.uppercase(), color = DIM, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
}

private fun <T> List<T>.swap(a: Int, b: Int): List<T> =
    if (b !in indices) this else toMutableList().also { it[a] = this[b]; it[b] = this[a] }
