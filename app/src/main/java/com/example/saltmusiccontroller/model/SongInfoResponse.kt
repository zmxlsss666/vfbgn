package com.example.saltmusiccontroller.model

import com.google.gson.annotations.SerializedName

data class SongInfoResponse(
    @SerializedName("name") val name: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("url") val url: String,
    @SerializedName("pic") val pic: String,
    @SerializedName("lrc") val lrc: String
)
    