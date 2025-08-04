package com.example.vehicletracker.api

import com.google.gson.Gson
import org.junit.Test
import org.junit.Assert.assertEquals

class VehicleStatusDtoTest {
    @Test
    fun testSerialization() {
        val dto = VehicleStatusDto(
            deviceId = "test-device-001",
            bluetoothDevice = "Car Audio XYZ",
            engineStatus = "ON",
            speed = 45.5f,
            timestamp = "2025-08-04T14:00:00Z",
            location = VehicleLocation(latitude = 37.5665, longitude = 126.9780)
        )
        val gson = Gson()
        val json = gson.toJson(dto)
        val expected = "{" +
                "\"deviceId\":\"test-device-001\"," +
                "\"bluetoothDevice\":\"Car Audio XYZ\"," +
                "\"engineStatus\":\"ON\"," +
                "\"speed\":45.5," +
                "\"timestamp\":\"2025-08-04T14:00:00Z\"," +
                "\"location\":{" +
                "\"latitude\":37.5665," +
                "\"longitude\":126.978\"}" +
                "}"
        // Deserialize and check round-trip
        val parsed = gson.fromJson(json, VehicleStatusDto::class.java)
        assertEquals(dto, parsed)
    }
}
