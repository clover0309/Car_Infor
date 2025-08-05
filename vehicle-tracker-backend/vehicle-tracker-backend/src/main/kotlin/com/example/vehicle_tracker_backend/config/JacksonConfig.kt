package com.example.vehicle_tracker_backend.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Configuration
class JacksonConfig {
    
    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        val mapper = ObjectMapper()
        
        // Kotlin 지원 모듈 등록 (data class 역직렬화를 위해 필수)
        mapper.registerModule(KotlinModule.Builder().build())
        
        // JavaTime 모듈 등록
        val javaTimeModule = JavaTimeModule()
        
        // LocalDateTime 커스텀 직렬화기 등록 (KST 타임존 적용)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        javaTimeModule.addSerializer(LocalDateTime::class.java, LocalDateTimeSerializer(formatter))
        
        mapper.registerModule(javaTimeModule)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        
        // 타임존 설정 (KST)
        mapper.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Seoul"))
        
        return mapper
    }
}
