package com.mhmdawad.chatme.ui.conversation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mhmdawad.chatme.ConversationAdapter
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.pojo.MainChatData
import com.mhmdawad.chatme.pojo.MessageData

class ConversationActivity : AppCompatActivity() {

    private lateinit var chatMessagesRV: RecyclerView
    private lateinit var conversationAdapter: ConversationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation)

        val chatID = intent.extras?.getString("chatID")
        val messageEditText: EditText = findViewById(R.id.messageEditText)
        val sendMessage: ImageButton = findViewById(R.id.sendMessage)
        initContactsRecyclerView()
        fetchMessages(chatID!!)
        sendMessage.setOnClickListener {
            sendMessage(messageEditText.text.toString(), chatID)
            messageEditText.text.clear()
        }
    }

    private fun initContactsRecyclerView() {
        chatMessagesRV = findViewById(R.id.chatMessagesRV)
        conversationAdapter = ConversationAdapter()
        chatMessagesRV.apply {
            layoutManager =
                LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, false)
            adapter = conversationAdapter
        }
    }

    private fun fetchMessages(chatID: String){
        val chatList: ArrayList<MessageData> = ArrayList()
        FirebaseDatabase.getInstance().reference.child("Chats").child(chatID)
            .child("messages").addValueEventListener(object : ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {
                    if(p0.exists()) {
                        chatList.clear()
                        for (data in p0.children) {
                            val chat: MessageData = data.getValue(MessageData::class.java)!!
                            chatList.add(chat)
                        }
                    }
                    conversationAdapter.addMessage(chatList)
                }
            })
    }
    private fun sendMessage(message: String, chatID: String){
        val key = FirebaseDatabase.getInstance().reference.child("Chats").child(chatID)
            .child("messages").push().key!!
        FirebaseDatabase.getInstance().reference.child("Chats").child(chatID)
            .child("messages").child(key).setValue(MessageData(FirebaseAuth.getInstance().uid!!,message))
//        FirebaseDatabase.getInstance().reference.child("Chats").child(chatID)
//            .child("Info").child("lastMessages").setValue(message)
//        FirebaseDatabase.getInstance().reference.child("Chats").child(chatID)
//            .child("Info").child("unreadMessages").child("Friend").setValue("1")
//        FirebaseDatabase.getInstance().reference.child("Chats").child(chatID)
//            .child("Info").child("unreadMessages").child("Me").setValue("0")
        FirebaseDatabase.getInstance().reference.child("Chats").child(chatID)
            .child("Info").setValue(MainChatData(chatID,"",message,userImage = ""))
        incrementUnreadValue(chatID)
    }

    private fun incrementUnreadValue(chatID: String){
        FirebaseDatabase.getInstance().reference.child("Chats").child(chatID)
            .child("Info")
            .child("friendUnreadMessages").addListenerForSingleValueEvent(object :ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {
                    if(p0.exists())
                    for(data in p0.children) {
                        var num = data.child("Me").getValue(String::class.java)?.toInt()!!
                        num++
                        p0.ref.setValue(num)
                    }
                }
            })

    }
}
