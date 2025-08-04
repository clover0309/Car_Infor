package com.example.vehicletracker

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import java.text.SimpleDateFormat
import java.util.*

class BluetoothGpsService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentDevice: BluetoothDevice? = null
    private var ignitionOn: Boolean = false
    private var lastSpeed: Float = 0f
    private var lastLocation: Location? = null
    private val deviceId: String by lazy { android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID) }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        registerBluetoothReceiver()
        startForegroundServiceNotification()
        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Optionally handle intent from Bixby Routine or others
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundServiceNotification() {
        val channelId = "vehicle_tracker_channel"
        val channelName = "Vehicle Tracker Service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)
        }
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("차량 정보 추적 서비스 실행중")
            .setContentText("블루투스 및 GPS 상태를 모니터링합니다.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)
    }

    private fun registerBluetoothReceiver() {
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        registerReceiver(bluetoothReceiver, filter)
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            val device = intent?.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            when (action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    currentDevice = device
                    ignitionOn = true
                    sendUpdateToBackend()
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    ignitionOn = false
                    sendUpdateToBackend()
                    currentDevice = null
                }
            }
        }
    }

    private fun startLocationUpdates() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                lastLocation = location
                lastSpeed = location?.speed ?: 0f
                if (ignitionOn) {
                    sendUpdateToBackend()
                }
            }
        }
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .build()
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun sendUpdateToBackend() {
    val engineStatus = if (ignitionOn) "ON" else "OFF"
    val timestamp = java.time.Instant.now().toString()
    val dto = com.example.vehicletracker.api.VehicleStatusDto(
        deviceId = deviceId,
        bluetoothDevice = currentDevice?.name,
        engineStatus = engineStatus,
        speed = lastSpeed,
        timestamp = timestamp,
        location = com.example.vehicletracker.api.VehicleLocation(
            latitude = lastLocation?.latitude,
            longitude = lastLocation?.longitude
        )
    )
    android.util.Log.d("VehicleTracker", "API Send: $dto")
    Thread {
        try {
            val response = com.example.vehicletracker.api.RetrofitInstance.api.sendVehicleStatus(dto).execute()
            if (response.isSuccessful) {
                android.util.Log.i("VehicleTracker", "전송 성공: ${response.code()}")
            } else {
                android.util.Log.e("VehicleTracker", "전송 실패: ${response.code()} ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            android.util.Log.e("VehicleTracker", "API 통신 오류", e)
        }
    }.start()
}


}
