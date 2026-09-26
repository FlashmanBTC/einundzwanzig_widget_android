package flashman.einundzwanzig.widget.settings

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.glance.appwidget.GlanceAppWidgetManager
import flashman.einundzwanzig.widget.BuildConfigInfo
import flashman.einundzwanzig.widget.data.store
import flashman.einundzwanzig.widget.update.CHECK_UPDATES
import flashman.einundzwanzig.widget.update.RELEASES_URL
import flashman.einundzwanzig.widget.update.UpdateChecker
import flashman.einundzwanzig.widget.widget.EinundzwanzigWidget
import flashman.einundzwanzig.widget.work.RefreshWorker
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// Same palette as the widget
val BG = Color(0xFF151515)
val ACCENT = Color(0xFFF7931A)
val TEXT = Color(0xFFF2F2F2)
val DIM = Color(0xFF9A9A9A)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(primary = ACCENT, onPrimary = BG, secondaryContainer = Color(0xFF3A2A12), background = BG, surface = BG),
        content = content,
    )
}

/** App screen: placed widgets (with their settings), app-wide options, version. */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppTheme { MainScreen() } }
    }
}

@Composable
private fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val version = remember { BuildConfigInfo.versionName(context) }
    var widgetIds by remember { mutableStateOf<List<Int>>(emptyList()) }
    val checkUpdates by remember { context.store.data.map { UpdateChecker.enabled(it) } }.collectAsState(initial = true)
    val update by remember { context.store.data.map { UpdateChecker.available(it, version) } }.collectAsState(initial = null)

    LaunchedEffect(Unit) {
        widgetIds = GlanceAppWidgetManager(context).getGlanceIds(EinundzwanzigWidget::class.java)
            .map { GlanceAppWidgetManager(context).getAppWidgetId(it) }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BG).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        Text("Einundzwanzig Widget", color = ACCENT, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("v$version", color = DIM, fontSize = 14.sp)

        update?.let {
            Spacer(Modifier.height(16.dp))
            Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL))) }, modifier = Modifier.fillMaxWidth()) {
                Text("⬆ Update v$it available", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("WIDGETS", color = DIM, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        if (widgetIds.isEmpty()) {
            Text(
                "No widget placed yet. Long-press your home screen → Widgets → Einundzwanzig Bitcoin.",
                color = TEXT, fontSize = 15.sp,
            )
        } else {
            widgetIds.forEachIndexed { i, id ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("Widget ${i + 1}", color = TEXT, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = {
                        context.startActivity(
                            Intent(context, WidgetConfigActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
                        )
                    }) { Text("Settings") }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { RefreshWorker.refreshNow(context) }, modifier = Modifier.fillMaxWidth()) { Text("Refresh now") }
        }

        Spacer(Modifier.height(24.dp))
        Text("APP", color = DIM, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Check for updates daily", color = TEXT, fontSize = 16.sp)
                Text("Asks GitHub once a day for a newer release", color = DIM, fontSize = 13.sp)
            }
            Switch(checked = checkUpdates, onCheckedChange = { on ->
                scope.launch { context.store.edit { it[CHECK_UPDATES] = on } }
            })
        }

        Spacer(Modifier.height(32.dp))
        Text("Data: mempool.space, blockstream.info, blockchain.info, mempool.flashman.ch", color = DIM, fontSize = 12.sp)
        Text("Open source (MIT) · github.com/FlashmanBTC", color = DIM, fontSize = 12.sp)
    }
}
