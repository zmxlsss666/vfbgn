package com.example.saltmusiccontroller.util

object Constants {
    const val DEFAULT_PORT = 35373
    const val API_NOW_PLAYING = "/api/now-playing"
    const val API_PLAY_PAUSE = "/api/play-pause"
    const val API_NEXT_TRACK = "/api/next-track"
    const val API_PREVIOUS_TRACK = "/api/previous-track"
    const val API_VOLUME_UP = "/api/volume/up"
    const val API_VOLUME_DOWN = "/api/volume/down"
    const val API_MUTE = "/api/mute"
    
    const val SEARCH_SONG_API = "https://music.163.com/api/search/get?type=1&offset=0&limit=1&s="
    const val SONG_INFO_API = "https://api.injahow.cn/meting/?type=song&id="
    
    const val PREFERENCES_NAME = "SaltMusicControllerPrefs"
    const val PREF_LAST_IP = "last_connected_ip"
    const val PREF_LAST_PORT = "last_connected_port"
    
    const val SCAN_TIMEOUT = 5000
    const val SCAN_PORT = 35373
}
    