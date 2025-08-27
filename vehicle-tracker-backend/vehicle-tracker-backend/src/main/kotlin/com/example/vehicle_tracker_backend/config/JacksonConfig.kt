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
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer



@Configuration
class JacksonConfig {
    
    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        val mapper = ObjectMapper()
        
        // Kotlin 모듈 등록
        mapper.registerModule(KotlinModule.Builder().build())
        
        // JavaTime 모듈 등록
        val javaTimeModule = JavaTimeModule()
        
        // LocalDateTime 커스텀 직렬화/역직렬화 등록 (KST 타임존 적용)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        javaTimeModule.addSerializer(LocalDateTime::class.java, LocalDateTimeSerializer(formatter))
        javaTimeModule.addDeserializer(LocalDateTime::class.java, object : com.fasterxml.jackson.databind.JsonDeserializer<LocalDateTime>() {
            override fun deserialize(p: com.fasterxml.jackson.core.JsonParser?, ctxt: com.fasterxml.jackson.databind.DeserializationContext?): LocalDateTime {
                return LocalDateTime.parse(p?.text, formatter)
            }
        })
        
        mapper.registerModule(javaTimeModule)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        
        // 타임존 설정 (KST)
        mapper.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Seoul"))
        
        return mapper
    }
}
