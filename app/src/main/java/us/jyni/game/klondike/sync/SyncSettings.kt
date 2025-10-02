package us.jyni.game.klondike.sync

import android.content.Context
import java.util.UUID

object SyncSettings {
    private const val PREFS = "sync_prefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_UPLOAD_ENABLED = "upload_enabled"
    private const val KEY_BASE_URL = "base_url"

    private const val DEFAULT_BASE_URL = "https://api.example.com/"

    fun getUserId(context: Context): String {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = sp.getString(KEY_USER_ID, null)
        if (!existing.isNullOrEmpty()) return existing
        val generated = "usr_" + UUID.randomUUID().toString().replace("-", "").take(16)
        sp.edit().putString(KEY_USER_ID, generated).apply()
        return generated
    }

    fun isUploadEnabled(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_UPLOAD_ENABLED, false)
    }

    fun setUploadEnabled(context: Context, enabled: Boolean) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_UPLOAD_ENABLED, enabled).apply()
    }

    fun getBaseUrl(context: Context): String {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return sp.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun setBaseUrl(context: Context, url: String) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_BASE_URL, url).apply()
    }
}
