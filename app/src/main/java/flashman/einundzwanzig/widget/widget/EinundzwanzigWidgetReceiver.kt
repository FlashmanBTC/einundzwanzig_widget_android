package flashman.einundzwanzig.widget.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import flashman.einundzwanzig.widget.data.WidgetConfigs
import flashman.einundzwanzig.widget.work.RefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EinundzwanzigWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EinundzwanzigWidget()

    // Called on placement and after app updates: make sure the schedule exists and load data right away
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        RefreshWorker.schedule(context)
        RefreshWorker.refreshNow(context)
    }

    // Removed widgets: forget their settings. No goAsync() here - Glance's own onDeleted
    // already claims it, and a second call returns null
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch { WidgetConfigs.delete(app, appWidgetIds) }
    }

    // Last widget removed: stop the background refresh
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        RefreshWorker.cancel(context)
    }
}
