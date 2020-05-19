package com.mhmdawad.chatme.pojo

import android.graphics.Color
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.masoudss.lib.WaveformSeekBar
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.utils.CircleTransform
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

data class MessageData(
    val senderUid: String = "",
    var message: String = "",
    val date: String = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date()),
    val mediaPath: String = "",
    val type: String = ""
) {

    companion object {
        private val userNameColors = HashMap<String, Int>()

        @BindingAdapter("imageSrc")
        @JvmStatic
        fun imageRes(imageView: ImageView, imagePath: String?) {
            if(imagePath != "")
                Picasso.get().load(imagePath).into(imageView)
        }

        @BindingAdapter("recordImage")
        @JvmStatic
        fun recordImage(imageView: ImageView, imagePath: String?) {
            if (imagePath != "")
                Picasso.get().load(imagePath).transform(CircleTransform()).into(
                    imageView
                )
            else
                imageView.setImageResource(R.drawable.ic_default_user)
        }

        @BindingAdapter("seekBarSample")
        @JvmStatic
        fun seekBarSample(seekBar: WaveformSeekBar, num :Int){
            seekBar.sample = intArrayOf(1,4,2,5,7,4,7,4,3,6,5,4,6,3,4,8,6,3,7,6,8)
            seekBar.isEnabled = false
        }
        @BindingAdapter("android:textColors")
        @JvmStatic
        fun randomUserNameColor(textView: TextView, userName: String) {
            if (!userNameColors.containsKey(userName)) {
                val rnd = Random()
                val color = Color.argb(
                    255,
                    rnd.nextInt(256),
                    rnd.nextInt(256),
                    rnd.nextInt(256)
                )
                userNameColors[userName] = color
            }
            textView.setTextColor(userNameColors[userName]!!)
        }
    }
}