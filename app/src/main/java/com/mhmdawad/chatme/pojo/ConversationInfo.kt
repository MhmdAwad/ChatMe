package com.mhmdawad.chatme.pojo

import java.io.File

data class ConversationInfo (
    val userUid: String = "",
    val chatID: String = "",
    val chatType: String = "",
    val recordFile: File
)