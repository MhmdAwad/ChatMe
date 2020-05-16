package com.mhmdawad.chatme.ui.activities.main_page

import com.mhmdawad.chatme.R

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.mhmdawad.chatme.databinding.ActivityMainPageBinding
import com.mhmdawad.chatme.pojo.ConversationChatData
import com.mhmdawad.chatme.pojo.MainChatData
import com.mhmdawad.chatme.ui.fragments.conversation.ConversationFragment
import com.mhmdawad.chatme.ui.activities.login.LoginActivity
import com.mhmdawad.chatme.ui.fragments.contact.ContactsFragment
import com.mhmdawad.chatme.ui.fragments.settings.SettingsFragment
import com.mhmdawad.chatme.utils.Contacts
import com.mhmdawad.chatme.utils.RecyclerViewClick
import com.squareup.picasso.Picasso


class MainPageActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMainPageBinding
    private lateinit var mainPageViewModel: MainPageViewModel
    private lateinit var chatAdapter: MainChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_page)
        mainPageViewModel = ViewModelProvider(this).get(MainPageViewModel::class.java)
        binding.mainPageVM = mainPageViewModel
        binding.lifecycleOwner = this
    }

    private fun initSearchView(){
        binding.searchButton.setOnSearchClickListener { binding.chatText.visibility = View.GONE }
        binding.searchButton.setOnCloseListener {
            binding.chatText.visibility = View.VISIBLE
            return@setOnCloseListener false
        }

        binding.searchButton.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                chatAdapter.filter.filter(newText)
                return false
            }
        })
    }

    private fun fillAdapter() {
        mainPageViewModel.chatsLiveData().observe(this, Observer {
            for (chatData in it) {
                if (chatData.chatType == "direct")
                    chatData.offlineUserName =
                        getContactName(chatData.usersPhone[chatData.userUid]!!)
            }
            chatAdapter.addMainChats(it as ArrayList<MainChatData>)
        })
    }

    private fun getContactName(phoneNumber: String): String {
        return if (checkPermission())
            Contacts.getContactName(phoneNumber, this.applicationContext)
        else
            phoneNumber
    }

    private fun profileContacts() {
        mainPageViewModel.openContacts.observe(this, androidx.lifecycle.Observer {
            if (checkPermission()) {
                if (this.supportFragmentManager.findFragmentById(android.R.id.content) == null) {
                    this.supportFragmentManager.beginTransaction()
                        .add(android.R.id.content, ContactsFragment())
                        .addToBackStack(null)
                        .commit()
                }
            } else
                requestPermissions()
        })
    }

    private fun checkPermission(): Boolean {
        val readContact = Manifest.permission.READ_CONTACTS
        val writeContact = Manifest.permission.WRITE_CONTACTS
        return (ContextCompat.checkSelfPermission(
            this,
            writeContact
        ) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
            this,
            readContact
        ) == PackageManager.PERMISSION_GRANTED)
    }


    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_CONTACTS
                , Manifest.permission.WRITE_CONTACTS
            ), 100
        )
    }

    private fun logOut() {
        mainPageViewModel.logoutLiveData().observe(this, androidx.lifecycle.Observer {
            if (it) {
                val intent = Intent(applicationContext, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
        })
    }

    private fun mainItemsClicked() {
        binding.mainBottomAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menuSettings -> profileSettings()
                R.id.menuSarahah -> Toast.makeText(
                    applicationContext,
                    "SARAHAH",
                    Toast.LENGTH_SHORT
                ).show()
                R.id.menuStatus -> Toast.makeText(
                    applicationContext,
                    "Status",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun initRecyclerView() {
        chatAdapter =
            MainChatAdapter(mainPageViewModel)
        binding.mainRV.apply {
            layoutManager = LinearLayoutManager(
                applicationContext,
                LinearLayoutManager.VERTICAL, false
            )
            adapter =
                chatAdapter
        }
    }


    override fun onStart() {
        super.onStart()
        initRecyclerView()
        mainItemsClicked()
        fillAdapter()
        startConversationActivity()
        initSearchView()
        logOut()
        profileContacts()
        openUserImage()
    }


    private fun openUserImage() {
        mainPageViewModel.imageMutableLiveData.observe(this, Observer {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            val viewGroup: ViewGroup = this.findViewById(android.R.id.content)
            val dialogView: View =
                LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.show_image_screen, viewGroup, false)
            val userNameDialog: TextView = dialogView.findViewById(R.id.userNameDialog)
            val userImageDialog: ImageView = dialogView.findViewById(R.id.userImageDialog)

            userNameDialog.text = it.first
            if (it.second.startsWith("https://firebasestorage"))
                Picasso.get().load(it.second).into(userImageDialog)
            else
                userImageDialog.setImageResource(R.drawable.ic_default_user)

            builder.setView(dialogView)
            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
        })
    }

    private fun startConversationActivity() {
        mainPageViewModel.openChatConversation().observe(this, Observer {
            val conversationFragment =
                ConversationFragment.newInstance(
                    it.key, it.userName, it.userImage,
                    it.chatType, it.userUid
                )
            if (supportFragmentManager.findFragmentById(android.R.id.content) == null) {
                supportFragmentManager.beginTransaction()
                    .add(android.R.id.content, conversationFragment)
                    .addToBackStack(null)
                    .commit()
            }
        })
    }


    private fun profileSettings() {
        if (supportFragmentManager.findFragmentById(android.R.id.content) == null) {
            supportFragmentManager.beginTransaction()
                .add(android.R.id.content, SettingsFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onPause() {
        super.onPause()
        if (FirebaseAuth.getInstance().uid != null)
            mainPageViewModel.offlineMode()
    }


    override fun onResume() {
        super.onResume()
        mainPageViewModel.onlineMode()
    }

    override fun onBackPressed() {
        if(!binding.searchButton.isIconified) {
            binding.searchButton.isIconified = true
            return
        }
        if (supportFragmentManager.fragments.isNotEmpty()) {
            supportFragmentManager.popBackStack()
            return
        }
        super.onBackPressed()
    }

}
