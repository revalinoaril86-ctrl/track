package com.spibjn4a.salestracker

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        // URL server dikunci di sini, tidak bisa diubah dari tampilan app.
        const val SERVER_URL = "https://script.google.com/macros/s/AKfycbyVOSoXyG89YQep__GxdLbZnHb5ZQAqNCKYimfgSJqh4U7fhoFBD0QTDeJOIGHjomsU/exec"
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var etUsername: EditText
    private lateinit var tvStatus: TextView

    private val permissionsRequestCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("sales_tracker_prefs", MODE_PRIVATE)
        etUsername = findViewById(R.id.etUsername)
        tvStatus = findViewById(R.id.tvStatus)

        etUsername.setText(prefs.getString("username", ""))

        // Selalu pastikan server_url tersimpan sesuai konstanta yang dikunci,
        // menimpa nilai lama apapun yang mungkin masih tersimpan dari versi sebelumnya.
        prefs.edit().putString("server_url", SERVER_URL).apply()

        if (LocationService.isRunning) {
            tvStatus.text = "Status: aktif mengirim lokasi"
        }

        findViewById<Button>(R.id.btnStart).setOnClickListener { onStartClicked() }
        findViewById<Button>(R.id.btnStop).setOnClickListener { onStopClicked() }
    }

    private fun onStartClicked() {
        val username = etUsername.text.toString().trim()

        if (username.isEmpty()) {
            Toast.makeText(this, "Isi nama sales dulu", Toast.LENGTH_SHORT).show()
            return
        }

        prefs.edit()
            .putString("username", username)
            .putString("server_url", SERVER_URL)
            .apply()

        if (hasAllPermissions()) {
            startTrackingService()
        } else {
            requestPermissions()
        }
    }

    private fun onStopClicked() {
        stopService(Intent(this, LocationService::class.java))
        tvStatus.text = "Status: berhenti"
    }

    private fun startTrackingService() {
        val intent = Intent(this, LocationService::class.java)
        ContextCompat.startForegroundService(this, intent)
        tvStatus.text = "Status: aktif mengirim lokasi"
    }

    private fun hasAllPermissions(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val background = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        return fine && background && notif
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), permissionsRequestCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionsRequestCode) {
            val fineGranted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED

            if (fineGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Minta izin lokasi latar belakang secara terpisah (wajib di Android 10+)
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    permissionsRequestCode + 1
                )
            } else if (fineGranted) {
                startTrackingService()
            } else {
                Toast.makeText(this, "Izin lokasi diperlukan untuk tracking", Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == permissionsRequestCode + 1) {
            startTrackingService()
        }
    }
}
