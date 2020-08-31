package com.mhmdawad.chatme.ui.fragments.conversation

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.mhmdawad.chatme.pojo.ConversationInfo
import com.mhmdawad.chatme.pojo.MessageData
import com.mhmdawad.chatme.utils.Contacts
import com.mhmdawad.chatme.utils.VoiceAudio
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class ConversationViewModel(
    val contentResolver: ContentResolver,
    private val conversationInfo: ConversationInfo
) : ViewModel() {


    private val groupUsersName = HashMap<String, String>()
    private lateinit var typingListener: ChildEventListener
    private lateinit var fetchMessagesListener: ChildEventListener
    private lateinit var typingChild: DatabaseReference
    private val voiceAudio = VoiceAudio()
    private var firebaseRef = FirebaseDatabase.getInstance().reference
    private var fetchMessagesChild =
        firebaseRef.child("Chats").child(conversationInfo.chatID).child("messages")
    val typingStatusMutableLiveData = MutableLiveData<String>()
    private val addMessagesData = MutableLiveData<ArrayList<MessageData>>()
    var usersData = MutableLiveData<Pair<HashMap<String, String>, HashMap<String, String>>>()
    val toastMessages = MutableLiveData<String>()
    val displayImageMutableLiveData = MutableLiveData<Pair<String, String>>()
    val unseenMessages = MutableLiveData<Int>()
    val typingStatusVisibilityLiveData = MutableLiveData<Int>().apply { value = 8 }
    private val typingVisibilityObserver = Observer<String>{userTypingStatusView(it)}
    private var startTime: Long = 0

    private val timer = Timer()
    private var oldMessage = ""
    private var newMessage = ""
    private fun checkUserTyping() {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (oldMessage == newMessage) {
                    changeMyTypingStatus("")
                } else {
                    oldMessage = newMessage
                }
            }
        }, 0, 500)
    }
    val messageEditTextLiveData = MutableLiveData<String>()
    val changeViewsVisibilityLiveData = MutableLiveData<Int>().apply { value = 0 }
    private val messageObserver = Observer<String> { changeViewsVisibility(it) }
    private fun changeViewsVisibility(text: String) {
        newMessage = text
        if (text.isEmpty()) {
            changeViewsVisibilityLiveData.value = 0
        } else {
            changeViewsVisibilityLiveData.value = 8
            changeMyTypingStatus()
        }
    }

    init {
        messageEditTextLiveData.observeForever(messageObserver)
        typingStatusMutableLiveData.observeForever(typingVisibilityObserver)
        checkUserTyping()
    }

    fun clickSendMessage() {
            if (newMessage.isNotEmpty()) {
                sendMessage(
                    newMessage,
                    "",
                    "message"
                )
                messageEditTextLiveData.value = ""
            }
    }


    fun changeMyTypingStatus(status: String = "typing..") {
        FirebaseDatabase.getInstance().reference.child("Chats").child(conversationInfo.chatID)
            .child("Info")
            .child("typing").child(FirebaseAuth.getInstance().uid!!).setValue(status)
    }

    private fun startVoiceRecord() {
        startTime = System.nanoTime()
        changeMyTypingStatus("recording audio..")
        voiceAudio.voiceRecord(createRecordFile())
    }

    private fun stopVoiceRecord() {
        val endTime = ((System.nanoTime() - startTime) / 1_000_000_000.0).toInt()
        if (endTime < 1) {
            toastMessages.value = "Hold to record, release to send"
            return
        }
        voiceAudio.stopRecordPlayer()
    }

    fun displayImage(mediaPath: String, type: String, message: String) {
        if (mediaPath != "" && type == "Photo")
            displayImageMutableLiveData.value = Pair(mediaPath, message)
    }

    private fun createRecordFile(): String {
        val file = File.createTempFile("file",".mp3",conversationInfo.recordFile)
        return file.absolutePath
    }

    val requestPermissions = MutableLiveData<Boolean>().apply { value = false }

    fun startRecording() {
        try {
            startVoiceRecord()
        }catch (e: Exception) {
            requestPermissions.value = true
        }
    }


    fun stopRecording() {
        try {
            stopVoiceRecord()
            addMedia()
            changeMyTypingStatus("")
        }catch (e: Exception) {
            requestPermissions.value = true
        }
    }

    private fun checkUserMood() {
        FirebaseDatabase.getInstance().reference.child("Users").child(conversationInfo.userUid)
            .child("mood")
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    return
                }

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.exists()) {
                        typingStatusMutableLiveData.value = p0.getValue(String::class.java)!!
                    }
                }
            })
    }

    private fun getUserTypingStatus() {
        typingChild =
            firebaseRef.child("Chats").child(conversationInfo.chatID).child("Info").child("typing")
        typingListener = typingChild.addChildEventListener(object : ChildEventListener {

            override fun onCancelled(p0: DatabaseError) {
                return
            }

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {
                return
            }

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {
                if (p0.key != FirebaseAuth.getInstance().uid) {
                    var typing = p0.getValue(String::class.java)!!

                    if (conversationInfo.chatType == "group" && typing != "")
                        typing = "${groupUsersName[p0.key]} $typing"

                    typingStatusMutableLiveData.value = typing

                    if (typingStatusMutableLiveData.value.toString() == "" && conversationInfo.chatType == "direct")
                        checkUserMood()
                }
            }

            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                return
            }

            override fun onChildRemoved(p0: DataSnapshot) {
                return
            }
        })
    }

    private fun userTypingStatusView(status: String){
        if (status == "")
            typingStatusVisibilityLiveData.value = 8
        else {
            typingStatusVisibilityLiveData.value = 0
        }
    }

    fun chatMessages(): LiveData<ArrayList<MessageData>> {
        fetchMessages()
        fetchMessagesListener =
            fetchMessagesChild.limitToLast(1).addChildEventListener(object : ChildEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onChildMoved(p0: DataSnapshot, p1: String?) {
                }

                override fun onChildChanged(p0: DataSnapshot, p1: String?) {
                }

                override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                    addMessagesData.value = arrayListOf(p0.getValue(MessageData::class.java)!!)
                    seenMessages()
                }

                override fun onChildRemoved(p0: DataSnapshot) {
                }
            })
        return addMessagesData
    }

    private fun fetchMessages() {
        val chatList: ArrayList<MessageData> = ArrayList()
        firebaseRef.child("Chats").child(conversationInfo.chatID)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.exists())
                        for (data in p0.child("messages").children) {
                            val chat: MessageData = data.getValue(MessageData::class.java)!!
                            chatList.add(chat)
                        }

                    if(chatList.size > 1)
                        chatList.removeAt(chatList.size - 1)
                    if (conversationInfo.chatType == "direct") {
                        seenMessages()
                        getUnseenMessages(conversationInfo.chatID)
                        checkUserMood()
                    }
                    getGroupUsersData()
                    getUserTypingStatus()
                    addMessagesData.value = chatList
                }
            })
    }

    private fun getGroupUsersData() {
        val usersImages = HashMap<String, String>()
        firebaseRef.child("Chats").child(conversationInfo.chatID).child("Info")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {

                }

                override fun onDataChange(p0: DataSnapshot) {
                    for (phones in p0.child("usersPhone").children) {
                        groupUsersName[phones.key!!] = (Contacts.getContactName(
                            phones.getValue(String::class.java)!!,
                            contentResolver
                        ))
                        Log.d("usersNames", groupUsersName[phones.key!!]!!)
                    }
                    for (images in p0.child("usersImage").children) {
                        usersImages[images.key!!] = images.getValue(String::class.java)!!
                    }
                    usersData.value = Pair(usersImages, groupUsersName)
                }
            })

    }

    fun sendMessage(message: String, media: String, type: String) {
        FirebaseDatabase.getInstance().reference.child("Chats").child(conversationInfo.chatID)
            .child("messages").push()
            .setValue(
                MessageData(
                    FirebaseAuth.getInstance().uid!!,
                    message,
                    mediaPath = media,
                    type = type
                )
            )
        addLastMessageData(message, type, conversationInfo.chatType)
        if (conversationInfo.chatType == "direct")
            incrementUnreadValue()
    }


    private fun addMedia() {
        val fileName = "media/ ${UUID.randomUUID()}"
        val filepath = FirebaseStorage.getInstance().reference.child(fileName)
        filepath.putFile(Uri.fromFile(File(createRecordFile()))).addOnSuccessListener {
            filepath.downloadUrl.addOnSuccessListener {
                toastMessages.value = "Record Sent"
                sendMessage("", it.toString(), "Voice Record")
            }
        }.addOnFailureListener {
            toastMessages.value = "Error with upload the image"
        }
    }

    private fun seenMessages() {
        FirebaseDatabase.getInstance().reference.child("Chats").child(conversationInfo.chatID)
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
            .child("Info").child("unreadMessage").child(conversationInfo.userUid)
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    return
                }

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.exists()) {
                        val num = p0.getValue(String::class.java)
                        unseenMessages.value = num!!.toInt()
                    }
                }
            })
    }


    private fun addLastMessageData(message: String, mediaType: String, chatType: String) {
        val firebaseRef =
            FirebaseDatabase.getInstance().reference.child("Chats").child(conversationInfo.chatID)
                .child("Info")
        firebaseRef.ref.child("mediaType").setValue(mediaType)
        firebaseRef.ref.child("chatType").setValue(chatType)
        firebaseRef.ref.child("lastSender").setValue(FirebaseAuth.getInstance().uid!!)
        firebaseRef.ref.child("chatID").setValue(conversationInfo.chatID)
        if (message == "") firebaseRef.ref.child("lastMessage").setValue(mediaType)
        else firebaseRef.ref.child("lastMessage").setValue(message)
        firebaseRef.ref.child("lastMessageDate").setValue(
            SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date())
        )
    }

    private fun incrementUnreadValue() {
        FirebaseDatabase.getInstance().reference.child("Chats").child(conversationInfo.chatID)
            .child("Info").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {
                    var num =
                        p0.child("unreadMessage").child(conversationInfo.userUid)
                            .getValue(String::class.java)
                            ?.toInt()
                    if (num == null) {
                        p0.ref.child("unreadMessage").child(conversationInfo.userUid)
                            .setValue("1")
                    } else {
                        p0.ref.child("unreadMessage").child(conversationInfo.userUid)
                            .setValue((++num).toString())
                    }
                    p0.ref.child("unreadMessage").child(FirebaseAuth.getInstance().uid!!)
                        .setValue("0")
                }
            })
    }

    private fun deleteListeners() {
        typingChild.removeEventListener(typingListener)
        fetchMessagesChild.removeEventListener(fetchMessagesListener)
    }

    override fun onCleared() {
        super.onCleared()
        deleteListeners()
        messageEditTextLiveData.removeObserver(messageObserver)
        typingStatusMutableLiveData.removeObserver(typingVisibilityObserver)
        timer.cancel()
    }
}