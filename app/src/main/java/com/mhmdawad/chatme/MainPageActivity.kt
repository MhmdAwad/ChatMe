package com.mhmdawad.chatme

import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Button
import java.util.jar.Manifest

class MainPageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_page)

        val findUser = findViewById<Button>(R.id.findUser)
        checkPermission()
        findUser.setOnClickListener {
            checkPermission()
            startActivity(Intent(this, ContactsActivity::class.java))
        }
    }



    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val readContact = android.Manifest.permission.READ_CONTACTS
            val writeContact = android.Manifest.permission.WRITE_CONTACTS
            if (checkCallingOrSelfPermission(writeContact) != PackageManager.PERMISSION_GRANTED
                || checkCallingOrSelfPermission(readContact) != PackageManager.PERMISSION_GRANTED
            )
                requestPermissions(arrayOf(readContact, writeContact), 100)
        }
    }
}
