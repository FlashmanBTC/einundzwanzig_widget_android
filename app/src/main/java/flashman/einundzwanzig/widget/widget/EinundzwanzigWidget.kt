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

// Layout buckets: Glance picks the largest one that fits the size the user dragged the widget to
private val SMALL = DpSize(110.dp, 110.dp)   // ~2x2: block height only
private val WIDE = DpSize(250.dp, 110.dp)    // ~4x2: block height + fees, Moscow Time, price
private val LARGE = DpSize(250.dp, 250.dp)   // ~4x4: everything, like the iOS mono theme

class EinundzwanzigWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE, LARGE))

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
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .clickable(actionRunCallback<RefreshAction>()),
        ) {
            when {
                s == null -> Loading()
                size.height >= LARGE.height && size.width >= LARGE.width -> Large(s)
                size.width >= WIDE.width -> Wide(s)
                else -> Small(s)
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

    // ~2x2: logo, status, block height
    @Composable
    private fun Small(s: Snapshot) {
        Column(modifier = GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Logo()
            Status(s, 9.sp)
            Spacer(GlanceModifier.defaultWeight())
            Block(s.height, 28.sp)
            Spacer(GlanceModifier.defaultWeight())
        }
    }

    // ~4x2: logo + block height on the left, three rows on the right
    @Composable
    private fun Wide(s: Snapshot) {
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
                DataRow("FEES  L·M·H", s.fees, 15.sp, divider = false)
                DataRow("MOSCOW", s.moscow, 15.sp)
                DataRow("${s.currency}/BTC", s.price, 15.sp)
            }
        }
    }

    // ~4x4: the iOS mono theme - logo, status, big block height, then label/value rows
    @Composable
    private fun Large(s: Snapshot) {
        Column(modifier = GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Logo()
            Spacer(GlanceModifier.height(3.dp))
            Status(s, 10.sp)
            Spacer(GlanceModifier.defaultWeight())
            Block(s.height, 40.sp)
            Spacer(GlanceModifier.height(8.dp))
            DataRow("FEES  L·M·H", s.fees, 16.sp)
            DataRow("MOSCOW", s.moscow, 16.sp)
            DataRow("${s.currency}/BTC", s.price, 16.sp)
            DataRow("SUPPLY", s.supply, 16.sp)
            DataRow("HASHRATE", s.hashrate, 16.sp)
            DataRow("DIFFICULTY", s.difficulty, 16.sp)
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
