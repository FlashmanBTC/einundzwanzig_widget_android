package flashman.einundzwanzig.widget.widget

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
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
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
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
import flashman.einundzwanzig.widget.BuildConfigInfo
import flashman.einundzwanzig.widget.R
import flashman.einundzwanzig.widget.data.Display
import flashman.einundzwanzig.widget.data.Item
import flashman.einundzwanzig.widget.data.Repository
import flashman.einundzwanzig.widget.data.RowKey
import flashman.einundzwanzig.widget.data.State
import flashman.einundzwanzig.widget.data.Theme
import flashman.einundzwanzig.widget.data.WidgetConfig
import flashman.einundzwanzig.widget.data.WidgetConfigs
import flashman.einundzwanzig.widget.data.store
import flashman.einundzwanzig.widget.update.RELEASES_URL
import flashman.einundzwanzig.widget.update.UpdateChecker
import flashman.einundzwanzig.widget.work.RefreshWorker
import kotlinx.coroutines.flow.map

// Same palette as the iOS widget
val C_BG = Color(0xFF151515)
val C_ACCENT = Color(0xFFF7931A)
val C_LABEL = Color(0xFFFFFFFF)
val C_DIM = Color(0xFF888888)
val C_ERROR = Color(0xFF555555)
val C_DIVIDER = Color(0xFF2A2A2A)

// Wide and flat -> block height left, rows right; otherwise stacked. The rows sit in a
// scrollable list: launchers report very different sizes for the same grid, so instead of
// guessing how many rows fit, all of them are there and a small widget can be scrolled.
private val WIDE_MIN_WIDTH = 230.dp
private val WIDE_MAX_HEIGHT = 150.dp
private val PADDING = 12.dp

/** Everything one widget needs to draw itself. */
private class ViewState(val display: Display?, val config: WidgetConfig, val update: String?, val debug: Boolean)

class EinundzwanzigWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val installed = BuildConfigInfo.versionName(context)
        val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        // Observe the store: snapshot, this widget's settings and the update notice can all change
        // while the Glance session is alive
        val states = context.store.data.map { prefs ->
            val config = WidgetConfigs.read(prefs, appWidgetId)
            ViewState(
                display = Repository.snapshot(prefs)?.let { Display(it, config) },
                config = config,
                update = UpdateChecker.available(prefs, installed),
                debug = debuggable,
            )
        }
        provideContent {
            val state by states.collectAsState(initial = null)
            Content(state)
        }
    }

    @Composable
    private fun Content(state: ViewState?) {
        val size = LocalSize.current
        val update = state?.update
        // With an update available, tapping opens the release page instead of refreshing
        val tap = if (update != null) actionStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL)))
                  else actionRunCallback<RefreshAction>()
        Box(
            modifier = GlanceModifier.fillMaxSize().background(C_BG).padding(PADDING).clickable(tap),
        ) {
            val d = state?.display
            if (state == null || d == null) {
                Loading()
                return@Box
            }
            val cfg = state.config
            // Test builds show the measured size in the status line, to tune the layout per launcher
            val sizeInfo = if (state.debug) " · ${size.width.value.toInt()}×${size.height.value.toInt()}" else ""
            val wide = cfg.showBlock && size.width - PADDING * 2 >= WIDE_MIN_WIDTH && size.height - PADDING * 2 < WIDE_MAX_HEIGHT
            when {
                wide -> Wide(d, cfg, update, sizeInfo)
                cfg.theme == Theme.CLASSIC -> Classic(d, cfg, update, sizeInfo)
                else -> Mono(d, cfg, update, sizeInfo)
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

    // Mono (like the iOS mono theme): logo, status, big block height, then label/value rows
    @Composable
    private fun Mono(d: Display, cfg: WidgetConfig, update: String?, sizeInfo: String) {
        Column(modifier = GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Header(d, cfg.rows, update, 10.sp, sizeInfo)
            if (cfg.showBlock) {
                Spacer(GlanceModifier.height(6.dp))
                BlockValue(d.block, "BLOCK", 38.sp, 9.sp)
                Spacer(GlanceModifier.height(6.dp))
            }
            LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                itemsIndexed(cfg.rows) { i, key ->
                    MonoRow(d.label(key, Theme.MONO), d.row(key), 16.sp, divider = cfg.showBlock || i > 0)
                }
            }
        }
    }

    // Classic (like the iOS classic theme): everything centred, label above value
    @Composable
    private fun Classic(d: Display, cfg: WidgetConfig, update: String?, sizeInfo: String) {
        Column(modifier = GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Header(d, cfg.rows, update, 10.sp, sizeInfo)
            Spacer(GlanceModifier.height(4.dp))
            LazyColumn(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (cfg.showBlock) item { Column { BlockValue(d.block, "Blockheight", 32.sp, 11.sp) } }
                items(cfg.rows) { key ->
                    Column(modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp)) {
                        BlockValue(d.row(key), d.label(key, Theme.CLASSIC), 20.sp, 11.sp)
                    }
                }
            }
        }
    }

    // Wide and flat: logo + block height on the left, rows on the right
    @Composable
    private fun Wide(d: Display, cfg: WidgetConfig, update: String?, sizeInfo: String) {
        Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Header(d, cfg.rows, update, 9.sp, sizeInfo)
                Spacer(GlanceModifier.defaultWeight())
                BlockValue(d.block, if (cfg.theme == Theme.MONO) "BLOCK" else "Blockheight", 28.sp, 9.sp)
                Spacer(GlanceModifier.defaultWeight())
            }
            Spacer(GlanceModifier.width(14.dp))
            LazyColumn(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
                itemsIndexed(cfg.rows) { i, key ->
                    MonoRow(d.label(key, Theme.MONO), d.row(key), 15.sp, divider = i > 0)
                }
            }
        }
    }

    @Composable
    private fun Header(d: Display, rows: List<RowKey>, update: String?, statusSize: TextUnit, sizeInfo: String) {
        Logo()
        Spacer(GlanceModifier.height(3.dp))
        Text(
            d.status(rows) + sizeInfo,
            maxLines = 1,
            style = TextStyle(color = ColorProvider(C_DIM), fontSize = statusSize, textAlign = TextAlign.Center),
            modifier = GlanceModifier.fillMaxWidth(),
        )
        if (update != null) {
            Text(
                "⬆ Update v$update available",
                maxLines = 1,
                style = TextStyle(color = ColorProvider(C_ACCENT), fontSize = statusSize, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                modifier = GlanceModifier.fillMaxWidth(),
            )
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

    /** Centred label with a big value below: the block height, and every value in the classic theme. */
    @Composable
    private fun BlockValue(item: Item, label: String, size: TextUnit, labelSize: TextUnit) {
        Text(
            label,
            maxLines = 1,
            style = TextStyle(color = ColorProvider(C_LABEL), fontSize = labelSize, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
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
    private fun MonoRow(label: String, item: Item, size: TextUnit, divider: Boolean) {
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
