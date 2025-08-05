package com.example.vehicletracker.receiver

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.vehicletracker.MainActivity
import com.example.vehicletracker.utils.NotificationHelper
import com.example.vehicletracker.api.VehicleStatusDto
import com.example.vehicletracker.api.VehicleLocation
import com.example.vehicletracker.api.RetrofitInstance
import java.text.SimpleDateFormat
import java.util.*

class BluetoothStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BluetoothStateReceiver"
        private const val PREFS_NAME = "VehicleTrackerPrefs"
        private const val KEY_LAST_CONNECTED_DEVICE_NAME = "last_connected_device_name"
        private const val KEY_LAST_CONNECTED_DEVICE_ADDRESS = "last_connected_device_address"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "블루투스 상태 변경 감지: $action")

        when (action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                handleBluetoothConnected(context, intent)
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                handleBluetoothDisconnected(context, intent)
            }
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                handleBluetoothStateChanged(context, intent)
            }
        }
    }

    /**
     * 블루투스 기기 연결 처리
     */
    private fun handleBluetoothConnected(context: Context, intent: Intent) {
        val device: BluetoothDevice? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
        val deviceName = device?.name ?: "Unknown Device"
        val deviceAddress = device?.address ?: "Unknown Address"

        Log.i(TAG, "블루투스 기기 연결됨: $deviceName ($deviceAddress)")

        // 차량 관련 블루투스 기기인지 확인
        if (isVehicleBluetoothDevice(deviceName, deviceAddress)) {
            Log.i(TAG, "차량 블루투스 기기 감지: $deviceName")

            // 연결된 기기 정보 저장
            saveLastConnectedDevice(context, deviceName, deviceAddress)

            // 알림 표시
            NotificationHelper.showBluetoothConnectedNotification(
                context,
                deviceName,
                "차량에 연결되었습니다. 앱을 시작합니다."
            )

            // BluetoothGpsService에 차량 연결 이벤트 전달
            try {
                val serviceIntent = Intent(context, com.example.vehicletracker.BluetoothGpsService::class.java).apply {
                    action = "com.example.vehicletracker.ACTION_VEHICLE_BLUETOOTH_CONNECTED"
                    putExtra("bluetooth_device_name", deviceName)
                    putExtra("bluetooth_device_address", deviceAddress)
                }
                Log.i(TAG, "서비스에 인텐트 전달 전: $deviceName ($deviceAddress)")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                Log.i(TAG, "BluetoothGpsService에 연결 이벤트 전달 완료")
            } catch (e: Exception) {
                Log.e(TAG, "BluetoothGpsService 인텐트 전달 실패", e)
            }

            // 앱 자동 실행
            launchVehicleTrackerApp(context, deviceName, deviceAddress)
        } else {
            Log.d(TAG, "차량이 아닌 블루투스 기기: $deviceName")
        }
    }

    /**
     * 블루투스 기기 연결 해제 처리
     */
    private fun handleBluetoothDisconnected(context: Context, intent: Intent) {
        val device: BluetoothDevice? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
        val deviceName = device?.name ?: "Unknown Device"
        val deviceAddress = device?.address ?: "Unknown Address"

        Log.i(TAG, "블루투스 기기 연결 해제됨: $deviceName ($deviceAddress)")

        if (isVehicleBluetoothDevice(deviceName, deviceAddress)) {
            Log.i(TAG, "차량 블루투스 기기 연결 해제: $deviceName")

            // 서버에 OFF 상태 전송
            sendEngineOffStatus(context, deviceName, deviceAddress)

            // 알림 표시
            NotificationHelper.showBluetoothDisconnectedNotification(
                context,
                deviceName,
                "차량 연결이 해제되었습니다."
            )

            // BluetoothGpsService에 연결 해제 이벤트 전달
            try {
                val serviceIntent = Intent(context, com.example.vehicletracker.BluetoothGpsService::class.java).apply {
                    action = "com.example.vehicletracker.ACTION_VEHICLE_BLUETOOTH_DISCONNECTED"
                    putExtra("bluetooth_device_name", deviceName)
                    putExtra("bluetooth_device_address", deviceAddress)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                Log.i(TAG, "BluetoothGpsService에 연결 해제 이벤트 전달 완료")
            } catch (e: Exception) {
                Log.e(TAG, "BluetoothGpsService 인텐트 전달 실패", e)
            }
        }
    }

    /**
     * 블루투스 어댑터 상태 변경 처리
     */
    private fun handleBluetoothStateChanged(context: Context, intent: Intent) {
        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)

        when (state) {
            BluetoothAdapter.STATE_OFF -> {
                Log.w(TAG, "블루투스가 꺼졌습니다")
                
                // 마지막으로 연결된 차량 기기가 있다면 OFF 상태 전송
                val (lastDeviceName, lastDeviceAddress) = getLastConnectedDevice(context)
                if (lastDeviceName != null && lastDeviceAddress != null) {
                    Log.i(TAG, "블루투스 꺼짐으로 인한 차량 연결 해제: $lastDeviceName")
                    sendEngineOffStatus(context, lastDeviceName, lastDeviceAddress)
                    
                    // BluetoothGpsService에 블루투스 꺼짐 이벤트 전달
                    try {
                        val serviceIntent = Intent(context, com.example.vehicletracker.BluetoothGpsService::class.java).apply {
                            action = "com.example.vehicletracker.ACTION_BLUETOOTH_TURNED_OFF"
                            putExtra("bluetooth_device_name", lastDeviceName)
                            putExtra("bluetooth_device_address", lastDeviceAddress)
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                        Log.i(TAG, "BluetoothGpsService에 블루투스 꺼짐 이벤트 전달 완료")
                    } catch (e: Exception) {
                        Log.e(TAG, "BluetoothGpsService 인텐트 전달 실패", e)
                    }
                }
                
                NotificationHelper.showBluetoothStateNotification(
                    context,
                    "블루투스 꺼짐",
                    "차량 연결을 위해 블루투스를 켜주세요"
                )
            }
            BluetoothAdapter.STATE_ON -> {
                Log.i(TAG, "블루투스가 켜졌습니다")
                NotificationHelper.showBluetoothStateNotification(
                    context,
                    "블루투스 켜짐",
                    "차량 블루투스 연결 대기 중"
                )
            }
            BluetoothAdapter.STATE_TURNING_OFF -> {
                Log.d(TAG, "블루투스가 꺼지는 중...")
            }
            BluetoothAdapter.STATE_TURNING_ON -> {
                Log.d(TAG, "블루투스가 켜지는 중...")
            }
        }
    }

    /**
     * 엔진 OFF 상태를 서버에 전송
     */
    private fun sendEngineOffStatus(context: Context, deviceName: String, deviceAddress: String) {
        Thread {
            try {
                val deviceId = android.provider.Settings.Secure.getString(
                    context.contentResolver, 
                    android.provider.Settings.Secure.ANDROID_ID
                )
                
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                val dto = VehicleStatusDto(
                    deviceId = deviceId,
                    bluetoothDevice = deviceName,
                    engineStatus = "OFF",
                    speed = 0f,
                    timestamp = timestamp,
                    location = null // 연결 해제 시에는 위치 정보 없음
                )
                
                Log.d(TAG, "엔진 OFF 상태 전송: $dto")
                
                val response = RetrofitInstance.api.sendVehicleStatus(dto).execute()
                if (response.isSuccessful) {
                    Log.i(TAG, "엔진 OFF 상태 전송 성공: ${response.code()}")
                } else {
                    Log.e(TAG, "엔진 OFF 상태 전송 실패: ${response.code()} ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "엔진 OFF 상태 전송 중 오류", e)
            }
        }.start()
    }

    /**
     * 마지막으로 연결된 기기 정보 저장
     */
    private fun saveLastConnectedDevice(context: Context, deviceName: String, deviceAddress: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_LAST_CONNECTED_DEVICE_NAME, deviceName)
            putString(KEY_LAST_CONNECTED_DEVICE_ADDRESS, deviceAddress)
            apply()
        }
        Log.d(TAG, "마지막 연결 기기 저장: $deviceName ($deviceAddress)")
    }

    /**
     * 마지막으로 연결된 기기 정보 조회
     */
    private fun getLastConnectedDevice(context: Context): Pair<String?, String?> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val deviceName = prefs.getString(KEY_LAST_CONNECTED_DEVICE_NAME, null)
        val deviceAddress = prefs.getString(KEY_LAST_CONNECTED_DEVICE_ADDRESS, null)
        return Pair(deviceName, deviceAddress)
    }

    /**
     * 차량 관련 블루투스 기기인지 판단
     */
    private fun isVehicleBluetoothDevice(deviceName: String, deviceAddress: String): Boolean {
        val vehicleKeywords = listOf(
            "car", "audio", "stereo", "multimedia", "infotainment",
            "hyundai", "kia", "samsung", "genesis", "bmw", "audi",
            "mercedes", "toyota", "honda", "nissan", "lexus",
            "차량", "오디오", "내비게이션", "카오디오"
        )

        val lowerDeviceName = deviceName.lowercase()

        // 테스트 모드: 모든 블루투스 기기를 차량으로 인식 (개발/테스트용)
        val isTestMode = true // 실제 배포시에는 false로 변경
        if (isTestMode) {
            Log.d(TAG, "테스트 모드: 모든 블루투스 기기를 차량으로 인식")
            return true
        }

        // 차량 관련 키워드 검사
        for (keyword in vehicleKeywords) {
            if (lowerDeviceName.contains(keyword)) {
                Log.d(TAG, "차량 키워드 발견: $keyword in $deviceName")
                return true
            }
        }

        // MAC 주소 기반 차량 제조사 검사 (일부 예시)
        val vehicleManufacturerPrefixes = listOf(
            "00:16:A4", // Samsung (차량용 시스템)
            "AC:5F:3E", // Apple CarPlay
            "00:1B:DC", // 현대/기아
        )

        for (prefix in vehicleManufacturerPrefixes) {
            if (deviceAddress.startsWith(prefix, ignoreCase = true)) {
                Log.d(TAG, "차량 제조사 MAC 주소 발견: $prefix")
                return true
            }
        }

        return false
    }

    /**
     * 차량 트래커 앱 자동 실행
     */
    private fun launchVehicleTrackerApp(context: Context, deviceName: String, deviceAddress: String) {
        try {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("auto_started", true)
                putExtra("bluetooth_device_name", deviceName)
                putExtra("bluetooth_device_address", deviceAddress)
                putExtra("start_reason", "bluetooth_connected")
            }

            context.startActivity(launchIntent)
            Log.i(TAG, "차량 트래커 앱 자동 실행 완료")
        } catch (e: Exception) {
            Log.e(TAG, "앱 자동 실행 실패", e)
        }
    }
}