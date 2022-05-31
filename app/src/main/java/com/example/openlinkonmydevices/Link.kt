package com.example.openlinkonmydevices

import java.time.LocalDateTime

data class Link (
        var receiverName: String,
        var sharedLink: String,
        var dateTime: LocalDateTime
        )