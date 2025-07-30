package com.example.saltmusiccontroller.model

data class Device(
    val ipAddress: String,
    val port: Int,
    val deviceName: String = "Salt Player"
)
    