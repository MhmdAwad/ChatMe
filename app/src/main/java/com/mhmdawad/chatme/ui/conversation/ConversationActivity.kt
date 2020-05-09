package com.mhmdawad.chatme.ui.conversation

import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mhmdawad.chatme.ConversationAdapter
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.pojo.MessageData
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class ConversationActivity : AppCompatActivity() {

    private lateinit var chatMessagesRV: RecyclerView
    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var typingStatus: TextView
    private lateinit var cameraButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation)

        val chatID = intent.extras?.getString("chatID")
        val userName = intent.extras?.getString("userName")
        val messageEditText: EditText = findViewById(R.id.messageEditText)
        val sendMessage: FloatingActionButton = findViewById(R.id.sendMessageFab)
        val linearLayout: LinearLayout = findViewById(R.id.linearLayout)
        val userNameTxt: TextView = findViewById(R.id.userNameTxt)
        iniButtons()
        typingStatus = findViewById(R.id.typingStatusTxt)
        userNameTxt.text = userName
        initContactsRecyclerView()
        fetchMessages(chatID!!)
        seenMessages(chatID)
        getUserTypingStatus(chatID)

        linearLayout.setOnClickListener {
            finish()
        }
        messageEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(userStoppedTyping, 400)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                return
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(messageEditText.text.isEmpty()) {
                    sendMessage.setImageResource(R.drawable.ic_conversation_microphone)
                    cameraButton.visibility = View.VISIBLE
                }
                else {
                    sendMessage.setImageResource(R.drawable.ic_conversation_send)
                    cameraButton.visibility = View.GONE
                }

                    userTypingStatus(true, chatID)
            }

            val handler = Handler()
            var userStoppedTyping = Runnable {
                userTypingStatus(false, chatID)
            }
        })
        sendMessage.setOnClickListener {
            sendMessage(messageEditText.text.toString(), chatID)
            messageEditText.text.clear()
        }
    }

    private fun iniButtons() {
        val emojiButton: ImageButton = findViewById(R.id.emojiButton)
        cameraButton = findViewById(R.id.cameraButton)
        val paperClipButton: ImageButton = findViewById(R.id.paperClipButton)
        emojiButton.setOnClickListener {
            Toast.makeText(applicationContext, "Emoji",Toast.LENGTH_SHORT).show()
        }
        cameraButton.setOnClickListener {
            Toast.makeText(applicationContext, "Camera",Toast.LENGTH_SHORT).show()
        }
        paperClipButton.setOnClickListener {
            Toast.makeText(applicationContext, "Paper Clip",Toast.LENGTH_SHORT).show()
        }
    }

    private fun getUserTypingStatus(chatId: String) {
        FirebaseDatabase.getInstance().reference.child("Chats").child(chatId).child("Info")
            .child("typing")
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    return
                }

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.exists())
                        for (data in p0.children) {
                            if (data.key != FirebaseAuth.getInstance().uid) {
                                if (data.getValue(String::class.java) == "true")
                                    typingStatus.text = "Typing.."
                                else
                                    typingStatus.text = "Online"
                            }
                        }

                }
            })
    }

    private fun userTypingStatus(isTyping: Boolean, chatID: String) {
        FirebaseDatabase.getInstance().reference.child("Chats").child(chatID).child("Info")
            .child("typing").child(FirebaseAuth.getInstance().uid!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun onDataChange(p0: DataSnapshot) {
                    p0.ref.setValue(isTyping.toString())
                }
            })

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

    private fun fetchMessages(chatID: String) {
        val chatList: ArrayList<MessageData> = ArrayList()
        FirebaseDatabase.getInstance().reference.child("Chats").child(chatID)
            .child("messages").addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.exists()) {
                        chatList.clear()
                        for (data in p0.children) {
                            val chat: MessageData = data.getValue(MessageData::class.java)!!
                            chatList.add(chat)
                        }
                    }
                    seenMessages(chatID)
                    conversationAdapter.addMessage(chatList)
                    getChatUsersUid(chatID,"",false)
                    chatMessagesRV.scrollToPosition(chatList.size-1)
                }
            })
    }


    private fun sendMessage(message: String, chatID: String) {
        val key = FirebaseDatabase.getInstance().reference.child("Chats").child(chatID)
            .child("messages").push().key!!
        FirebaseDatabase.getInstance().reference.child("Chats").child(chatID)
            .child("messages").child(key)
            .setValue(MessageData(FirebaseAuth.getInstance().uid!!, message))
        getChatUsersUid(chatID, message, true)
    }

    private fun getChatUsersUid(chatID: String, message: String, incrementValue: Boolean) {
        FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().uid!!).child("chat")
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.exists())
                        for (data in p0.children) {
                            if (data.getValue(String::class.java) == chatID) {
                                val userUid = data.key
                                if (incrementValue)
                                incrementUnreadValue(chatID, message, userUid!!)
                                else
                                    getUnseenMessages(chatID, userUid!!)
                            }
                        }
                }
            })
    }

    private fun seenMessages(chatID: String) {
        FirebaseDatabase.getInstance().reference.child("Chats").child(chatID)
            .child("Info").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.exists())
                        p0.ref.child("unreadMessage").child(FirebaseAuth.getInstance().uid!!)
                            .setValue("0")
                }
            })
    }

    private fun getUnseenMessages(chatID: String, userUid: String){
        FirebaseDatabase.getInstance().reference.child("Chats").child(chatID)
            .child("Info").addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    return
                }

                override fun onDataChange(p0: DataSnapshot) {
                    var num =
                        p0.child("unreadMessage").child(userUid).getValue(String::class.java)
                            ?.toInt()
                    if(num == null)
                        num = 1

                    conversationAdapter.unSeenMessages(num)
                }
            })
    }
    private fun incrementUnreadValue(chatID: String, message: String, userUid: String) {
        FirebaseDatabase.getInstance().reference.child("Chats").child(chatID)
            .child("Info").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {
                    var num =
                        p0.child("unreadMessage").child(userUid).getValue(String::class.java)
                            ?.toInt()
                    if (num == null) {
                        p0.ref.child("unreadMessage").child(userUid)
                            .setValue("1")
                        num = 1
                    } else {
                        p0.ref.child("unreadMessage").child(userUid)
                            .setValue((++num).toString())
                    }
                    Log.d("PPP", "$num")
                    p0.ref.child("chatID").setValue(chatID)
                    p0.ref.child("lastMessage").setValue(message)
                    p0.ref.child("lastMessageDate").setValue(
                        SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date()))
                    p0.ref.child("unreadMessage").child(FirebaseAuth.getInstance().uid!!)
                        .setValue("0")
                }
            })

    }
}