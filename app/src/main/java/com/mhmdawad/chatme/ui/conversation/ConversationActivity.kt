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
import com.google.firebase.database.*
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
    private lateinit var firebaseRef: DatabaseReference
    private lateinit var typingListener: ValueEventListener
    private lateinit var fetchMessagesListener: ValueEventListener
    private lateinit var typingChild: DatabaseReference
    private lateinit var fetchMessagesChild: DatabaseReference
    private lateinit var userUid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation)

        val chatID = intent.extras?.getString("chatID")
        val userName = intent.extras?.getString("userName")
        val messageEditText: EditText = findViewById(R.id.messageEditText)
        val sendMessage: FloatingActionButton = findViewById(R.id.sendMessageFab)
        val linearLayout: LinearLayout = findViewById(R.id.linearLayout)
        val userNameTxt: TextView = findViewById(R.id.userNameTxt)
        firebaseRef = FirebaseDatabase.getInstance().reference
        iniButtons()
        typingStatus = findViewById(R.id.typingStatusTxt)
        userNameTxt.text = userName
        getChatUsersUid(chatID!!)
        initContactsRecyclerView()
        fetchMessages(chatID)
        seenMessages(chatID)
        getUserTypingStatus(chatID)

        linearLayout.setOnClickListener {
            onBackPressed()
        }
        messageEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed(userStoppedTyping, 400)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                return
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (messageEditText.text.isEmpty()) {
                    sendMessage.setImageResource(R.drawable.ic_conversation_microphone)
                    cameraButton.visibility = View.VISIBLE
                } else {
                    sendMessage.setImageResource(R.drawable.ic_conversation_send)
                    cameraButton.visibility = View.GONE
                    userTypingStatus(true, chatID)
                }

            }

            val handler = Handler()
            var userStoppedTyping = Runnable {
                userTypingStatus(false, chatID)
            }
        })
        sendMessage.setOnClickListener {
            if(messageEditText.text.isNotEmpty()) {
                sendMessage(messageEditText.text.toString(), chatID)
                messageEditText.text.clear()
            }
        }
    }

    private fun iniButtons() {
        val emojiButton: ImageButton = findViewById(R.id.emojiButton)
        cameraButton = findViewById(R.id.cameraButton)
        val paperClipButton: ImageButton = findViewById(R.id.paperClipButton)
        emojiButton.setOnClickListener {
            Toast.makeText(applicationContext, "Emoji", Toast.LENGTH_SHORT).show()
        }
        cameraButton.setOnClickListener {
            Toast.makeText(applicationContext, "Camera", Toast.LENGTH_SHORT).show()
        }
        paperClipButton.setOnClickListener {
            Toast.makeText(applicationContext, "Paper Clip", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getUserTypingStatus(chatId: String) {
        typingChild = firebaseRef.child("Chats").child(chatId).child("Info").child("typing")
        typingListener = typingChild.addValueEventListener(object : ValueEventListener {
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
                    return
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
        fetchMessagesChild = firebaseRef.child("Chats").child(chatID)
        fetchMessagesListener = fetchMessagesChild.addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.exists()) {
                        chatList.clear()
                        for (data in p0.child("messages").children) {
                            val chat: MessageData = data.getValue(MessageData::class.java)!!
                            chatList.add(chat)
                        }
                    }
                    seenMessages(chatID)
                    getUnseenMessages(chatID)
                    conversationAdapter.addMessage(chatList)
                    chatMessagesRV.scrollToPosition(chatList.size - 1)
                }
            })
    }

    private fun deleteListeners() {
        typingChild.removeEventListener(typingListener)
        fetchMessagesChild.removeEventListener(fetchMessagesListener)
        Toast.makeText(applicationContext, "DELETE", Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        deleteListeners()
    }

//    override fun onResume() {
//        super.onResume()
//    }

    private fun sendMessage(message: String, chatID: String) {
        val key = FirebaseDatabase.getInstance().reference.child("Chats").child(chatID)
            .child("messages").push().key!!
        FirebaseDatabase.getInstance().reference.child("Chats").child(chatID)
            .child("messages").child(key)
            .setValue(MessageData(FirebaseAuth.getInstance().uid!!, message))
        incrementUnreadValue(chatID,message)
    }

    private fun getChatUsersUid(chatID: String) {
        FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().uid!!).child("chat")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.exists())
                        for (data in p0.children) {
                            if (data.getValue(String::class.java) == chatID) {
                                userUid = data.key!!
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

    private fun getUnseenMessages(chatID: String) {
        FirebaseDatabase.getInstance().reference.child("Chats").child(chatID)
            .child("Info").child("unreadMessage").child(userUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    return
                }

                override fun onDataChange(p0: DataSnapshot) {
                    val num = p0.getValue(String::class.java)
                    conversationAdapter.unSeenMessages(num!!.toInt())
                }
            })
    }

    private fun incrementUnreadValue(chatID: String, message: String) {
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
                    } else {
                        p0.ref.child("unreadMessage").child(userUid)
                            .setValue((++num).toString())
                    }
                    p0.ref.child("chatID").setValue(chatID)
                    p0.ref.child("lastMessage").setValue(message)
                    p0.ref.child("lastMessageDate").setValue(
                        SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date())
                    )
                    p0.ref.child("unreadMessage").child(FirebaseAuth.getInstance().uid!!)
                        .setValue("0")
                }
            })

    }
}