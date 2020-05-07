package com.mhmdawad.chatme.ui.main_Page

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mhmdawad.chatme.MainChatAdapter
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.pojo.MainChatData
import com.mhmdawad.chatme.ui.contact.ContactsActivity
import com.mhmdawad.chatme.ui.conversation.ConversationActivity
import com.mhmdawad.chatme.ui.main.MainActivity
import com.mhmdawad.chatme.utils.RecyclerViewClick

class MainPageActivity : AppCompatActivity(), RecyclerViewClick {
    private lateinit var mainPageChatRV: RecyclerView
    private lateinit var chatAdapter: MainChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_page)

        val findUser = findViewById<Button>(R.id.findUser)
        val logout = findViewById<Button>(R.id.logout)
        initRecyclerView()
        checkPermission()

        logout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(applicationContext, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
        findUser.setOnClickListener {
            checkPermission()
            startActivity(Intent(this, ContactsActivity::class.java))
        }
    }

    private fun fetchConversations() {
        FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().uid!!)
            .child("chat").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {
                    getChatInfo(p0)
                }
            })
    }

    private fun getChatInfo(p0: DataSnapshot) {
        val chatObject: ArrayList<MainChatData> = ArrayList()
        for (data in p0.children) {
            FirebaseDatabase.getInstance().reference.child("Chats").child(data.getValue(String::class.java)!!).child("Info")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {}

                    override fun onDataChange(p0: DataSnapshot) {
                        if (p0.exists()) {
                            val chatData = p0.getValue(MainChatData::class.java)!!
                            chatObject.add(chatData)
                            chatAdapter.addMainChats(chatObject)
                        }
                    }
                })
        }
    }

    private fun startConversationActivity(key: String) {
        val intent = Intent(
            applicationContext,
            ConversationActivity::class.java
        )
        intent.putExtra("chatID", key)
        startActivity(intent)
    }

    private fun initRecyclerView() {
        chatAdapter = MainChatAdapter(this)
        mainPageChatRV = findViewById(R.id.mainPageChatRV)
        mainPageChatRV.apply {
            layoutManager = LinearLayoutManager(
                applicationContext,
                LinearLayoutManager.VERTICAL, false
            )
            adapter = chatAdapter
        }
        fetchConversations()
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val readContact = android.Manifest.permission.READ_CONTACTS
            val writeContact = android.Manifest.permission.WRITE_CONTACTS
            if (checkCallingOrSelfPermission(writeContact) != PackageManager.PERMISSION_GRANTED ||
                checkCallingOrSelfPermission(readContact) != PackageManager.PERMISSION_GRANTED
            )
                requestPermissions(arrayOf(readContact, writeContact), 100)
        }
    }

    override fun onItemClickedString(key: String) {
        startConversationActivity(key)
    }
}
