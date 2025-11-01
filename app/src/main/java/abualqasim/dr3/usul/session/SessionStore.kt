package abualqasim.dr3.usul.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.sessionDataStore by preferencesDataStore(name = "session_prefs")

class SessionStore(private val context: Context) {
    private object Keys {
        val USER_ID     = stringPreferencesKey("user_id")
        val USER_HASH   = stringPreferencesKey("user_hash")
        val CITY        = stringPreferencesKey("session_city")
        val DISTRICT    = stringPreferencesKey("session_district")
        val LOGGED_AT   = longPreferencesKey("logged_at")
    }

    val userIdFlow: Flow<String> = context.sessionDataStore.data.map { it[Keys.USER_ID] ?: "" }
    val userHashFlow: Flow<String> = context.sessionDataStore.data.map { it[Keys.USER_HASH] ?: "" }
    val cityFlow: Flow<String> = context.sessionDataStore.data.map { it[Keys.CITY] ?: "" }
    val districtFlow: Flow<String> = context.sessionDataStore.data.map { it[Keys.DISTRICT] ?: "" }

    suspend fun saveLogin(userId: String, userHash: String) {
        val now = System.currentTimeMillis()
        context.sessionDataStore.edit { p ->
            p[Keys.USER_ID] = userId
            p[Keys.USER_HASH] = userHash
            p[Keys.LOGGED_AT] = now
        }
    }

    suspend fun setCity(city: String, district: String) {
        context.sessionDataStore.edit { p ->
            p[Keys.CITY] = city
            p[Keys.DISTRICT] = district
        }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { it.clear() }
    }
}
