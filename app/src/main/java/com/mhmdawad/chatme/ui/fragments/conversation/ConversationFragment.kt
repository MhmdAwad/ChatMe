package com.mhmdawad.chatme.ui.fragments.conversation

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaRecorder
import android.media.SoundPool
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.mhmdawad.chatme.utils.CircleTransform
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.databinding.ActivityConversationBinding

import com.mhmdawad.chatme.pojo.MessageData
import com.mhmdawad.chatme.ui.fragments.DisplayImageFragment
import com.mhmdawad.chatme.utils.Contacts
import com.mhmdawad.chatme.utils.RecyclerViewClick
import com.squareup.picasso.Picasso
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ConversationFragment : Fragment(), DisplayImageFragment.NewMessage,
    RecyclerViewClick {

    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var firebaseRef: DatabaseReference
    private lateinit var typingListener: ChildEventListener
    private lateinit var fetchMessagesListener: ChildEventListener
    private lateinit var typingChild: DatabaseReference
    private lateinit var fetchMessagesChild: DatabaseReference
    private lateinit var userUid: String
    private lateinit var chatID: String
    private lateinit var binding: ActivityConversationBinding
    private var send: Int? = null
    private var receive: Int? = null
    private var soundPool: SoundPool? = null
    private var recorder: MediaRecorder? = null
    private lateinit var fileName: String

    companion object {
        fun newInstance(
            ChatID: String,
            userName: String,
            userImage: String,
            chatType: String,
            userUid: String
        ): ConversationFragment {
            val fragment = ConversationFragment()
            val args = Bundle()
            args.putString("ChatID", ChatID)
            args.putString("userName", userName)
            args.putString("userUid", userUid)
            args.putString("userImage", userImage)
            args.putString("chatType", chatType)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.activity_conversation, container, false)
        return binding.root
    }


    private fun clickSendMessage() {
        binding.sendButton.setOnClickListener {
            if (binding.messageEditText.text.isNotEmpty()) {
                sendMessage(binding.messageEditText.text.toString(), "", "message")
                binding.messageEditText.text.clear()
            }
        }
    }

    private fun changeStatusBarColors() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity!!.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            activity!!.window.statusBarColor =
                ContextCompat.getColor(activity!!, R.color.whiteColor)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun sendRecord() {
        var startTime: Long = 0
        binding.recordButton.setOnTouchListener { _, event ->

            if (event.action == MotionEvent.ACTION_DOWN) {
                if (binding.messageEditText.text.isEmpty()) {
                    startTime = System.nanoTime()
                    stopRecordPlayer()
                    voiceRecord()
                    binding.sendButton.visibility = View.GONE
                    userTypingStatus("recording audio..")
                }
            } else if (event.action == MotionEvent.ACTION_UP) {
                if (binding.messageEditText.text.isEmpty()) {
                    binding.sendButton.visibility = View.VISIBLE
                    val endTime = ((System.nanoTime() - startTime) / 1_000_000_000.0).toInt()
                    if (endTime < 1) {
                        Toast.makeText(
                            activity!!.applicationContext,
                            "Hold to record, release to send",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        if (checkPermission()) {
                            stopRecordPlayer()
                            addMedia(Uri.fromFile(File(fileName)))
                            userTypingStatus("")
                        } else
                            ActivityCompat.requestPermissions(
                                activity!!,
                                arrayOf(WRITE_EXTERNAL_STORAGE, RECORD_AUDIO),
                                100
                            )
                    }
                }
            }
            return@setOnTouchListener true
        }
    }

    private fun stopRecordPlayer() {
        if (recorder != null) {
            try {
                recorder?.release()
                recorder = null
            } catch (e: RuntimeException) {
            }
        }
    }

    private fun playSound() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        send = soundPool!!.load(activity!!, R.raw.send, 1)
        receive = soundPool!!.load(activity!!, R.raw.receive, 1)

    }

    private fun voiceRecord() {
        if (checkPermission()) {
            val file = File(activity!!.getExternalFilesDir(null), "records")
            if (!file.exists())
                file.mkdirs()

            fileName = "${file.absolutePath}/file.mp3"
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(fileName)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                Thread {
                    try {
                        prepare()
                    } catch (e: IOException) {
                        Log.e("LOG_TAG", "prepare() failed")
                    }
                    start()
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                activity!!,
                arrayOf(WRITE_EXTERNAL_STORAGE, RECORD_AUDIO),
                100
            )
        }
    }

    private fun checkPermission(): Boolean {
        val result = checkSelfPermission(
            activity!!.applicationContext,
            WRITE_EXTERNAL_STORAGE
        )
        val result1 =
            checkSelfPermission(activity!!.applicationContext, RECORD_AUDIO)
        return result == PackageManager.PERMISSION_GRANTED &&
                result1 == PackageManager.PERMISSION_GRANTED
    }

    private fun fragmentArguments() {
        chatID = arguments!!.getString("ChatID")!!
        val userName = arguments!!.getString("userName")!!
        val userImage = arguments!!.getString("userImage")!!
        userUid = arguments!!.getString("userUid", "")!!
        if (userImage.startsWith("https://firebasestorage"))
            Picasso.get().load(userImage).transform(CircleTransform()).into(binding.imageView)
        firebaseRef = FirebaseDatabase.getInstance().reference
        binding.userNameTxt.text = userName
        binding.backPress.setOnClickListener { activity!!.supportFragmentManager.popBackStack() }
    }

    private fun changeTypingVisibility() {
        if (binding.typingStatus.text.toString() == "")
            binding.typingStatus.visibility = View.GONE
        else
            binding.typingStatus.visibility = View.VISIBLE
    }

    private fun editTextChanged() {
        binding.messageEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed(userStoppedTyping, 500)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { return }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (binding.messageEditText.text.isEmpty()) {
                    binding.cameraButton.visibility = View.VISIBLE
                    binding.recordButton.visibility = View.VISIBLE
                } else {
                    binding.cameraButton.visibility = View.GONE
                    binding.recordButton.visibility = View.GONE
                    userTypingStatus("typing..")
                }
            }

            val handler = Handler()
            var userStoppedTyping = Runnable {
                userTypingStatus("")
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
                        binding.typingStatus.text = p0.getValue(String::class.java)!!
                        changeTypingVisibility()
                    }
                }
            })
    }

    private fun iniButtons() {
        binding.cameraButton.setOnClickListener {
            chooseImage()
        }
    }

    private fun getUserTypingStatus() {
        typingChild =
            firebaseRef.child("Chats").child(chatID).child("Info").child("typing").child(userUid)
        typingListener = typingChild.addChildEventListener(object : ChildEventListener {

            override fun onCancelled(p0: DatabaseError) {
                return
            }

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {
                return
            }

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {
                var typing = ""
                if (p0.key != FirebaseAuth.getInstance().uid)
                    typing = p0.getValue(String::class.java)!!

                if (groupUsersName.containsKey(p0.key) && typing != "")
                    typing = "${groupUsersName[p0.key]} $typing"

                binding.typingStatus.text = typing
                changeTypingVisibility()

                if (binding.typingStatus.text == "" && ::userUid.isInitialized)
                    checkUserMood()
            }

            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                return
            }

            override fun onChildRemoved(p0: DataSnapshot) {
                return
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
        if (resultCode == Activity.RESULT_OK && requestCode == 101) {
            displayImage(data?.data!!.toString(), false, "")
        }
    }

    private fun displayImage(imageUri: String, hideViews: Boolean, message: String) {
        val fragment = DisplayImageFragment.newInstance(imageUri, message, hideViews, this)
        activity!!.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit()
    }


    private fun userTypingStatus(status: String) {
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
            ConversationAdapter(
                this,
                arguments?.getString("chatType")!!
            )
        binding.chatMessagesRV.apply {
            layoutManager =
                LinearLayoutManager(
                    activity!!.applicationContext,
                    LinearLayoutManager.VERTICAL,
                    false
                )
            adapter = conversationAdapter
        }
    }

    private val groupUsersName = HashMap<String, String>()

    private fun newMessages() {
        fetchMessagesChild = firebaseRef.child("Chats").child(chatID).child("messages")
        fetchMessagesListener =
            fetchMessagesChild.addChildEventListener(object : ChildEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onChildMoved(p0: DataSnapshot, p1: String?) {
                }

                override fun onChildChanged(p0: DataSnapshot, p1: String?) {

                }

                override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                    conversationAdapter.addMessage(arrayListOf(p0.getValue(MessageData::class.java)!!))
                    binding.chatMessagesRV.scrollToPosition(conversationAdapter.itemCount - 1)
                }

                override fun onChildRemoved(p0: DataSnapshot) {
                }
            })
    }

    private fun fetchMessages() {
        val chatList: ArrayList<MessageData> = ArrayList()
        firebaseRef.child("Chats").child(chatID)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.exists()) {
                        chatList.clear()
                        for (data in p0.child("messages").children) {
                            val chat: MessageData = data.getValue(MessageData::class.java)!!
                            chatList.add(chat)
                        }
                    }

                    getUserTypingStatus()
                    if (arguments?.getString("chatType") == "direct") {
                        seenMessages()
                        getUnseenMessages(chatID)
                    } else
                        for (phones in p0.child("Info").child("usersPhone").children)
                            groupUsersName[phones.key!!] = (Contacts.getContactName(
                                phones.getValue(String::class.java)!!,
                                activity!!.applicationContext
                            ))

                    val usersImages = HashMap<String, String>()
                    for (images in p0.child("Info").child("usersImage").children)
                        usersImages[images.key!!] = images.getValue(String::class.java)!!


                    conversationAdapter.addUsersImage(usersImages)
                    conversationAdapter.addUsersName(groupUsersName)
                    conversationAdapter.addMessage(chatList)
                    binding.chatMessagesRV.scrollToPosition(chatList.size - 1)
                    newMessages()
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
        fragmentArguments()
        iniButtons()
        changeStatusBarColors()
        initContactsRecyclerView()
        editTextChanged()
        sendRecord()
        clickSendMessage()
        if (userUid != "")
            checkUserMood()
        binding.linearLayout.setOnClickListener { activity!!.supportFragmentManager.popBackStack() }
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
        val chatType = arguments?.getString("chatType")
        addLastMessageData(message, type, chatType!!)
        if (chatType == "direct")
            incrementUnreadValue()
    }

    private fun addMedia(mediaPath: Uri) {
        val fileName = "media/ ${UUID.randomUUID()}"
        val filepath = FirebaseStorage.getInstance().reference.child(fileName)
        filepath.putFile(mediaPath).addOnSuccessListener {
            filepath.downloadUrl.addOnSuccessListener {
                Toast.makeText(activity!!.applicationContext, "Record Sent", Toast.LENGTH_SHORT)
                    .show()
                sendMessage("", it.toString(), "Voice Record")
            }
        }.addOnFailureListener {
            Toast.makeText(
                activity!!.applicationContext,
                "Error with upload the image",
                Toast.LENGTH_SHORT
            ).show()
        }
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

    private fun addLastMessageData(message: String, mediaType: String, chatType: String) {
        val firebaseRef =
            FirebaseDatabase.getInstance().reference.child("Chats").child(chatID).child("Info")
        firebaseRef.ref.child("mediaType").setValue(mediaType)
        firebaseRef.ref.child("chatType").setValue(chatType)
        firebaseRef.ref.child("lastSender").setValue(FirebaseAuth.getInstance().uid!!)
        firebaseRef.ref.child("chatID").setValue(chatID)
        if (message == "") firebaseRef.ref.child("lastMessage").setValue(mediaType)
        else firebaseRef.ref.child("lastMessage").setValue(message)
        firebaseRef.ref.child("lastMessageDate").setValue(
            SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date())
        )
    }

    private fun incrementUnreadValue() {
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
                    p0.ref.child("unreadMessage").child(FirebaseAuth.getInstance().uid!!)
                        .setValue("0")
                }
            })
    }

    // userName -> Message
    override fun openUserImage(userImage: String, message: String) {
        displayImage(userImage, true, message)
    }

    override fun createMediaMessage(message: String, mediaPath: String) {
        sendMessage(message, mediaPath, "Photo")
    }

    override fun receivedNewMessage() {
        soundPool!!.play(receive!!, 1f, 1f, 0, 0, 1f)
    }

}