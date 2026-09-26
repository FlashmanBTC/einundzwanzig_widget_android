package flashman.einundzwanzig.widget.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flashman.einundzwanzig.widget.BuildConfigInfo

// Phase 1 placeholder; the real settings (currency, theme, rows, ...) follow in phase 4
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val version = BuildConfigInfo.versionName(this)
        setContent {
            Column(
                modifier = Modifier.fillMaxSize().background(Color(0xFF151515)).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Einundzwanzig Widget", color = Color(0xFFF7931A), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("v$version", color = Color(0xFF888888), fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                Text(
                    "Long-press your home screen → Widgets → Einundzwanzig Bitcoin",
                    color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
    }
}
