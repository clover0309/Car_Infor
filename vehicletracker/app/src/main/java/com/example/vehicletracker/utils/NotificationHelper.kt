package com.example.vehicletracker.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.util.Log
import com.example.vehicletracker.MainActivity
import com.example.vehicletracker.R

object NotificationHelper {

    private const val TAG = "NotificationHelper"

    // 알림 채널 ID
    private const val CHANNEL_BLUETOOTH = "bluetooth_channel"
    private const val CHANNEL_VEHICLE_SERVICE = "vehicle_service_channel"
    private const val CHANNEL_DATA_SYNC = "data_sync_channel"

    // 알림 ID
    private const val NOTIFICATION_BLUETOOTH_CONNECTED = 1001
    private const val NOTIFICATION_BLUETOOTH_DISCONNECTED = 1002
    private const val NOTIFICATION_BLUETOOTH_STATE = 1003
    private const val NOTIFICATION_VEHICLE_SERVICE = 1004
    private const val NOTIFICATION_DATA_SYNC = 1005

    /**
     * 알림 채널 초기화 (Android 8.0+)
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 블루투스 알림 채널
            val bluetoothChannel = NotificationChannel(
                CHANNEL_BLUETOOTH,
                "블루투스 연결",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "차량 블루투스 연결 상태 알림"
                enableVibration(true)
                setShowBadge(true)
            }

            // 차량 서비스 알림 채널
            val vehicleServiceChannel = NotificationChannel(
                CHANNEL_VEHICLE_SERVICE,
                "차량 서비스",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "차량 데이터 수집 서비스 상태"
                enableVibration(false)
                setShowBadge(false)
            }

            // 데이터 동기화 알림 채널
            val dataSyncChannel = NotificationChannel(
                CHANNEL_DATA_SYNC,
                "데이터 동기화",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "서버와 데이터 동기화 상태"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(listOf(
                bluetoothChannel,
                vehicleServiceChannel,
                dataSyncChannel
            ))

            Log.d(TAG, "알림 채널 생성 완료")
        }
    }

    /**
     * 블루투스 연결 알림
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun showBluetoothConnectedNotification(context: Context, deviceName: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("from_notification", true)
            putExtra("notification_type", "bluetooth_connected")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_BLUETOOTH)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 차량 아이콘으로 변경 권장
            .setContentTitle("🚗 차량 연결됨")
            .setContentText("$deviceName: $message")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$deviceName\n$message"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "앱 열기",
                pendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_BLUETOOTH_CONNECTED, notification)
            Log.d(TAG, "블루투스 연결 알림 표시: $deviceName")
        } catch (e: SecurityException) {
            Log.e(TAG, "알림 권한이 없습니다", e)
        }
    }

    /**
     * 블루투스 연결 해제 알림
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun showBluetoothDisconnectedNotification(context: Context, deviceName: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_BLUETOOTH)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📱 차량 연결 해제")
            .setContentText("$deviceName: $message")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$deviceName\n$message"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_BLUETOOTH_DISCONNECTED, notification)
            Log.d(TAG, "블루투스 연결 해제 알림 표시: $deviceName")
        } catch (e: SecurityException) {
            Log.e(TAG, "알림 권한이 없습니다", e)
        }
    }

    /**
     * 블루투스 상태 변경 알림
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun showBluetoothStateNotification(context: Context, title: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_BLUETOOTH)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🔵 $title")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_BLUETOOTH_STATE, notification)
            Log.d(TAG, "블루투스 상태 알림 표시: $title")
        } catch (e: SecurityException) {
            Log.e(TAG, "알림 권한이 없습니다", e)
        }
    }

    /**
     * 차량 서비스 포그라운드 알림 (계속 실행 중임을 표시)
     */
    fun createVehicleServiceNotification(context: Context, deviceName: String): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_VEHICLE_SERVICE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🚗 차량 데이터 수집 중")
            .setContentText("$deviceName - GPS 및 속도 데이터 수집 중")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("연결된 차량: $deviceName\n실시간 GPS 위치 및 속도 데이터를 수집하고 있습니다."))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // 사용자가 직접 제거할 수 없는 알림
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "앱 보기",
                pendingIntent
            )
            .build()
    }

    /**
     * 데이터 동기화 성공 알림
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun showDataSyncSuccessNotification(context: Context, recordCount: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_DATA_SYNC)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("✅ 데이터 동기화 완료")
            .setContentText("$recordCount 개의 기록이 서버에 전송되었습니다")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_DATA_SYNC, notification)
            Log.d(TAG, "데이터 동기화 성공 알림 표시: $recordCount 개 기록")
        } catch (e: SecurityException) {
            Log.e(TAG, "알림 권한이 없습니다", e)
        }
    }

    /**
     * 데이터 동기화 실패 알림
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun showDataSyncFailureNotification(context: Context, errorMessage: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_DATA_SYNC)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("❌ 데이터 동기화 실패")
            .setContentText("서버 연결에 실패했습니다: $errorMessage")
            .setStyle(NotificationCompat.BigTextStyle().bigText("서버 연결에 실패했습니다.\n오류: $errorMessage\n\n네트워크 연결을 확인해주세요."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_DATA_SYNC, notification)
            Log.d(TAG, "데이터 동기화 실패 알림 표시: $errorMessage")
        } catch (e: SecurityException) {
            Log.e(TAG, "알림 권한이 없습니다", e)
        }
    }

    /**
     * 특정 알림 제거
     */
    fun cancelNotification(context: Context, notificationId: Int) {
        try {
            NotificationManagerCompat.from(context).cancel(notificationId)
            Log.d(TAG, "알림 제거: ID $notificationId")
        } catch (e: Exception) {
            Log.e(TAG, "알림 제거 실패", e)
        }
    }

    /**
     * 모든 알림 제거
     */
    fun cancelAllNotifications(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancelAll()
            Log.d(TAG, "모든 알림 제거")
        } catch (e: Exception) {
            Log.e(TAG, "모든 알림 제거 실패", e)
        }
    }

    /**
     * 알림 권한 체크
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            true // Android 13 미만에서는 기본적으로 알림 권한이 있음
        }
    }
}