package com.mhmdawad.chatme.pojo

import java.text.SimpleDateFormat
import java.util.*

data class MainChatData (
    val chatID: String="",
    val userName: String="",
    val lastMessage: String="",
    val lastMessageDate: String= SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date()),
    val meUnreadMessages: String = "0",
    val friendUnreadMessages: String= "0",
    val userImage: String=""
)