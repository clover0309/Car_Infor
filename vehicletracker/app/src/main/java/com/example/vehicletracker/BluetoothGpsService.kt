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
import android.os.Handler
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
    private val handler = Handler(Looper.getMainLooper())
    private var pendingResetRunnable: Runnable? = null
    // 현재 연결 세션에서 OFF 상태에 마지막 위치를 포함해 전송했는지 여부
    private var hasSentOffLocationForCurrentDisconnect: Boolean = false
    // 위치 콜백이 없을 때도 주기적으로 상태를 전송하기 위한 타이커
    private var statusTicker: Runnable? = null

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

            // 3초 주기의 상태 전송 타이커 시작 (연결/시동 상태를 내부에서 체크)
            statusTicker = object : Runnable {
                override fun run() {
                    try {
                        if (ignitionOn && currentDeviceAddress != null) {
                            sendUpdateToBackend()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "statusTicker 실행 중 예외", e)
                    } finally {
                        handler.postDelayed(this, 3000)
                    }
                }
            }
            handler.postDelayed(statusTicker!!, 3000)
        } catch (e: Exception) {
            Log.e(TAG, "onCreate crash", e)
        }
    }

    /**
     * 최초 ON 전송 시 lastKnownLocation을 우선 확보하여 위치 포함을 보장
     */
    @android.annotation.SuppressLint("MissingPermission")
    private fun sendFirstOnWithLastKnownLocation() {
        try {
            // 연결/시동 상태 확인
            val isConnected = currentDeviceAddress != null
            if (!ignitionOn || !isConnected) {
                sendUpdateToBackend()
                return
            }

            // 이미 최근 위치가 있으면 바로 전송
            if (lastLocation != null) {
                sendUpdateToBackend()
                return
            }

            // 1) lastLocation 시도
            fusedLocationClient.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        lastLocation = loc
                        lastSpeed = loc.speed
                    }
                    // lastLocation 결과와 상관없이 우선 전송
                    sendUpdateToBackend()

                    // 2) 추가로 getCurrentLocation으로 최신 위치를 한 번 더 갱신 시도 후 전송 (옵셔널)
                    try {
                        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                            .addOnSuccessListener { cur ->
                                if (cur != null) {
                                    lastLocation = cur
                                    lastSpeed = cur.speed
                                    // 최신 위치로 한 번 더 전송하여 지도 상 즉시 보정
                                    sendUpdateToBackend()
                                }
                            }
                            .addOnFailureListener { _ -> /* 무시 */ }
                    } catch (_: Exception) { /* 무시 */ }
                }
                .addOnFailureListener {
                    // 실패 시에도 전송은 진행
                    sendUpdateToBackend()
                }
        } catch (e: Exception) {
            Log.w(TAG, "sendFirstOnWithLastKnownLocation 중 예외", e)
            sendUpdateToBackend()
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
                
                // 중요: 현재 디바이스 ID 기록 (로그용)
                val currentId = deviceId
                Log.i(TAG, "서비스 종료 - 디바이스 ID($currentId)로 OFF 상태 전송")
                
                // 서비스 종료 시 확실하게 OFF 상태 전송 (마지막 위치 포함)
                sendUpdateToBackend(currentId, includeLastLocationOnOff = true)
                
                // 약간의 지연 후 한 번 더 OFF 상태 전송 (네트워크 문제로 첫 번째 요청이 실패할 경우를 대비)
                try {
                    // 서비스 종료 중이므로 별도 스레드 없이 바로 실행
                    Thread.sleep(500) // 0.5초 지연 (서비스 종료 중이므로 짧게 설정)
                    sendUpdateToBackend(currentId) // 한 번 더 OFF 상태 전송(위치 미포함)
                    Log.i(TAG, "서비스 종료 - 추가 OFF 상태 전송 완료")
                    
                    // 한 번 더 지연 후 세 번째 OFF 상태 전송 (확실한 전송을 위해)
                    Thread.sleep(500) // 0.5초 추가 지연
                    sendUpdateToBackend(currentId) // 세 번째 OFF 상태 전송(위치 미포함)
                    Log.i(TAG, "서비스 종료 - 세 번째 OFF 상태 전송 완료")
                } catch (e: Exception) {
                    Log.e(TAG, "추가 OFF 상태 전송 중 오류", e)
                }
            }
            // 대기 중인 ANDROID_ID 복원 작업이 있으면 취소 (서비스 종료 시 오동작/누수 방지)
            pendingResetRunnable?.let { handler.removeCallbacks(it) }
            pendingResetRunnable = null
            // 주기 타이커 해제
            statusTicker?.let { handler.removeCallbacks(it) }
            statusTicker = null
            
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
            // 재연결 시 ANDROID_ID 복원 예약을 취소
            pendingResetRunnable?.let { handler.removeCallbacks(it) }
            pendingResetRunnable = null
            
            ignitionOn = true
            // 새 연결 세션 시작: OFF 위치 전송 플래그 초기화
            hasSentOffLocationForCurrentDisconnect = false
            
            // 즉시 연결 상태 전송(최초 1회 lastKnownLocation 우선 포함)
            sendFirstOnWithLastKnownLocation()
            
            // 재연결 시 위치 업데이트 재시작 보장
            try {
                fusedLocationClient.requestLocationUpdates(
                    LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                        .setMinUpdateIntervalMillis(1000L)
                        .build(),
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (_: Exception) { /* 이미 시작된 경우 무시 */ }

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
        
        // 중요: 연결 해제 시에도 블루투스 디바이스 ID를 유지하여 OFF 상태 전송
        // 디바이스 ID를 변경하기 전에 현재 디바이스 ID로 OFF 상태 전송
        val originalDeviceId = deviceId
        ignitionOn = false
        
        // 연결 해제 시 확실하게 OFF 상태 전송 (여러 번 전송하여 확실히 반영되도록 함)
        Log.i(TAG, "블루투스 연결 해제 - 디바이스 ID($originalDeviceId)로 OFF 상태 전송 (마지막 위치 포함)")
        // 첫 번째 OFF 전송에만 마지막 위치 포함 (sendUpdateToBackend 내부 가드 적용)
        sendUpdateToBackend(originalDeviceId, includeLastLocationOnOff = true)
        
        // 약간의 지연 후 한 번 더 OFF 상태 전송 (네트워크 문제로 첫 번째 요청이 실패할 경우를 대비)
        Thread {
            try {
                Thread.sleep(1000) // 1초 지연
                sendUpdateToBackend(originalDeviceId) // 한 번 더 OFF 상태 전송(위치 미포함)
                Log.i(TAG, "블루투스 연결 해제 - 추가 OFF 상태 전송 완료")
                
                // 한 번 더 지연 후 세 번째 OFF 상태 전송 (확실한 전송을 위해)
                Thread.sleep(2000) // 2초 추가 지연
                sendUpdateToBackend(originalDeviceId) // 세 번째 OFF 상태 전송(위치 미포함)
                Log.i(TAG, "블루투스 연결 해제 - 세 번째 OFF 상태 전송 완료")
            } catch (e: Exception) {
                Log.e(TAG, "추가 OFF 상태 전송 중 오류", e)
            }
        }.start()
        
        // 모든 OFF 상태 전송이 완료된 후 ANDROID_ID로 되돌리기 위해 약간의 지연 추가
        // 기존 예약이 있으면 취소 후 재등록
        pendingResetRunnable?.let { handler.removeCallbacks(it) }
        pendingResetRunnable = Runnable {
            try {
                // 재연결이 감지되면 복원 취소
                if (ignitionOn || currentDeviceAddress != null) {
                    Log.i(TAG, "디바이스 ID 초기화 취소: 재연결 또는 연결 상태 감지")
                    return@Runnable
                }
                deviceId = android.provider.Settings.Secure.getString(
                    contentResolver, 
                    android.provider.Settings.Secure.ANDROID_ID
                )
                Log.i(TAG, "디바이스 ID 초기화: $deviceId (ANDROID_ID)")
            } catch (e: Exception) {
                Log.e(TAG, "디바이스 ID 초기화 중 오류", e)
            } finally {
                pendingResetRunnable = null
            }
        }
        handler.postDelayed(pendingResetRunnable!!, 3000)
        
        // 현재 추적 중인 기기 정보 부분 초기화 (디바이스 이름은 유지)
        currentDevice = null
        currentDeviceAddress = null
        
        // 중요: currentDeviceName은 초기화하지 않고 유지
        // 이렇게 하면 연결 해제 후에도 마지막으로 연결된 디바이스 이름을 계속 사용
        
        Log.i(TAG, "차량 추적 중지: $deviceName (디바이스 이름 유지: $currentDeviceName)")

        // 연결 해제 시 위치 업데이트 중단 (주기 전송 완전 차단)
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            Log.w(TAG, "위치 업데이트 중단 중 예외", e)
        }
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
            // 중요: 현재 디바이스 정보 유지하면서 OFF 상태 전송
            val originalDeviceId = deviceId
            currentDeviceName = deviceName
            currentDeviceAddress = deviceAddress
            ignitionOn = false
            
            // 블루투스 꺼짐 시 확실하게 OFF 상태 전송
            Log.i(TAG, "블루투스 꺼짐 - 디바이스 ID($originalDeviceId)로 OFF 상태 전송 (마지막 위치 포함)")
            // 첫 번째 OFF 전송에만 마지막 위치 포함 (sendUpdateToBackend 내부 가드 적용)
            sendUpdateToBackend(originalDeviceId, includeLastLocationOnOff = true)
            
            // 약간의 지연 후 한 번 더 OFF 상태 전송
            Thread {
                try {
                    Thread.sleep(1000) // 1초 지연
                    sendUpdateToBackend(originalDeviceId) // 한 번 더 OFF 상태 전송(위치 미포함)
                    Log.i(TAG, "블루투스 꺼짐 - 추가 OFF 상태 전송 완료")
                    
                    // 한 번 더 지연 후 세 번째 OFF 상태 전송 (확실한 전송을 위해)
                    Thread.sleep(2000) // 2초 추가 지연
                    sendUpdateToBackend(originalDeviceId) // 세 번째 OFF 상태 전송(위치 미포함)
                    Log.i(TAG, "블루투스 꺼짐 - 세 번째 OFF 상태 전송 완료")
                } catch (e: Exception) {
                    Log.e(TAG, "추가 OFF 상태 전송 중 오류", e)
                }
            }.start()
        }
        
        // 모든 OFF 상태 전송이 완료된 후 ANDROID_ID로 되돌리기 위해 약간의 지연 추가
        // 기존 예약이 있으면 취소 후 재등록, 재연결/연결 유지 시 복원 스킵
        pendingResetRunnable?.let { handler.removeCallbacks(it) }
        pendingResetRunnable = Runnable {
            try {
                if (ignitionOn || currentDeviceAddress != null) {
                    Log.i(TAG, "디바이스 ID 초기화 취소: 재연결 또는 연결 상태 감지(블루투스 꺼짐 경로)")
                    return@Runnable
                }
                deviceId = android.provider.Settings.Secure.getString(
                    contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                )
                Log.i(TAG, "디바이스 ID 초기화: $deviceId (ANDROID_ID)")
            } catch (e: Exception) {
                Log.e(TAG, "디바이스 ID 초기화 중 오류", e)
            } finally {
                pendingResetRunnable = null
            }
        }
        handler.postDelayed(pendingResetRunnable!!, 3000)
        
        // 현재 추적 중인 기기 정보 부분 초기화 (디바이스 이름은 유지)
        currentDevice = null
        currentDeviceAddress = null
        
        // 중요: currentDeviceName은 초기화하지 않고 유지
        // 이렇게 하면 블루투스가 꺼진 후에도 마지막으로 연결된 디바이스 이름을 계속 사용
        
        Log.i(TAG, "블루투스 꺼짐으로 인한 차량 추적 중지 (디바이스 이름 유지: $currentDeviceName)")

        // 블루투스 어댑터 꺼짐 시 위치 업데이트 중단
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            Log.w(TAG, "위치 업데이트 중단 중 예외", e)
        }
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
                
                // 차량이 실제로 연결되어 있고(주소 존재) 시동이 켜진 상태일 때만 전송
                // currentDeviceName 은 OFF 전송을 위해 유지되므로 주소를 기준으로 연결 여부를 판단
                if (ignitionOn && currentDeviceAddress != null) {
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
    private fun sendUpdateToBackend(overrideDeviceId: String? = null, includeLastLocationOnOff: Boolean = false) {
        // 실제 연결 여부(주소 존재)를 기반으로 엔진상태를 계산하여, 연결이 없으면 어떤 경우에도 ON을 보내지 않도록 보장
        val isConnected = currentDeviceAddress != null
        val engineStatus = if (ignitionOn && isConnected) "ON" else "OFF"
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        
        // 중요: 블루투스 연결 해제 시에도 마지막으로 연결된 디바이스 이름을 유지
        // 저장된 디바이스 이름이 있으면 사용, 없으면 Unknown Device
        val btName = currentDeviceName ?: "Unknown Device"
        
        // OFF에서 마지막 위치를 포함할지 여부(세션별 1회로 제한)
        val willIncludeOffLocation = engineStatus == "OFF" && includeLastLocationOnOff && !hasSentOffLocationForCurrentDisconnect && lastLocation != null
        if (willIncludeOffLocation) {
            hasSentOffLocationForCurrentDisconnect = true
        }

        val dto = com.example.vehicletracker.api.VehicleStatusDto(
            deviceId = overrideDeviceId ?: deviceId,
            bluetoothDevice = btName,
            engineStatus = engineStatus,
            speed = if (engineStatus == "ON") lastSpeed else 0f, // OFF 상태일 때는 속도 0
            timestamp = timestamp,
            location = if ((engineStatus == "ON" && lastLocation != null) ||
                (engineStatus == "OFF" && willIncludeOffLocation)) {
                com.example.vehicletracker.api.VehicleLocation(
                    latitude = lastLocation?.latitude,
                    longitude = lastLocation?.longitude
                )
            } else {
                null // OFF 상태일 때는 기본적으로 위치 정보 없음 (첫 전송에 한해 포함 가능)
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