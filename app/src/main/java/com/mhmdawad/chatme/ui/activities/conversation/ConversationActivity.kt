package com.mhmdawad.chatme.ui.activities.conversation

import android.Manifest.permission.*
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaRecorder
import android.media.SoundPool
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.mhmdawad.chatme.utils.CircleTransform
import com.mhmdawad.chatme.adapters.ConversationAdapter
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.pojo.MessageData
import com.mhmdawad.chatme.ui.fragments.DisplayImageFragment
import com.mhmdawad.chatme.utils.RecyclerViewClick
import com.squareup.picasso.Picasso
import hani.momanii.supernova_emoji_library.Actions.EmojIconActions
import kotlinx.android.synthetic.main.activity_conversation.*
import kotlinx.android.synthetic.main.activity_conversation.view.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ConversationActivity : AppCompatActivity(), DisplayImageFragment.NewMessage, RecyclerViewClick {

    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var firebaseRef: DatabaseReference
    private lateinit var typingListener: ValueEventListener
    private lateinit var fetchMessagesListener: ValueEventListener
    private lateinit var typingChild: DatabaseReference
    private lateinit var fetchMessagesChild: DatabaseReference
    private lateinit var userUid: String
    private lateinit var chatID: String
    private lateinit var rootView: View
    private lateinit var recordFilePath: String
    private var send: Int? = null
    private var receive: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation)
    }

    private fun clickSendMessage() {
        var startTime: Long = 0
        rootView.sendMessageFab.setOnTouchListener { _, event ->

            if (event.action == MotionEvent.ACTION_DOWN) {
                if (messageEditText.text.isEmpty()) {
                        startTime = System.nanoTime()
                        stopRecordPlayer()
                        voiceRecord()
                        rootView.emojiButton.setImageResource(R.drawable.ic_red_microphone)
                }
            } else if (event.action == MotionEvent.ACTION_UP) {
                if (messageEditText.text.isEmpty()) {
                    val endTime = ((System.nanoTime() - startTime) / 1_000_000_000.0).toInt()
                    if (endTime < 1) {
                        Toast.makeText(
                            applicationContext,
                            "Hold to record, release to send",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        if(checkPermission()) {
                            rootView.emojiButton.setImageResource(R.drawable.ic_conversation_emoji)
                            stopRecordPlayer()
                            addMedia(Uri.fromFile(File(recordFilePath)))
                        }else
                            requestPermissions(arrayOf(WRITE_EXTERNAL_STORAGE, RECORD_AUDIO), 100)
                    }
                } else {
                    sendMessage(messageEditText.text.toString(), "", "message")
                    messageEditText.text.clear()
                }

            }
            return@setOnTouchListener true
        }
    }

    private fun stopRecordPlayer() {
        if (this::recorder.isInitialized) {
            try {
                recorder.stop()
                recorder.reset()
                recorder.release()
            } catch (e: RuntimeException) {
                Log.d("RuntimeException", "$e")
            }
        }
    }


    private var soundPool: SoundPool? = null
    private fun playSound() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        send = soundPool!!.load(this, R.raw.send, 1)
        receive = soundPool!!.load(this, R.raw.receive, 1)

    }

    private lateinit var recorder: MediaRecorder

    private fun voiceRecord() {
        if (checkPermission()) {
            val file =  File(getExternalFilesDir(null)!!.absolutePath + "/record/")
            if (!file.exists())
                file.mkdirs()

            recordFilePath = file.absolutePath + "/file.mp3"
            recorder = MediaRecorder()
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setOutputFile(recordFilePath)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.prepare()
            recorder.start()
        } else {
            requestPermissions(arrayOf(WRITE_EXTERNAL_STORAGE, RECORD_AUDIO), 100)
        }

    }

    private fun checkPermission(): Boolean {
        val result = checkSelfPermission(
            applicationContext,
            WRITE_EXTERNAL_STORAGE
        )
        val result1 =
            checkSelfPermission(applicationContext, RECORD_AUDIO)
        return result == PackageManager.PERMISSION_GRANTED &&
                result1 == PackageManager.PERMISSION_GRANTED
    }

    private fun initArguments() {
        chatID = intent.extras!!.getString("ChatID")!!
        val userName = intent.extras!!.getString("userName")!!
        val userImage = intent.extras!!.getString("userImage")!!
        if (userImage.startsWith("https://firebasestorage"))
            Picasso.get().load(userImage).transform(CircleTransform()).into(rootView.imageView)
        firebaseRef = FirebaseDatabase.getInstance().reference
        rootView.userNameTxt.text = userName }

    private fun editTextChanged() {
        rootView.messageEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed(userStoppedTyping, 400)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                return
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (rootView.messageEditText.text.isEmpty()) {
                    rootView.sendMessageFab.setImageResource(R.drawable.ic_conversation_microphone)
                    rootView.cameraButton.visibility = View.VISIBLE
                } else {
                    rootView.sendMessageFab.setImageResource(R.drawable.ic_conversation_send)
                    rootView.cameraButton.visibility = View.GONE
                    userTypingStatus("typing..", chatID)
                }
            }

            val handler = Handler()
            var userStoppedTyping = Runnable {
                userTypingStatus("", chatID)
            }
        })
    }

    private fun checkUserMood() {
        FirebaseDatabase.getInstance().reference.child("Users").child(userUid).child("mood")
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    return
                }

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.exists()) {
                        rootView.typingStatus.text = p0.getValue(String::class.java)!!
                        if (rootView.typingStatus.text == "")
                            rootView.typingStatus.visibility = View.GONE
                        else
                            rootView.typingStatus.visibility = View.VISIBLE
                    }
                }
            })
    }

    private fun iniButtons() {
        val emojIcon = EmojIconActions(this, rootView, messageEditText, emojiButton)
        emojIcon.setUseSystemEmoji(true)
        messageEditText.setUseSystemDefault(true)
        emojIcon.setIconsIds(R.drawable.ic_keyboard, R.drawable.ic_conversation_emoji)
        emojIcon.ShowEmojIcon()
        emojiButton.setOnClickListener {
            emojIcon.ShowEmojIcon()
        }

        rootView.cameraButton.setOnClickListener {
            chooseImage()
        }
        //TODO
//        rootView.paperClipButton.setOnClickListener {
//
//        }
    }

    private fun getUserTypingStatus() {
        typingChild = firebaseRef.child("Chats").child(chatID).child("Info").child("typing")
        typingListener = typingChild.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                return
            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists())
                    for (data in p0.children) {
                        if (data.key != FirebaseAuth.getInstance().uid)
                            rootView.typingStatus.text = data.getValue(String::class.java)!!
                        if (rootView.typingStatus.text == "")
                            checkUserMood()
                        else
                            rootView.typingStatus.visibility = View.VISIBLE

                    }
            }
        })
    }


    private fun chooseImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), 101)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 101 ) {
            displayImage(data?.data!!.toString(), false, "")
        }
    }

    private fun displayImage(imageUri: String, hideViews: Boolean, message: String) {
        val fragment = DisplayImageFragment.newInstance(imageUri, message, hideViews, this)
        supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit()
    }


    private fun userTypingStatus(status: String, chatID: String) {
        FirebaseDatabase.getInstance().reference.child("Chats").child(chatID).child("Info")
            .child("typing").child(FirebaseAuth.getInstance().uid!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    return
                }

                override fun onDataChange(p0: DataSnapshot) {
                    p0.ref.setValue(status)
                }
            })
    }

    private fun initContactsRecyclerView() {
        conversationAdapter =
            ConversationAdapter(this)
        rootView.chatMessagesRV.apply {
            layoutManager =
                LinearLayoutManager(
                    applicationContext,
                    LinearLayoutManager.VERTICAL,
                    false
                )
            adapter = conversationAdapter
        }
    }

    private fun fetchMessages() {
        val chatList: ArrayList<MessageData> = ArrayList()
        fetchMessagesChild = firebaseRef.child("Chats").child(chatID)
        fetchMessagesListener =
            fetchMessagesChild.addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.exists()) {
                        chatList.clear()
                        for (data in p0.child("messages").children) {
                            val chat: MessageData = data.getValue(MessageData::class.java)!!
                            chatList.add(chat)
                        }
                    }
                    seenMessages()
                    getUnseenMessages(chatID)
                    var myImage = ""
                    var userImage = ""
                    for (images in p0.child("Info").child("usersImage").children) {
                        if (images.key == FirebaseAuth.getInstance().uid) {
                            myImage = images.getValue(String::class.java)!!
                        } else
                            userImage = images.getValue(String::class.java)!!
                    }
                    conversationAdapter.addUsersImage(myImage, userImage)
                    conversationAdapter.addMessage(chatList)
                    rootView.chatMessagesRV.scrollToPosition(chatList.size - 1)
                }
            })
    }

    private fun deleteListeners() {
        typingChild.removeEventListener(typingListener)
        fetchMessagesChild.removeEventListener(fetchMessagesListener)
    }

    override fun onPause() {
        super.onPause()
        deleteListeners()
    }

    override fun onStart() {
        super.onStart()
        playSound()
        initArguments()
        iniButtons()
        getChatUsersUid()
        initContactsRecyclerView()
        getUserTypingStatus()
        editTextChanged()
        clickSendMessage()
        rootView.linearLayout.setOnClickListener { onBackPressed() }
    }

    override fun onResume() {
        super.onResume()
        fetchMessages()
    }

    private fun sendMessage(message: String, media: String, type: String) {
        val key = FirebaseDatabase.getInstance().reference.child("Chats").child(chatID)
            .child("messages").push().key!!
        FirebaseDatabase.getInstance().reference.child("Chats").child(chatID)
            .child("messages").child(key)
            .setValue(
                MessageData(
                    FirebaseAuth.getInstance().uid!!,
                    message,
                    mediaPath = media,
                    type = type
                )
            ).addOnSuccessListener {
                soundPool!!.play(send!!, 1f, 1f, 0, 0, 1f)
            }

        incrementUnreadValue(chatID, message, type)
    }

    private fun addMedia(media: Uri) {
        val fileName = "media/ ${UUID.randomUUID()}"
        val filepath = FirebaseStorage.getInstance().reference.child(fileName)
        filepath.putFile(media).addOnSuccessListener {
            filepath.downloadUrl.addOnSuccessListener {
                Toast.makeText(applicationContext, "Record Sent", Toast.LENGTH_SHORT)
                    .show()
                sendMessage("", it.toString(), "Voice Record")
            }
        }.addOnFailureListener {
            Toast.makeText(
                applicationContext,
                "Error with upload the image",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getChatUsersUid() {
        FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().uid!!).child("chat")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.exists())
                        for (data in p0.children) {
                            if (data.getValue(String::class.java) == chatID) {
                                userUid = data.key!!
                                checkUserMood()
                            }
                        }
                }
            })
    }

    private fun seenMessages() {
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
                    if (p0.exists()) {
                        val num = p0.getValue(String::class.java)
                        conversationAdapter.unSeenMessages(num!!.toInt())
                    }
                }
            })
    }

    private fun incrementUnreadValue(chatID: String, message: String, mediaType: String) {
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
                    p0.ref.child("mediaType").setValue(mediaType)
                    p0.ref.child("lastSender").setValue(FirebaseAuth.getInstance().uid!!)
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


    override fun onBackPressed() {
        val fragments = supportFragmentManager.fragments
        for (fragment in fragments) {
            if (fragment is DisplayImageFragment) {
                fragment.onBackPressed()
                return
            }
        }
        super.onBackPressed()
    }

    // userName -> Message
    override fun openUserImage(userImage: String, userName: String) {
        displayImage(userImage, true, userName)
    }

    override fun createMediaMessage(message: String, mediaPath: String) {
        sendMessage(message, mediaPath, "Photo")
    }

    override fun receivedNewMessage() {
        soundPool!!.play(receive!!, 1f, 1f, 0, 0, 1f)
    }
}