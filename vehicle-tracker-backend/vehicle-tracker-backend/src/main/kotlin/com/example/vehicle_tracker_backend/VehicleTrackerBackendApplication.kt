package com.example.vehicle_tracker_backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@SpringBootApplication
class VehicleTrackerBackendApplication {
	

}

fun main(args: Array<String>) {
	// 시스템 정보 출력
	println("=== 🚀 Vehicle Tracker Backend Starting ===")
	println("☕ Java Version: ${System.getProperty("java.version")}")
	println("🏠 Java Home: ${System.getProperty("java.home")}")
	println("🐍 Kotlin Version: ${KotlinVersion.CURRENT}")
	println("🔧 JVM Target: 17")
	println("=======================================")
	
	try {
		runApplication<VehicleTrackerBackendApplication>(*args) {
			// 추가 설정이 필요한 경우 여기에 작성
		}
		println("✅ 서버가 성공적으로 시작되었습니다!")
		println("🌐 서버 주소: http://localhost:8080")
		println("🧪 API 테스트: http://localhost:8080/api/vehicle/test")
		println("📊 현재 상태: http://localhost:8080/api/vehicle/current")
	} catch (e: Exception) {
		println("❌ 서버 시작 실패!")
		println("오류 메시지: ${e.message}")
		
		// JVM 버전 관련 오류인지 확인
		if (e.message?.contains("JVM target") == true) {
			println("🔧 JVM 타겟 버전 문제가 감지되었습니다.")
			println("해결방법:")
			println("1. ./gradlew clean build --refresh-dependencies")
			println("2. JAVA_HOME이 올바르게 설정되었는지 확인")
			println("3. IDE에서 프로젝트 JDK를 17로 설정")
		}
		
		throw e
	}
}