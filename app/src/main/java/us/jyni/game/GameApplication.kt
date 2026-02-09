package us.jyni

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import java.util.Locale

class GameApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply saved language preference
        applySavedLanguage()
    }
    
    private fun applySavedLanguage() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val languageCode = prefs.getString("language", "ko") ?: "ko"
        
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applySavedLanguage()
    }
}