package com.mhmdawad.chatme.utils

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.ui.fragments.conversation.ConversationAdapter
import com.squareup.picasso.Picasso

object BindingViews {

    @BindingAdapter("picasso")
    @JvmStatic
    fun loadImage(imageView: ImageView , mediaPath: String){
        if(mediaPath.startsWith("https://firebasestorage"))
            Picasso.get().load(mediaPath).transform(CircleTransform()).into(imageView)
        else
            imageView.setImageResource(R.drawable.ic_default_user)
    }
}