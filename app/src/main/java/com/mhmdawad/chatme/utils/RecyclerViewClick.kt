package com.mhmdawad.chatme.utils

import com.mhmdawad.chatme.pojo.UserData


interface RecyclerViewClick {

    fun onItemClickedPosition(data: UserData){

    }
    fun onChatClickedString(key: String, userName:String, userImage: String,chatType:String, userUid: String){

    }
    fun openUserImage(userImage: String, userName: String){

    }
    fun receivedNewMessage(){

    }
}