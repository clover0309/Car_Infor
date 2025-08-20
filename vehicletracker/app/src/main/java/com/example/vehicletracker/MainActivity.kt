package com.example.vehicletracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebResourceResponse
import android.webkit.ConsoleMessage
import android.webkit.WebSettings
import android.view.View
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast
import android.view.KeyEvent
import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.example.vehicletracker.R
import com.example.vehicletracker.BuildConfig
import com.example.vehicletracker.api.DeviceInfoEntity
import com.example.vehicletracker.api.DeviceRegisterRequest
import com.example.vehicletracker.api.RetrofitInstance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    
    private val apiService = ApiService()
    private val mainScope = CoroutineScope(Dispatchers.Main)

    private lateinit var webView: WebView
    private lateinit var statusText: TextView
    private lateinit var registerDeviceButton: Button
    private var backPressTime: Long = 0
    
    // 기기 등록 모달창이 표시되고 있는지 확인하는 플래그
    private var isRegisterDialogShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startBluetoothGpsServiceIfPermitted()

        initViews()

        setupWebView()

        registerDeviceButton.setOnClickListener {
            showRegisterDeviceDialog()
        }

        updateStatus("앱이 정상적으로 시작되었습니다 ")

        webView.postDelayed({
            loadWebPage()
        }, 5000)

        startPollingVehicleStatus()
    }

    private fun startBluetoothGpsServiceIfPermitted() {
        val requiredPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.FOREGROUND_SERVICE
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requiredPermissions.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
            requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        
        val notGranted = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (notGranted.isEmpty()) {
            val serviceIntent = Intent(this, BluetoothGpsService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } else {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), 101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, 
        permissions: Array<out String>, 
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            startBluetoothGpsServiceIfPermitted()
        }
    }

    private fun initViews() {
        webView = findViewById(R.id.webView)
        statusText = findViewById(R.id.statusText)
        registerDeviceButton = findViewById(R.id.registerDeviceButton)
    }

    private fun setupWebView() {
    // Chrome DevTools로 WebView 디버깅 허용 (adb로 연결하여 chrome://inspect)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        WebView.setWebContentsDebuggingEnabled(true)
    }
    webView.settings.javaScriptEnabled = true
    webView.settings.domStorageEnabled = true
    // Kakao Map 타일 리소스(https)와 페이지(http)의 혼합 콘텐츠 허용
    webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
    // 지도 타일 렌더링 품질 안정화
    webView.settings.useWideViewPort = true
    webView.settings.loadWithOverviewMode = true
    // 이미지 로드/타일 렌더링 보강
    webView.settings.setLoadsImagesAutomatically(true)
    webView.settings.setBlockNetworkImage(false)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        webView.settings.setOffscreenPreRaster(true)
    }
    // 하드웨어 가속 강제 사용 (타일 미표시 이슈 방지)
    webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url.toString()
                if (url.contains("daumcdn.net") || url.contains("kakao.com") || url.contains("dapi.kakao.com")) {
                    Log.d("WebViewReq", "Requesting: $url")
                }
                return super.shouldInterceptRequest(view, request)
            }
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                // 앱의 기본 URL(WEB_URL)과 같은 도메인인지 확인합니다.
                val baseUrl = BuildConfig.WEB_URL
                if (url.startsWith(baseUrl)) {
                    // 같은 도메인이면 WebView가 직접 로드하도록 false를 반환합니다.
                    return false
                }

                // 외부 링크(http, https)를 외부 브라우저에서 열도록 처리합니다.
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        startActivity(intent)
                        return true
                    } catch (e: Exception) {
                        return true // 예외 발생 시 아무것도 하지 않음
                    }
                }
                // 그 외의 스킴(tel:, mailto: 등)도 외부 앱으로 처리합니다.
                return try {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    startActivity(intent)
                    true
                } catch (e: Exception) {
                    true
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                updateStatus("페이지 로딩 중...")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                updateStatus("페이지 로딩 완료.")
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    Log.e("WebViewError", "URL: ${request.url}, Error: ${error?.errorCode}, ${error?.description}")
                    loadFallbackPage()
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: android.webkit.WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                val url = request?.url.toString()
                val statusCode = errorResponse?.statusCode
                if (url.contains("daumcdn.net") || url.contains("kakao.com") || url.contains("dapi.kakao.com")) {
                    Log.e("WebViewHttpError", "HTTP $statusCode on $url")
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Log.d("WebViewConsole", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                return true
            }
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (System.currentTimeMillis() - backPressTime < 2000) {
                finish()
            } else {
                backPressTime = System.currentTimeMillis()
                Toast.makeText(this, "한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun loadWebPage() {
        val url = BuildConfig.WEB_URL
        if (url.isNotEmpty()) {
            Log.d("MainActivity", "웹 페이지 로딩: $url")
            webView.loadUrl(url)
        } else {
            Log.e("MainActivity", "웹 페이지 URL을 찾을 수 없습니다.")
            loadFallbackPage()
        }
    }

    private fun loadFallbackPage() {
        try {
            webView.loadUrl("file:///android_asset/index.html")
        } catch (e: Exception) {
            Log.e("FallbackLoad", "대체 페이지 로딩 실패", e)
            updateStatus("모든 페이지 로딩에 실패했습니다. 네트워크 연결을 확인해주세요.")
        }
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            statusText.text = message
            Log.d("StatusUpdate", message)
        }
    }

    private fun startPollingVehicleStatus() {
        mainScope.launch {
            while (true) {
                try {
                    val jsonResponse = withContext(Dispatchers.IO) {
                        apiService.getLatestVehicleStatus()
                    }

                    if (jsonResponse != null) {
                        // JSON 파싱
                        val jsonObject = org.json.JSONObject(jsonResponse)
                        val engineStatus = jsonObject.getString("engineStatus")
                        val speed = jsonObject.getInt("speed")
                        val timestamp = jsonObject.getString("timestamp") // 타임스탬프는 일단 문자열로 받음

                        val statusMessage = "엔진: $engineStatus, 속도: ${speed}km/h (업데이트: $timestamp)"
                        updateStatus(statusMessage)
                    } else {
                        // updateStatus("최신 상태를 가져오지 못했습니다.") // 실패 메시지를 계속 표시하지 않으려면 주석 처리
                    }
                } catch (e: Exception) {
                    Log.e("PollingError", "차량 상태 업데이트 중 오류", e)
                }
                kotlinx.coroutines.delay(5000) // 5초마다 반복
            }
        }
    }

    override fun onDestroy() {
        try {
            // 앱 종료 시 직접 OFF 전송은 중복/오검지 위험이 있어 제거
            // OFF 전송은 BluetoothGpsService에서만 책임집니다.
            
            if (::webView.isInitialized) {
                webView.destroy()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "리소스 정리 중 오류", e)
        }

        super.onDestroy()
    }
    
    /**
     * 앱 종료 시 엔진 OFF 상태를 서버에 전송
     */
    // 현재는 사용하지 않음: OFF 전송은 BluetoothGpsService에서만 처리
    private fun sendEngineOffStatus() {
        Log.i("MainActivity", "[시동 OFF 감지] 앱 종료로 인한 엔진 OFF 상태 전송 시도")
        Thread {
            try {
                val deviceId = android.provider.Settings.Secure.getString(
                    contentResolver, 
                    android.provider.Settings.Secure.ANDROID_ID
                )
                
                val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                
                // SharedPreferences에서 마지막으로 연결된 기기 정보 가져오기
                val prefs = getSharedPreferences("VehicleTrackerPrefs", android.content.Context.MODE_PRIVATE)
                val lastDeviceName = prefs.getString("last_connected_device_name", "Unknown Device")
                
                val dto = com.example.vehicletracker.api.VehicleStatusDto(
                    deviceId = deviceId,
                    bluetoothDevice = lastDeviceName ?: "Unknown Device",
                    engineStatus = "OFF",
                    speed = 0f,
                    timestamp = timestamp,
                    location = null // 앱 종료 시에는 위치 정보 없음
                )
                
                Log.d("MainActivity", "[시동 OFF 감지] 전송할 데이터: $dto")
                
                val response = com.example.vehicletracker.api.RetrofitInstance.api.sendVehicleStatus(dto).execute()
                if (response.isSuccessful) {
                    Log.i("MainActivity", "[시동 OFF 감지] 앱 종료로 인한 엔진 OFF 상태 전송 성공: ${response.code()}, 기기: $lastDeviceName")
                } else {
                    Log.e("MainActivity", "[시동 OFF 감지] 앱 종료로 인한 엔진 OFF 상태 전송 실패: ${response.code()} ${response.errorBody()?.string()}, 기기: $lastDeviceName")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "[시동 OFF 감지] 앱 종료로 인한 엔진 OFF 상태 전송 중 오류", e)
            }
        }.start()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == "ACTION_SHOW_REGISTER_DIALOG") {
            val deviceId = intent.getStringExtra("device_id")
            val deviceName = intent.getStringExtra("device_name")
            if (deviceId != null) {
                mainScope.launch {
                    try {
                        val response = withContext(Dispatchers.IO) {
                            RetrofitInstance.api.checkDeviceExists(deviceId)
                        }
                        if (response.isSuccessful) {
                            val exists = response.body() ?: false
                            if (exists) {
                                Toast.makeText(this@MainActivity, "이미 등록된 기기입니다.", Toast.LENGTH_SHORT).show()
                            } else {
                                showRegisterDeviceDialog(deviceId, deviceName)
                            }
                        } else {
                            Toast.makeText(this@MainActivity, "기기 확인에 실패했습니다: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "기기 확인 중 오류 발생", e)
                        Toast.makeText(this@MainActivity, "오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showRegisterDeviceDialog(deviceId: String? = null, deviceName: String? = null) {
        // 이미 모달창이 표시되고 있는 경우 중복 표시 방지
        if (isRegisterDialogShowing) {
            Log.d("MainActivity", "[기기 등록] 이미 모달창이 표시되고 있어 중복 표시를 방지합니다.")
            return
        }
        
        isRegisterDialogShowing = true
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_register_device, null)
        val deviceIdTextView = dialogView.findViewById<TextView>(R.id.deviceIdTextView)
        val deviceNameEditText = dialogView.findViewById<EditText>(R.id.deviceNameEditText)

        if (deviceId != null) {
            deviceIdTextView.text = deviceId
            deviceNameEditText.setText(deviceName)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("등록", null)
            .setNegativeButton("취소", null)
            .create()
            
        // 다이얼로그가 닫혔을 때 플래그 초기화
        dialog.setOnDismissListener {
            isRegisterDialogShowing = false
            Log.d("MainActivity", "[기기 등록] 모달창 닫힘, 플래그 초기화")
        }

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val newDeviceId = deviceIdTextView.text.toString().trim()
                val newDeviceName = deviceNameEditText.text.toString().trim()

                if (newDeviceName.isEmpty()) {
                    Toast.makeText(this, "기기 이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
                } else {
                    registerDevice(newDeviceId, newDeviceName)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun registerDevice(deviceId: String, deviceName: String) {
        val request = DeviceRegisterRequest(deviceId = deviceId, deviceName = deviceName)
        RetrofitInstance.api.registerDevice(request).enqueue(object : Callback<DeviceInfoEntity> {
            override fun onResponse(call: Call<DeviceInfoEntity>, response: Response<DeviceInfoEntity>) {
                if (response.isSuccessful) {
                    val registeredDevice = response.body()
                    Log.d("ApiService", "기기 등록 성공: $registeredDevice")
                    Toast.makeText(this@MainActivity, "'${registeredDevice?.deviceName}' 기기가 등록되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("ApiService", "기기 등록 실패: $errorBody")
                    Toast.makeText(this@MainActivity, "기기 등록 실패: $errorBody", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<DeviceInfoEntity>, t: Throwable) {
                Log.e("ApiService", "네트워크 오류", t)
                Toast.makeText(this@MainActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}