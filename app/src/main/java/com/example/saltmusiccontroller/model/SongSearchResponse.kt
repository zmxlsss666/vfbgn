package com.example.saltmusiccontroller.model

import com.google.gson.annotations.SerializedName

data class SongSearchResponse(
    @SerializedName("result") val result: SongSearchResult,
    @SerializedName("code") val code: Int
)

data class SongSearchResult(
    @SerializedName("songs") val songs: List<SongItem>,
    @SerializedName("hasMore") val hasMore: Boolean,
    @SerializedName("songCount") val songCount: Int
)

data class SongItem(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("artists") val artists: List<Artist>,
    @SerializedName("album") val album: Album,
    @SerializedName("duration") val duration: Long
)

data class Artist(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String
)

data class Album(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String
)
    