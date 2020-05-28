package com.mhmdawad.chatme.ui.activities.main_page

import android.content.ContentResolver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mhmdawad.chatme.pojo.ConversationInfo
import java.io.File

@Suppress("UNCHECKED_CAST")
class MainPageFactory constructor(
    private val contentResolver: ContentResolver
) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(MainPageViewModel::class.java)) {
            MainPageViewModel(contentResolver) as T
        } else {
            throw IllegalArgumentException("ViewModel Not Found")
        }
    }
}