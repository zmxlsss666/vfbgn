package com.example.saltmusiccontroller.model

import com.google.gson.annotations.SerializedName

data class ApiResponse(
    @SerializedName("status") val status: String,
    @SerializedName("action") val action: String?,
    @SerializedName("isPlaying") val isPlaying: Boolean?,
    @SerializedName("isMuted") val isMuted: Boolean?,
    @SerializedName("currentVolume") val currentVolume: Double?,
    @SerializedName("newTrack") val newTrack: String?,
    @SerializedName("message") val message: String
)
    