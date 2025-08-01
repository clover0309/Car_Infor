package com.example.vehicletracker

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.webkit.*
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var statusText: TextView
    private var backPressedCallback: OnBackPressedCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI 요소 초기화
        initViews()

        // WebView 설정
        setupWebView()

        // 뒤로가기 버튼 처리 설정 (수정된 버전)
        setupBackPressHandler()

        // 테스트: 간단한 메시지 표시
        updateStatus("앱이 정상적으로 시작되었습니다 ✓")

        // 5초 후 웹페이지 로드 시도
        webView.postDelayed({
            loadWebPage()
        }, 5000)
    }

    private fun initViews() {
        webView = findViewById(R.id.webView)
        statusText = findViewById(R.id.statusText)
    }

    private fun setupWebView() {
        // WebView 기본 설정
        webView.settings.apply {
            // JavaScript 활성화 (필수)
            javaScriptEnabled = true

            // DOM Storage 활성화
            domStorageEnabled = true

            // 파일 접근 허용
            allowContentAccess = true
            allowFileAccess = true

            // 줌 기능
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false // 줌 버튼 숨기기

            // 뷰포트 설정
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        // WebViewClient 설정 (페이지 로딩 이벤트 처리)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d("WebView", "페이지 로딩 시작: $url")
                updateStatus("웹페이지 로딩 중...")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("WebView", "페이지 로딩 완료: $url")
                updateStatus("웹페이지 로드 완료 ✓")
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                Log.e("WebView", "페이지 로딩 오류: ${error?.description}")
                updateStatus("웹페이지 로드 실패 ✗")

                // 사용자에게 오류 메시지 표시
                Toast.makeText(
                    this@MainActivity,
                    "웹페이지를 불러올 수 없습니다. 네트워크를 확인해주세요.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // WebChromeClient 설정 (JavaScript 콘솔 로그 등)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Log.d("WebView-Console", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                return true
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                updateStatus("로딩 중... ($newProgress%)")
            }
        }

        // 개발자 도구 활성화 (DEBUG 빌드에서만)
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    private fun setupBackPressHandler() {
        try {
            // 기존 콜백이 있다면 제거
            backPressedCallback?.remove()

            // 새로운 뒤로가기 콜백 생성
            backPressedCallback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (::webView.isInitialized && webView.canGoBack()) {
                        // WebView에서 뒤로 가기
                        webView.goBack()
                        Log.d("BackPress", "WebView 뒤로가기 실행")
                    } else {
                        // 앱 종료 확인 대화상자 표시
                        showExitConfirmation()
                    }
                }
            }

            // 콜백 등록 (Activity가 ComponentActivity를 상속받지 않으므로 다른 방식 사용)
            // Activity에서는 onBackPressed() 오버라이드 방식 사용
            Log.d("BackPress", "뒤로가기 핸들러 설정 완료")

        } catch (e: Exception) {
            Log.e("BackPress", "뒤로가기 핸들러 설정 실패", e)
            // 기본 뒤로가기 동작 유지
        }
    }

    // Activity에서 뒤로가기 처리 (Activity는 ComponentActivity가 아니므로 기존 방식 사용)
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
            Log.d("BackPress", "WebView 뒤로가기 실행")
        } else {
            showExitConfirmation()
        }
    }

    private fun showExitConfirmation() {
        try {
            // 간단한 Toast로 대체 (AlertDialog 사용 시 추가 import 필요)
            Toast.makeText(this, "한 번 더 누르면 앱이 종료됩니다.", Toast.LENGTH_SHORT).show()

            // 2초 이내에 다시 뒤로가기를 누르면 앱 종료
            var backPressTime = 0L
            val currentTime = System.currentTimeMillis()

            if (currentTime - backPressTime < 2000) {
                super.onBackPressed()
                finish()
            } else {
                backPressTime = currentTime
            }
        } catch (e: Exception) {
            Log.e("BackPress", "종료 확인 처리 실패", e)
            super.onBackPressed()
        }
    }

    private fun loadWebPage() {
        try {
            // 웹페이지 URL 설정
            val webUrl = getWebPageUrl()

            Log.d("WebView", "웹페이지 로딩 시도: $webUrl")
            updateStatus("연결 중...")

            // 웹페이지 로드
            webView.loadUrl(webUrl)
        } catch (e: Exception) {
            Log.e("WebView", "웹페이지 로딩 중 오류", e)
            updateStatus("로딩 오류 발생")

            // 간단한 HTML 페이지 로드 (fallback)
            loadFallbackPage()
        }
    }

    private fun loadFallbackPage() {
        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Vehicle Tracker</title>
                <style>
                    body { 
                        font-family: Arial, sans-serif; 
                        text-align: center; 
                        padding: 50px;
                        background-color: #f5f5f5;
                    }
                    .container {
                        background: white;
                        padding: 30px;
                        border-radius: 10px;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                    }
                    .status { color: #ff6b6b; margin: 20px 0; }
                    .info { color: #666; font-size: 14px; line-height: 1.5; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>🚗 Vehicle Tracker</h1>
                    <div class="status">서버에 연결할 수 없습니다</div>
                    <div class="info">
                        확인사항:<br>
                        • Spring Boot 서버 실행 상태 (포트 8080)<br>
                        • NextJS 서버 실행 상태 (포트 3000)<br>
                        • 네트워크 연결 상태
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        updateStatus("오프라인 모드")
    }

    private fun getWebPageUrl(): String {
        // 강제 URL 설정 (에뮬레이터용)
        val url = "http://10.0.2.2:3000"

        Log.d("WebView", "=== URL 설정 ===")
        Log.d("WebView", "사용할 URL: $url")
        Log.d("WebView", "=================")

        return url
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            if (::statusText.isInitialized) {
                statusText.text = message
            }
        }
    }

    override fun onDestroy() {
        try {
            // 콜백 제거
            backPressedCallback?.remove()

            // WebView 정리
            if (::webView.isInitialized) {
                webView.destroy()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "리소스 정리 중 오류", e)
        }

        super.onDestroy()
    }
}