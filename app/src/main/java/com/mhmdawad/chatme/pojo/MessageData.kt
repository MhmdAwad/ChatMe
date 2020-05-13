package com.mhmdawad.chatme.pojo

import java.text.SimpleDateFormat
import java.util.*

data class MessageData (
    val senderUid: String="",
    var message: String="",
    val date: String = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date()),
    val mediaPath:String = "",
    val type:String = ""

)