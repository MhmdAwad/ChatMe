package com.mhmdawad.chatme.pojo

import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.utils.CircleTransform
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

data class MainChatData (
    val chatID: String="",
    var lastMessage: String="",
    var lastMessageDate: String = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date()),
    val unreadMessage: HashMap<String, String> = HashMap(),
    val usersPhone: HashMap<String, String> = HashMap(),
    var offlineUserName: String="",
    val usersImage: HashMap<String, String> = HashMap(),
    var userUid: String="",
    val mediaType: String = "",
    val lastSender:String = "",
    val chatType:String = "",
    val groupName:String = "",
    val groupImage:String = "")
{
    companion object {
        @BindingAdapter("bindingSrc")
        @JvmStatic
        fun setChatImage(imageView: ImageView, chat: MainChatData) {
            if (chat.chatType == "direct") {
                if (chat.usersImage[chat.userUid]!!.startsWith("https://firebasestorage"))
                    Picasso.get().load(chat.usersImage[chat.userUid]).transform(CircleTransform()).into(imageView)
                else
                    imageView.setImageResource(R.drawable.ic_default_user)
            } else {
                if (chat.groupImage.startsWith("https://firebasestorage"))
                    Picasso.get().load(chat.usersImage[chat.userUid]).transform(CircleTransform()).into(imageView)
                else
                    imageView.setImageResource(R.drawable.ic_group_black_24dp)
            }
        }

        @BindingAdapter("bindingText")
        @JvmStatic
        fun dateFormat(textView: TextView, date: String) {
            val input = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
            val check = SimpleDateFormat("dd", Locale.getDefault())
            val checkDate = input.parse(date)
            val output: SimpleDateFormat
            output =
                if (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) < (check.format(checkDate!!).toInt()))
                    SimpleDateFormat("yy/MM/dd", Locale.getDefault())
                else
                    SimpleDateFormat("hh:mm a", Locale.getDefault())

            val formatDate = input.parse(date)
            textView.text = output.format(formatDate!!)
        }
    }
}