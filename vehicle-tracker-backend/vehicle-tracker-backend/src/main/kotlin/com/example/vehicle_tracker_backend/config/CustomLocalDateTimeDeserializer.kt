package com.example.vehicle_tracker_backend.config

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CustomLocalDateTimeDeserializer : JsonDeserializer<LocalDateTime>() {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LocalDateTime {
        val text = p.text.trim()
        return LocalDateTime.parse(text, formatter)
    }
}
