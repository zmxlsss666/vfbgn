package com.example.saltmusiccontroller.model

import com.google.gson.annotations.SerializedName

data class NowPlaying(
    @SerializedName("status") val status: String,
    @SerializedName("title") val title: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("album") val album: String,
    @SerializedName("isPlaying") val isPlaying: Boolean,
    @SerializedName("position") val position: Long,
    @SerializedName("volume") val volume: Double,
    @SerializedName("timestamp") val timestamp: Long
)
    