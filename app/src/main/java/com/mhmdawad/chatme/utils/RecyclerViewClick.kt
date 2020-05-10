package com.mhmdawad.chatme.utils

interface RecyclerViewClick {

    fun onItemClickedPosition(pos: Int){

    }
    fun onChatClickedString(key: String, userName:String){

    }
    fun openUserImage(userImage: String){

    }
}