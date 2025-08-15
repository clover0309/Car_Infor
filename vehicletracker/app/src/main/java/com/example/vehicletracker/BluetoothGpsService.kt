package com.example.vehicletracker

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
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
    // 기본값으로 ANDROID_ID를 사용하지만, 블루투스 연결 시 디바이스 주소로 대체
    private var deviceId: String = ""
    private lateinit var bluetoothReceiver: BluetoothStateReceiver

    companion object {
        private const val TAG = "BluetoothGpsService"
        const val ACTION_VEHICLE_BLUETOOTH_CONNECTED = "com.example.vehicletracker.ACTION_VEHICLE_BLUETOOTH_CONNECTED"
        const val ACTION_VEHICLE_BLUETOOTH_DISCONNECTED = "com.example.vehicletracker.ACTION_VEHICLE_BLUETOOTH_DISCONNECTED"
        const val ACTION_BLUETOOTH_TURNED_OFF = "com.example.vehicletracker.ACTION_BLUETOOTH_TURNED_OFF"
    }

    override fun onCreate() {
        super.onCreate()
        try {
            Log.d(TAG, "onCreate called")
            
            // ANDROID_ID 초기화
            deviceId = android.provider.Settings.Secure.getString(
                contentResolver, 
                android.provider.Settings.Secure.ANDROID_ID
            )
            Log.d(TAG, "기본 디바이스 ID 초기화: $deviceId")
            
            bluetoothReceiver = BluetoothStateReceiver()
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            }
            registerReceiver(bluetoothReceiver, filter)
            
            // Initialize fusedLocationClient before using it
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            startForegroundServiceNotification()
            startLocationUpdates()
        } catch (e: Exception) {
            Log.e(TAG, "onCreate crash", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand called, action: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_VEHICLE_BLUETOOTH_CONNECTED -> {
                handleVehicleBluetoothConnected(intent)
            }
            ACTION_VEHICLE_BLUETOOTH_DISCONNECTED -> {
                handleVehicleBluetoothDisconnected(intent)
            }
            ACTION_BLUETOOTH_TURNED_OFF -> {
                handleBluetoothTurnedOff(intent)
            }
        }
        
        return START_STICKY
    }

    override fun onDestroy() {
        try {
            // 서비스 종료 시 엔진 OFF 상태 전송
            if (ignitionOn) {
                Log.i(TAG, "[시동 OFF 감지] 서비스 종료로 인한 엔진 OFF 상태 전송, 기기: ${currentDeviceName ?: "Unknown"}")
                ignitionOn = false
                sendUpdateToBackend()
            }
            
            unregisterReceiver(bluetoothReceiver)
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            Log.e(TAG, "onDestroy 중 오류", e)
        }
        
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * 차량 블루투스 연결 처리
     */
    @android.annotation.SuppressLint("MissingPermission")
    private fun handleVehicleBluetoothConnected(intent: Intent) {
        val deviceName = intent.getStringExtra("bluetooth_device_name")
        val deviceAddress = intent.getStringExtra("bluetooth_device_address")
        
        Log.i(TAG, "차량 블루투스 연결: $deviceName ($deviceAddress)")
        
        if (deviceName != null && deviceAddress != null) {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val device = bluetoothManager.adapter?.bondedDevices?.find { 
                it.address == deviceAddress 
            }
            
            currentDevice = device
            currentDeviceName = deviceName
            currentDeviceAddress = deviceAddress
            
            // 블루투스 디바이스 주소를 deviceId로 사용
            deviceId = deviceAddress
            Log.i(TAG, "디바이스 ID 업데이트: $deviceId (블루투스 주소)")
            
            ignitionOn = true
            
            // 즉시 연결 상태 전송
            sendUpdateToBackend()
            
            Log.i(TAG, "차량 추적 시작: $deviceName")
        }
    }

    /**
     * 차량 블루투스 연결 해제 처리
     */
    private fun handleVehicleBluetoothDisconnected(intent: Intent) {
        val deviceName = intent.getStringExtra("bluetooth_device_name")
        val deviceAddress = intent.getStringExtra("bluetooth_device_address")
        
        Log.i(TAG, "차량 블루투스 연결 해제: $deviceName ($deviceAddress)")
        
        // 즉시 OFF 상태 전송 (연결 해제 전 디바이스 ID로 마지막 상태 전송)
        ignitionOn = false
        sendUpdateToBackend()
        
        // ANDROID_ID로 되돌리기
        deviceId = android.provider.Settings.Secure.getString(
            contentResolver, 
            android.provider.Settings.Secure.ANDROID_ID
        )
        Log.i(TAG, "디바이스 ID 초기화: $deviceId (ANDROID_ID)")
        
        // 현재 추적 중인 기기 정보 초기화
        currentDevice = null
        currentDeviceName = null
        currentDeviceAddress = null
        
        Log.i(TAG, "차량 추적 중지: $deviceName")
    }

    /**
     * 블루투스 어댑터 꺼짐 처리
     */
    private fun handleBluetoothTurnedOff(intent: Intent) {
        val deviceName = intent.getStringExtra("bluetooth_device_name")
        val deviceAddress = intent.getStringExtra("bluetooth_device_address")
        
        Log.i(TAG, "블루투스 어댑터 꺼짐으로 인한 연결 해제: $deviceName ($deviceAddress)")
        
        // 블루투스가 꺼진 경우에도 OFF 상태 전송
        if (ignitionOn) {
            currentDeviceName = deviceName
            currentDeviceAddress = deviceAddress
            ignitionOn = false
            sendUpdateToBackend()
        }
        
        // ANDROID_ID로 되돌리기
        deviceId = android.provider.Settings.Secure.getString(
            contentResolver, 
            android.provider.Settings.Secure.ANDROID_ID
        )
        Log.i(TAG, "디바이스 ID 초기화: $deviceId (ANDROID_ID)")
        
        // 현재 추적 중인 기기 정보 초기화
        currentDevice = null
        currentDeviceName = null
        currentDeviceAddress = null
        
        Log.i(TAG, "블루투스 꺼짐으로 인한 차량 추적 중지")
    }

    private fun startForegroundServiceNotification() {
        val channelId = "vehicle_tracker_channel"
        val channelName = "Vehicle Tracker Service"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                channelId, 
                channelName, 
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)
        }
        
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            notificationIntent, 
            PendingIntent.FLAG_IMMUTABLE
        )
        
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
                
                // 차량이 연결되어 있고 시동이 켜진 상태일 때만 전송
                if (ignitionOn && currentDeviceName != null) {
                    sendUpdateToBackend()
                }
            }
        }
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .build()
            
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == 
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest, 
                locationCallback, 
                Looper.getMainLooper()
            )
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun sendUpdateToBackend() {
        val engineStatus = if (ignitionOn) "ON" else "OFF"
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val btName = currentDevice?.name ?: currentDeviceName ?: "Unknown Device"
        
        val dto = com.example.vehicletracker.api.VehicleStatusDto(
            deviceId = deviceId,
            bluetoothDevice = btName,
            engineStatus = engineStatus,
            speed = if (ignitionOn) lastSpeed else 0f, // OFF 상태일 때는 속도 0
            timestamp = timestamp,
            location = if (ignitionOn && lastLocation != null) {
                com.example.vehicletracker.api.VehicleLocation(
                    latitude = lastLocation?.latitude,
                    longitude = lastLocation?.longitude
                )
            } else {
                null // OFF 상태일 때는 위치 정보 없음
            }
        )
        
        Log.d(TAG, "API Send: $dto")
        
        Thread {
            try {
                val response = com.example.vehicletracker.api.RetrofitInstance.api
                    .sendVehicleStatus(dto).execute()
                    
                if (response.isSuccessful) {
                    Log.i(TAG, "상태 전송 성공: ${response.code()}")
                } else {
                    Log.e(TAG, "상태 전송 실패: ${response.code()} ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "API 통신 오류", e)
            }
        }.start()
    }
}