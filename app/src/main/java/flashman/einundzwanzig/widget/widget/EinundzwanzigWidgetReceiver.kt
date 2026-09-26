package flashman.einundzwanzig.widget.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import flashman.einundzwanzig.widget.work.RefreshWorker

class EinundzwanzigWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EinundzwanzigWidget()

    // Called on placement and after app updates: make sure the schedule exists and load data right away
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        RefreshWorker.schedule(context)
        RefreshWorker.refreshNow(context)
    }

    // Last widget removed: stop the background refresh
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        RefreshWorker.cancel(context)
    }
}
