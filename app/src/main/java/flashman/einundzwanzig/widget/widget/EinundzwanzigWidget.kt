package flashman.einundzwanzig.widget.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import flashman.einundzwanzig.widget.BuildConfigInfo
import flashman.einundzwanzig.widget.settings.SettingsActivity

// Same palette as the iOS widget
val C_BG = Color(0xFF151515)
val C_ACCENT = Color(0xFFF7931A)
val C_LABEL = Color(0xFFFFFFFF)
val C_DIM = Color(0xFF888888)

class EinundzwanzigWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val version = BuildConfigInfo.versionName(context)
        provideContent { Placeholder(version) }
    }

    // Phase 1: proves install, widget placement and tap handling work on the device
    @Composable
    private fun Placeholder(version: String) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(C_BG)
                .padding(12.dp)
                .clickable(actionStartActivity<SettingsActivity>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "₿ Einundzwanzig",
                style = TextStyle(color = ColorProvider(C_ACCENT), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(GlanceModifier.height(6.dp))
            Text("Hallo 👋", style = TextStyle(color = ColorProvider(C_LABEL), fontSize = 14.sp))
            Spacer(GlanceModifier.height(4.dp))
            Text("v$version", style = TextStyle(color = ColorProvider(C_DIM), fontSize = 11.sp))
        }
    }
}
