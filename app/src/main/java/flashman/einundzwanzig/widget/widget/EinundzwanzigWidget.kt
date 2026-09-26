package flashman.einundzwanzig.widget.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import flashman.einundzwanzig.widget.data.Item
import flashman.einundzwanzig.widget.data.Repository
import flashman.einundzwanzig.widget.data.Snapshot
import flashman.einundzwanzig.widget.data.State
import flashman.einundzwanzig.widget.work.RefreshWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Same palette as the iOS widget
val C_BG = Color(0xFF151515)
val C_ACCENT = Color(0xFFF7931A)
val C_LABEL = Color(0xFFFFFFFF)
val C_DIM = Color(0xFF888888)
val C_ERROR = Color(0xFF555555)

class EinundzwanzigWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Observe the store: a Glance session outlives a single update, so reading the
        // snapshot once here would keep showing whatever was stored when the session started
        val snapshots = Repository.snapshots(context)
        provideContent {
            val snapshot by snapshots.collectAsState(initial = null)
            Content(snapshot)
        }
    }

    // Phase 2: plain list of all values; the mono/classic layouts per size follow in phases 3 and 4
    @Composable
    private fun Content(s: Snapshot?) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(C_BG)
                .padding(12.dp)
                .clickable(actionRunCallback<RefreshAction>()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("₿ Einundzwanzig", style = TextStyle(color = ColorProvider(C_ACCENT), fontSize = 16.sp, fontWeight = FontWeight.Bold))
            if (s == null) {
                Spacer(GlanceModifier.height(8.dp))
                Text("Loading… (tap to retry)", style = TextStyle(color = ColorProvider(C_DIM), fontSize = 12.sp))
                return@Column
            }
            Text(statusText(s), style = TextStyle(color = ColorProvider(C_DIM), fontSize = 10.sp))
            Spacer(GlanceModifier.height(6.dp))
            DataRow("BLOCK", s.height)
            DataRow("FEES  L·M·H", s.fees)
            DataRow("MOSCOW", s.moscow)
            DataRow("${s.currency}/BTC", s.price)
            DataRow("SUPPLY", s.supply)
            DataRow("HASHRATE", s.hashrate)
            DataRow("DIFFICULTY", s.difficulty)
        }
    }

    @Composable
    private fun DataRow(label: String, item: Item) {
        Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Text(label, style = TextStyle(color = ColorProvider(C_LABEL), fontSize = 10.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.defaultWeight())
            Text(item.text, style = TextStyle(color = ColorProvider(stateColor(item.state)), fontSize = 13.sp, fontWeight = FontWeight.Bold))
        }
    }
}

/** 🟢 all fresh | 🟡 partly fresh | 🔴 nothing fresh (cached values may still show), plus time and cache age. */
fun statusText(s: Snapshot): String {
    val icon = when {
        s.items.all { it.state == State.OK } -> "🟢"
        s.items.none { it.state == State.OK } -> "🔴"
        else -> "🟡"
    }
    val time = SimpleDateFormat("HH:mm", Locale.getDefault())
    val cache = s.oldestCacheAt?.let { "  ·  cache " + time.format(Date(it)) } ?: ""
    return "$icon ${time.format(Date(s.updatedAt))}$cache"
}

fun stateColor(state: State) = when (state) {
    State.OK -> C_ACCENT
    State.CACHED -> C_DIM
    State.FAIL -> C_ERROR
}

/** Tapping the widget refreshes immediately. */
class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        RefreshWorker.refreshNow(context)
    }
}
