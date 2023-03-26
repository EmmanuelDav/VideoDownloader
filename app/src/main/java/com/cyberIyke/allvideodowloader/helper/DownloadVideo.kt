package com.cyberIyke.allvideodowloader.helper

import java.io.Serializable

class DownloadVideo : Serializable {
    var size: String? = null
    var type: String? = null
    var link: String? = null
    var name: String? = null
    var page: String? = null
    var website: String? = null
    var chunked = false
}