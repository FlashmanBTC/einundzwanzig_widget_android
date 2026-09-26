package flashman.einundzwanzig.widget.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import flashman.einundzwanzig.widget.R
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
val C_DIVIDER = Color(0xFF2A2A2A)

// Layout by the widget's real size (launchers report very different sizes for the same grid):
// wide and flat -> block height left, rows right; otherwise stacked, with as many rows as fit
private val WIDE_MIN_WIDTH = 230.dp
private val WIDE_MAX_HEIGHT = 170.dp
private val PADDING = 12.dp
private val HEADER_HEIGHT = 34.dp      // logo + status line
private val BLOCK_HEIGHT = 62.dp       // "BLOCK" label + big value + spacing
private val ROW_HEIGHT = 28.dp         // divider + row with padding

class EinundzwanzigWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Observe the store: a Glance session outlives a single update, so reading the
        // snapshot once here would keep showing whatever was stored when the session started
        val snapshots = Repository.snapshots(context)
        provideContent {
            val snapshot by snapshots.collectAsState(initial = null)
            Content(snapshot)
        }
    }

    @Composable
    private fun Content(s: Snapshot?) {
        val size = LocalSize.current
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(C_BG)
                .padding(PADDING)
                .clickable(actionRunCallback<RefreshAction>()),
        ) {
            val inner = DpSize(size.width - PADDING * 2, size.height - PADDING * 2)
            when {
                s == null -> Loading()
                inner.width >= WIDE_MIN_WIDTH && inner.height < WIDE_MAX_HEIGHT -> Wide(s, rowsFitting(inner.height, stacked = false))
                else -> Stacked(s, rowsFitting(inner.height, stacked = true))
            }
        }
    }

    @Composable
    private fun Loading() {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Logo()
            Spacer(GlanceModifier.height(8.dp))
            Text("Loading… (tap to retry)", style = TextStyle(color = ColorProvider(C_DIM), fontSize = 11.sp))
        }
    }

    /** Rows below the block height, in this order, as far as they fit. */
    private fun rows(s: Snapshot) = listOf(
        "FEES  L·M·H" to s.fees,
        "MOSCOW" to s.moscow,
        "${s.currency}/BTC" to s.price,
        "SUPPLY" to s.supply,
        "HASHRATE" to s.hashrate,
        "DIFFICULTY" to s.difficulty,
    )

    private fun rowsFitting(height: androidx.compose.ui.unit.Dp, stacked: Boolean): Int {
        val free = if (stacked) height - HEADER_HEIGHT - BLOCK_HEIGHT else height
        return (free / ROW_HEIGHT).toInt().coerceIn(0, 6)
    }

    // Stacked (the iOS mono theme): logo, status, big block height, then as many rows as fit
    @Composable
    private fun Stacked(s: Snapshot, rowCount: Int) {
        Column(modifier = GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Logo()
            Spacer(GlanceModifier.height(3.dp))
            Status(s, 10.sp)
            Spacer(GlanceModifier.defaultWeight())
            Block(s.height, if (rowCount == 0) 30.sp else 38.sp)
            Spacer(GlanceModifier.defaultWeight())
            rows(s).take(rowCount).forEach { (label, item) -> DataRow(label, item, 16.sp) }
        }
    }

    // Wide and flat: logo + block height on the left, rows on the right
    @Composable
    private fun Wide(s: Snapshot, rowCount: Int) {
        Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Logo()
                Status(s, 9.sp)
                Spacer(GlanceModifier.defaultWeight())
                Block(s.height, 28.sp)
                Spacer(GlanceModifier.defaultWeight())
            }
            Spacer(GlanceModifier.width(14.dp))
            Column(modifier = GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                rows(s).take(rowCount.coerceAtLeast(1)).forEachIndexed { i, (label, item) ->
                    DataRow(label, item, 15.sp, divider = i > 0)
                }
            }
        }
    }

    @Composable
    private fun Logo() {
        Image(
            provider = ImageProvider(R.drawable.logo),
            contentDescription = "Einundzwanzig",
            contentScale = ContentScale.Fit,
            modifier = GlanceModifier.fillMaxWidth().height(16.dp),
        )
    }

    @Composable
    private fun Status(s: Snapshot, size: TextUnit) {
        Text(
            statusText(s),
            style = TextStyle(color = ColorProvider(C_DIM), fontSize = size, textAlign = TextAlign.Center),
            modifier = GlanceModifier.fillMaxWidth(),
        )
    }

    @Composable
    private fun ColumnScope.Block(item: Item, size: TextUnit) {
        Text(
            "BLOCK",
            style = TextStyle(color = ColorProvider(C_LABEL), fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            modifier = GlanceModifier.fillMaxWidth(),
        )
        Text(
            item.text,
            maxLines = 1,
            style = TextStyle(color = ColorProvider(stateColor(item.state)), fontSize = size, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            modifier = GlanceModifier.fillMaxWidth(),
        )
    }

    @Composable
    private fun DataRow(label: String, item: Item, size: TextUnit, divider: Boolean = true) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            if (divider) Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(C_DIVIDER)) {}
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, maxLines = 1, style = TextStyle(color = ColorProvider(C_LABEL), fontSize = 9.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    item.text,
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(stateColor(item.state)), fontSize = size, fontWeight = FontWeight.Bold),
                )
            }
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
    val cache = s.oldestCacheAt?.let { " · cache " + time.format(Date(it)) } ?: ""
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
