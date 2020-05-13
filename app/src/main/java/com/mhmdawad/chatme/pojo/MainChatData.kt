package com.mhmdawad.chatme.pojo

import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

data class MainChatData (
    val chatID: String="",
    var lastMessage: String="",
    val lastMessageDate: String = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date()),
    val unreadMessage: HashMap<String, String> = HashMap(),
    val usersPhone: HashMap<String, String> = HashMap(),
    var offlineUserName: String="",
    val usersImage: HashMap<String, String> = HashMap(),
    var userUid: String="",
    val mediaType: String = "",
    val lastSender:String = ""
)