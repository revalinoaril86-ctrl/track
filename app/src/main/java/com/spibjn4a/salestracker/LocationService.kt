package com.spibjn4a.salestracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

/**
 * Service yang berjalan di latar belakang (foreground service) dan mengirim
 * lokasi terkini ke track_api.php setiap UPDATE_INTERVAL_MS.
 *
 * Field yang dikirim (POST, x-www-form-urlencoded): username, latitude, longitude, accuracy
 * Sesuaikan nama field ini kalau format track_api.php Anda berbeda.
 */
class LocationService : Service() {

    companion object {
        var isRunning = false
        private const val CHANNEL_ID = "sales_tracker_channel"
        private const val NOTIFICATION_ID = 1
        private const val UPDATE_INTERVAL_MS = 15000L // kirim tiap 15 detik
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var prefs: SharedPreferences
    private val executor = Executors.newSingleThreadExecutor()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { sendLocationToServer(it) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        prefs = getSharedPreferences("sales_tracker_prefs", MODE_PRIVATE)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        isRunning = true
        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS
        ).build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // izin belum diberikan, service akan berhenti sendiri dari MainActivity
        }
    }

    private fun sendLocationToServer(location: Location) {
        val username = prefs.getString("username", "") ?: ""
        val serverUrl = prefs.getString("server_url", "") ?: ""
        if (username.isEmpty() || serverUrl.isEmpty()) return

        executor.execute {
            try {
                val url = URL(serverUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty(
                    "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8"
                )
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val params = listOf(
                    "username" to username,
                    "latitude" to location.latitude.toString(),
                    "longitude" to location.longitude.toString(),
                    "accuracy" to location.accuracy.toString()
                ).joinToString("&") { (k, v) ->
                    "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
                }

                conn.outputStream.use { it.write(params.toByteArray()) }
                conn.responseCode // trigger request
                conn.disconnect()
            } catch (e: Exception) {
                // koneksi gagal, akan dicoba lagi di update berikutnya
            }
        }
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sales Tracker aktif")
            .setContentText("Mengirim lokasi secara berkala")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Sales Tracker", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
