package com.mhmdawad.chatme.ui.fragments.conversation

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.mhmdawad.chatme.utils.CircleTransform
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.databinding.ActivityConversationBinding
import com.mhmdawad.chatme.pojo.ConversationInfo

import com.mhmdawad.chatme.ui.fragments.DisplayImageFragment
import com.squareup.picasso.Picasso
import java.io.File

class ConversationFragment : Fragment(), DisplayImageFragment.NewMessage {

    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var userUid: String
    private lateinit var chatID: String
    private lateinit var chatType: String
    private lateinit var binding: ActivityConversationBinding
    private lateinit var conversationViewModel: ConversationViewModel


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

        fragmentArguments()
        conversationViewModel =
            ViewModelProvider(
                this, ConversationFactory(
                    ConversationInfo(
                        userUid, chatID,
                        chatType,
                        File(activity!!.getExternalFilesDir(null), "records")), activity!!.contentResolver
                )
            ).get(ConversationViewModel::class.java)
        binding.conversationVM = conversationViewModel
        binding.lifecycleOwner = this

        fillAdapterData()
        initViewListeners()
        initContactsRecyclerView()
        sendRecord()
        usersImages()
        observePermissions()
        toastMessages()
        unseenMessages()
        openUserImage()
        return binding.root
    }


    private fun fillAdapterData() {
        conversationViewModel.chatMessages().observe(this, androidx.lifecycle.Observer {
            conversationAdapter.addMessage(it)
            binding.chatMessagesRV.scrollToPosition(conversationAdapter.itemCount - 1)
        })
    }

    private fun toastMessages() {
        conversationViewModel.toastMessages.observe(this, Observer {
            Toast.makeText(activity!!.applicationContext, it, Toast.LENGTH_SHORT).show()
        })
    }

    private fun unseenMessages() {
        conversationViewModel.unseenMessages.observe(this, Observer {
            conversationAdapter.unSeenMessages(it)
        })
    }


    private fun usersImages() {
        conversationViewModel.usersData.observe(this, androidx.lifecycle.Observer {
            conversationAdapter.addUsersImage(it.first)
            conversationAdapter.addUsersName(it.second)
        })
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun sendRecord() {
        binding.recordButton.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (binding.messageEditText.text.isEmpty()) {
                    conversationViewModel.startRecording()
                    binding.sendButton.visibility = View.GONE
                }
            } else if (event.action == MotionEvent.ACTION_UP) {
                if (binding.messageEditText.text.isEmpty()) {
                    binding.sendButton.visibility = View.VISIBLE
                    conversationViewModel.stopRecording()
                }
            }
            return@setOnTouchListener false
        }
    }




    private fun observePermissions(){
        conversationViewModel.requestPermissions.observe(this, Observer {
            if(it)
                requestPermissions()
        })
    }
    private fun requestPermissions() {
        if(checkPermission())
            return
        ActivityCompat.requestPermissions(
            activity!!,
            arrayOf(WRITE_EXTERNAL_STORAGE, RECORD_AUDIO),
            100
        )
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
        chatType = arguments?.getString("chatType")!!
        val userImage = arguments!!.getString("userImage")!!
        userUid = arguments!!.getString("userUid", "")!!
        if (userImage.startsWith("https://firebasestorage"))
            Picasso.get().load(userImage).transform(CircleTransform()).into(binding.imageView)
        binding.userNameTxt.text = userName
    }


    private fun initViewListeners() {
        binding.cameraButton.setOnClickListener { chooseImage() }
        binding.backPress.setOnClickListener { activity!!.supportFragmentManager.popBackStack() }
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
            displayImageFragment(data?.data!!.toString(), false, "")
        }
    }

    private fun displayImageFragment(imageUri: String, hideViews: Boolean, message: String) {
        val fragment = DisplayImageFragment.newInstance(imageUri, message, hideViews, this)
        activity!!.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit()
    }


    private fun initContactsRecyclerView() {
        conversationAdapter =
            ConversationAdapter(
                chatType,
                conversationViewModel
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


    private fun openUserImage() {
        conversationViewModel.displayImageMutableLiveData.observe(this, Observer {
            displayImageFragment(it.first, true, it.second)
        })
    }

    override fun createMediaMessage(message: String, mediaPath: String) {
        conversationViewModel.sendMessage(message, mediaPath, "Photo")
    }

}