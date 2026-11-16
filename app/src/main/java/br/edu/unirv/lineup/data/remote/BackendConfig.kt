package br.edu.unirv.lineup.data.remote

import android.content.Context
import android.content.SharedPreferences

class BackendConfig(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, DEFAULT_URL) ?: DEFAULT_URL
        set(value) {
            val normalized = value.trim().removeSuffix("/")
            prefs.edit().putString(KEY_BASE_URL, normalized).apply()
        }

    var isMockMode: Boolean
        get() = prefs.getBoolean(KEY_MOCK_MODE, true) // Padrão MOCADO para entrega do front-end
        set(value) = prefs.edit().putBoolean(KEY_MOCK_MODE, value).apply()

    companion object {
        private const val PREFS_NAME = "lineup_backend_prefs"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_MOCK_MODE = "mock_mode"

        // URLs predefinidas sugeridas no enunciado RF002
        const val URL_EMULATOR = "http://10.0.2.2:8080"
        const val URL_LOCALHOST = "http://localhost:8080"
        const val URL_WIFI_SAMPLE = "http://192.168.0.21:8080"
        const val DEFAULT_URL = URL_EMULATOR
    }
}
