package us.jyni

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import android.widget.EditText
import us.jyni.R
import us.jyni.game.klondike.ui.screens.GameActivity
import us.jyni.game.klondike.sync.SyncSettings
import us.jyni.game.klondike.sync.UploadScheduler
import us.jyni.game.klondike.sync.JsonlFileRepository

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Debug controls (optional): toggle upload + trigger once
        val panel = findViewById<android.view.View>(R.id.debug_panel)
        if (!BuildConfig.DEBUG) {
            panel?.visibility = android.view.View.GONE
        }
        val status = findViewById<TextView>(R.id.debug_status)
        val toggle = findViewById<Switch>(R.id.toggle_upload)
        val trigger = findViewById<Button>(R.id.btn_trigger_upload)
        val editBaseUrl = findViewById<Button>(R.id.btn_edit_base_url)
        val refreshPending = findViewById<Button>(R.id.btn_refresh_pending)
        if (BuildConfig.DEBUG && status != null && toggle != null && trigger != null && editBaseUrl != null && refreshPending != null) {
            val userId = SyncSettings.getUserId(this)
            fun renderStatus() {
                val repo = JsonlFileRepository(this)
                val count = repo.pendingCount()
                status.text = "userId=$userId\nbaseUrl=${SyncSettings.getBaseUrl(this)}\npending=$count"
            }
            renderStatus()
            toggle.isChecked = SyncSettings.isUploadEnabled(this)
            toggle.setOnCheckedChangeListener { _, isChecked ->
                SyncSettings.setUploadEnabled(this, isChecked)
                if (isChecked) UploadScheduler.schedulePeriodic(this)
            }
            trigger.setOnClickListener {
                UploadScheduler.triggerOnce(this)
            }
            editBaseUrl.setOnClickListener {
                val current = SyncSettings.getBaseUrl(this)
                val input = EditText(this)
                input.setText(current)
                AlertDialog.Builder(this)
                    .setTitle("Base URL")
                    .setView(input)
                    .setPositiveButton("Save") { d, _ ->
                        val newUrl = input.text?.toString()?.trim().orEmpty()
                        if (newUrl.isNotEmpty()) {
                            SyncSettings.setBaseUrl(this, if (newUrl.endsWith("/")) newUrl else "$newUrl/")
                            renderStatus()
                        }
                        d.dismiss()
                    }
                    .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                    .show()
            }
            refreshPending.setOnClickListener { renderStatus() }
        }

        // Start the game activity
        val intent = Intent(this, GameActivity::class.java)
        startActivity(intent)
        finish()
    }
}