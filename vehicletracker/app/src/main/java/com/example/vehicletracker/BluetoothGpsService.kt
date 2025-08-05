package com.example.vehicletracker

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import com.example.vehicletracker.receiver.BluetoothStateReceiver
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
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class BluetoothGpsService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentDevice: BluetoothDevice? = null
    private var currentDeviceName: String? = null
    private var currentDeviceAddress: String? = null
    private var ignitionOn: Boolean = false
    private var lastSpeed: Float = 0f
    private var lastLocation: Location? = null
    private val deviceId: String by lazy { android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID) 
    }
    private lateinit var bluetoothReceiver: BluetoothStateReceiver


    override fun onCreate() {
        super.onCreate()
        try {
            Log.d("BlueToothGpsService", "onCreate called");
            bluetoothReceiver = BluetoothStateReceiver()
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            }
            registerReceiver(bluetoothReceiver, filter)
            // Initialize fusedLocationClient before using it
            fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)
            startForegroundServiceNotification()
            startLocationUpdates()
        } catch (e: Exception) {
            Log.e("BlueToothGpsService", "onCreate crash", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("BlueToothGpsService", "onStartCommand called, action: ${intent?.action}")
        Log.i("BlueToothGpsService", "ACTION_VEHICLE_BLUETOOTH_CONNECTED 수신: ...")
        // 커스텀 인텐트로 차량 블루투스 연결 이벤트 처리
        if (intent != null && intent.action == "com.example.vehicletracker.ACTION_VEHICLE_BLUETOOTH_CONNECTED") {
            val deviceName = intent.getStringExtra("bluetooth_device_name")
            val deviceAddress = intent.getStringExtra("bluetooth_device_address")
            Log.i("BlueToothGpsService", "ACTION_VEHICLE_BLUETOOTH_CONNECTED 수신: $deviceName ($deviceAddress)")
            if (deviceName != null && deviceAddress != null) {
    val device = BluetoothAdapter.getDefaultAdapter()?.bondedDevices?.find { it.address == deviceAddress }
    currentDevice = device
    currentDeviceName = deviceName
    currentDeviceAddress = deviceAddress
    ignitionOn = true

    // 신규 DeviceID 감지 → 존재 여부 확인 → 사용자 입력 → 등록 → 위치 전송
    val ctx = this
    com.example.vehicletracker.api.RetrofitInstance.api.checkDeviceExists(deviceId)
        .enqueue(object : retrofit2.Callback<Boolean> {
            override fun onResponse(call: retrofit2.Call<Boolean>, response: retrofit2.Response<Boolean>) {
                val exists = response.body() ?: false
                if (!exists) {
                    // 사용자에게 이름 입력받아 등록
                    com.example.vehicletracker.util.DevicePromptUtil.promptDeviceName(ctx) { userInputName ->
                        val req = com.example.vehicletracker.api.DeviceRegisterRequest(deviceId, userInputName)
                        com.example.vehicletracker.api.RetrofitInstance.api.registerDevice(req)
                            .enqueue(object : retrofit2.Callback<Void> {
                                override fun onResponse(call: retrofit2.Call<Void>, response: retrofit2.Response<Void>) {
                                    // 등록 성공 후 위치 전송
                                    sendUpdateToBackend()
                                }
                                override fun onFailure(call: retrofit2.Call<Void>, t: Throwable) {
                                    android.util.Log.e("VehicleTracker", "기기 등록 실패", t)
                                }
                            })
                    }
                } else {
                    // 이미 등록된 경우 바로 위치 전송
                    sendUpdateToBackend()
                }
            }
            override fun onFailure(call: retrofit2.Call<Boolean>, t: Throwable) {
                android.util.Log.e("VehicleTracker", "기기 존재 확인 실패", t)
            }
        })
}
        }
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
    val timestamp = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul")).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd aHH:mm:ss"))
    val btName = currentDevice?.name ?: currentDeviceName ?: "Unknown Device"
    val latitude = lastLocation?.latitude
    val longitude = lastLocation?.longitude
    if (latitude == null || longitude == null) {
        android.util.Log.e("VehicleTracker", "위치 정보 없음, 전송 생략")
        return
    }
    val speedInt = lastSpeed.toInt()
val dto = com.example.vehicletracker.api.LocationRequest(
    deviceId = deviceId,
    deviceName = btName,
    latitude = latitude,
    longitude = longitude,
    timestamp = timestamp,
    speed = speedInt
)
    android.util.Log.d("VehicleTracker", "위치 전송: $dto")
    Thread {
        try {
            val response = com.example.vehicletracker.api.RetrofitInstance.api.sendLocation(dto).execute()
            if (response.isSuccessful) {
                android.util.Log.i("VehicleTracker", "위치 전송 성공: ${response.code()}")
            } else {
                android.util.Log.e("VehicleTracker", "위치 전송 실패: ${response.code()} ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            android.util.Log.e("VehicleTracker", "위치 API 통신 오류", e)
        }
    }.start()
}


}
