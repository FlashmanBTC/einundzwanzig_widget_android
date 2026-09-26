package flashman.einundzwanzig.widget.update

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import flashman.einundzwanzig.widget.data.Http
import flashman.einundzwanzig.widget.data.store
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

const val RELEASES_URL = "https://github.com/FlashmanBTC/einundzwanzig_widget_android/releases/latest"
private const val LATEST_API = "https://api.github.com/repos/FlashmanBTC/einundzwanzig_widget_android/releases/latest"
private const val INTERVAL_MS = 24 * 60 * 60 * 1000L

val CHECK_UPDATES = booleanPreferencesKey("check_updates")
private val LATEST = stringPreferencesKey("update_latest")
private val CHECKED = longPreferencesKey("update_checked")

/**
 * Once a day asks GitHub for the latest release. Users who installed via Obtainium
 * get updates anyway; the notice is for everyone who installed the APK by hand.
 */
object UpdateChecker {

    fun enabled(prefs: Preferences) = prefs[CHECK_UPDATES] ?: true

    /** Newer version to announce, or null. */
    fun available(prefs: Preferences, installed: String): String? {
        if (!enabled(prefs)) return null
        val latest = prefs[LATEST] ?: return null
        return latest.takeIf { isNewer(it, installed) }
    }

    suspend fun checkIfDue(context: Context) {
        val prefs = context.store.data.first()
        if (!enabled(prefs)) return
        if (System.currentTimeMillis() - (prefs[CHECKED] ?: 0L) < INTERVAL_MS) return
        // On failure (offline, no release yet) keep the old result and try again next refresh
        val body = Http.get(LATEST_API) ?: return
        val tag = runCatching { Json.parseToJsonElement(body).jsonObject["tag_name"]?.jsonPrimitive?.content }.getOrNull() ?: return
        context.store.edit {
            it[LATEST] = tag.removePrefix("v")
            it[CHECKED] = System.currentTimeMillis()
        }
    }

    /** "1.2.0" > "1.1.9"; suffixes like "-debug" are ignored. */
    fun isNewer(candidate: String, installed: String): Boolean {
        val a = parts(candidate)
        val b = parts(installed)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private fun parts(v: String) = v.removePrefix("v").substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
}
