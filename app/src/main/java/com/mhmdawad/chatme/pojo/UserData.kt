package com.mhmdawad.chatme.pojo

import android.os.Parcel
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UserData(
    var uid: String,
    var Name: String,
    var Number: String,
    var Image: String,
    var Statue: String,
    var haveAccount: Boolean = false
) : Parcelable