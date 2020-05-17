package com.mhmdawad.chatme.pojo

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.utils.CircleTransform
import com.squareup.picasso.Picasso
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UserData(
    var uid: String,
    var Name: String,
    var Number: String,
    var image: String,
    var Statue: String,
    var haveAccount: Boolean = false
) : Parcelable {

    companion object {
        @BindingAdapter("displayImage")
        @JvmStatic
        fun setProfileImage(imageView: ImageView, imagePath: String) {
            Log.d("xxc", imagePath)
            if (imagePath.startsWith("https://firebasestorage"))
                Picasso.get().load(imagePath).transform(CircleTransform()).into(imageView)
            else
                imageView.setImageResource(R.drawable.ic_default_user)
        }
    }
}