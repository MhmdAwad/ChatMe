package com.mhmdawad.chatme.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.mhmdawad.chatme.pojo.UserData

class Contacts {

    companion object {
        fun getContactName(number: String, context: Context): String {
            var name = number
            val projection = arrayOf(
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup._ID
            )
            val contactUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            val cursor: Cursor? =
                context.contentResolver.query(contactUri, projection, null, null, null)
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    name =
                        cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME))
                }
                cursor.close()
            }
            Log.d("sssss", name)
            return name
        }




    }
}