package com.mhmdawad.chatme.utils

import android.widget.SeekBar

interface RecyclerViewClick {

    fun onItemClickedPosition(pos: Int){

    }
    fun onChatClickedString(key: String, userName:String, userImage: String){

    }
    fun openUserImage(userImage: String, userName: String){

    }
    fun receivedNewMessage(){

    }
}