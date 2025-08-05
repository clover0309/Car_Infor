package com.example.vehicletracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.vehicletracker.BluetoothGpsService

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "부팅 이벤트 수신: $action")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.i(TAG, "부팅 완료 - 차량 추적 서비스 시작")
                startVehicleTrackingService(context)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.i(TAG, "앱 업데이트 완료 - 차량 추적 서비스 재시작")
                startVehicleTrackingService(context)
            }
        }
    }

    private fun startVehicleTrackingService(context: Context) {
        try {
            val serviceIntent = Intent(context, BluetoothGpsService::class.java)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            Log.i(TAG, "차량 추적 서비스 시작 완료")
        } catch (e: Exception) {
            Log.e(TAG, "차량 추적 서비스 시작 실패", e)
        }
    }
}