package flashman.einundzwanzig.widget.work

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import flashman.einundzwanzig.widget.data.Repository
import flashman.einundzwanzig.widget.widget.EinundzwanzigWidget
import java.util.concurrent.TimeUnit

/** Fetches fresh data and redraws every placed widget. */
class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ok = runCatching { Repository.refresh(applicationContext) }
            .onFailure { Log.e(TAG, "refresh failed", it) }
            .isSuccess
        EinundzwanzigWidget().updateAll(applicationContext)
        return if (ok) Result.success() else Result.retry()
    }

    companion object {
        private const val TAG = "RefreshWorker"
        private const val PERIODIC = "refresh-periodic"
        private const val NOW = "refresh-now"

        /** Every 15 min, the shortest interval Android allows. KEEP leaves an existing schedule alone. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RefreshWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** Immediate refresh, e.g. when the widget is placed or tapped. Runs offline too, to show cached values. */
        fun refreshNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<RefreshWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
        }
    }
}
