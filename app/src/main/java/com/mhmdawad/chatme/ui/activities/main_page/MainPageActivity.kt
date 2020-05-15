package com.mhmdawad.chatme.ui.activities.main_page

import com.mhmdawad.chatme.R

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.mhmdawad.chatme.adapters.MainChatAdapter
import com.mhmdawad.chatme.databinding.ActivityMainPageBinding
import com.mhmdawad.chatme.pojo.MainChatData
import com.mhmdawad.chatme.pojo.UserChatData
import com.mhmdawad.chatme.ui.activities.conversation.ConversationFragment
import com.mhmdawad.chatme.ui.activities.login.LoginActivity
import com.mhmdawad.chatme.ui.fragments.contact.ContactsFragment
import com.mhmdawad.chatme.ui.fragments.settings.SettingsFragment
import com.mhmdawad.chatme.utils.Contacts
import com.mhmdawad.chatme.utils.RecyclerViewClick
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class MainPageActivity : AppCompatActivity(), RecyclerViewClick {

    private lateinit var binding: ActivityMainPageBinding
    private lateinit var chatInfoListener: ValueEventListener
    private lateinit var chatInfoChild: DatabaseReference
    private lateinit var databaseRef: DatabaseReference
    private lateinit var fetchConversationChild: DatabaseReference
    private lateinit var fetchListener: ValueEventListener
    private lateinit var fetchGroupsChild: DatabaseReference
    private lateinit var fetchGroupsListener: ValueEventListener

    companion object {
        lateinit var chatAdapter: MainChatAdapter
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_page)

    }
    private fun mainItemsClicked(){
        binding.mainBottomAppBar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.menuSettings -> profileSettings()
                R.id.menuSarahah -> Toast.makeText(applicationContext, "SARAHAH", Toast.LENGTH_SHORT).show()
                R.id.menuStatus ->Toast.makeText(applicationContext, "Status", Toast.LENGTH_SHORT).show()
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun newContact(){
        binding.mainFAB.setOnClickListener {
            if (checkPermission())
                profileContacts()
            else
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.READ_CONTACTS
                        , Manifest.permission.WRITE_CONTACTS
                    ), 100
                )
        }
    }



    private fun changeStatusBarColors() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            window.statusBarColor = ContextCompat.getColor(this, R.color.whiteColor);
        }
    }


    private fun initRecyclerView() {
        databaseRef = FirebaseDatabase.getInstance().reference
        chatAdapter = MainChatAdapter(this)
        binding.mainRV.apply {
            layoutManager = LinearLayoutManager(
                applicationContext,
                LinearLayoutManager.VERTICAL, false
            )
            adapter =
                chatAdapter
        }
    }

    private fun fetchGroups(map: ArrayList<UserChatData>) {
        fetchGroupsChild = databaseRef.child("Users").child(FirebaseAuth.getInstance().uid!!)
            .child("group")
        fetchGroupsListener = fetchGroupsChild.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                return
            }

            override fun onDataChange(p0: DataSnapshot) {
                if(p0.exists()){
                    for (data in p0.children)
                        map.add(UserChatData("group", data.getValue(String::class.java)!!))

                }
                getChatInfo(map)
            }
        })
    }

    private fun fetchConversations() {
        fetchConversationChild = databaseRef.child("Users").child(FirebaseAuth.getInstance().uid!!)
            .child("chat")
        fetchListener = fetchConversationChild.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(p0: DataSnapshot) {
                val map = ArrayList<UserChatData>()
                if (p0.exists()) {
                    for (data in p0.children)
                        map.add(UserChatData(data.key!!, data.getValue(String::class.java)!!))

                }
                fetchGroups(map)
            }
        })
    }


    private fun checkPermission(): Boolean {
        val readContact = Manifest.permission.READ_CONTACTS
        val writeContact = Manifest.permission.WRITE_CONTACTS
        return (ContextCompat.checkSelfPermission(this, writeContact) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, readContact) == PackageManager.PERMISSION_GRANTED)
    }

    private fun profileContacts() {
        if (this.supportFragmentManager.findFragmentById(android.R.id.content) == null) {
            this.supportFragmentManager.beginTransaction()
                .add(android.R.id.content, ContactsFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun getChatInfo(myChat: ArrayList<UserChatData>) {
        val chatList: ArrayList<MainChatData> = ArrayList()
        chatInfoChild = databaseRef.child("Chats")
        chatInfoListener = chatInfoChild.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    for (data in myChat) {
                        if (p0.child(data.userChat).child("Info").exists()) {
                            val chatData =
                                p0.child(data.userChat).child("Info").getValue(MainChatData::class.java)!!
                            if (chatData.chatType == "direct")
                                chatData.offlineUserName =
                                    getContactName(chatData.usersPhone[data.userUid]!!)

                            chatData.userUid = data.userUid
                            chatList.add(chatData)
                            chatAdapter.addMainChats(chatList)
                        }
                    }
                }
                chatList.clear()
            }
        })
    }

    private fun getContactName(phoneNumber: String): String {
        if(checkPermission())
            return Contacts.getContactName(phoneNumber, this.applicationContext)
        else
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_CONTACTS
                    , Manifest.permission.WRITE_CONTACTS
                ), 100
            )
        return phoneNumber
    }

    override fun onChatClickedString(key: String, userName: String, userImage: String, chatType:String, userUid: String) {
        startConversationActivity(key, userName, userImage, chatType, userUid)
    }

    override fun openUserImage(userImage: String, userName: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        val viewGroup: ViewGroup = this.findViewById(android.R.id.content)
        val dialogView: View =
            LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.show_image_screen, viewGroup, false)
        val userNameDialog: TextView = dialogView.findViewById(R.id.userNameDialog)
        val userImageDialog: ImageView = dialogView.findViewById(R.id.userImageDialog)

        userNameDialog.text = userName
        if (userImage.startsWith("https://firebasestorage"))
            Picasso.get().load(userImage).into(userImageDialog)
        else
            userImageDialog.setImageResource(R.drawable.ic_default_user)

        builder.setView(dialogView)
        val alertDialog: AlertDialog = builder.create()
        alertDialog.show()
    }

    private fun deleteListeners() {
        fetchConversationChild.removeEventListener(fetchListener)
        chatInfoChild.removeEventListener(chatInfoListener)
        fetchGroupsChild.removeEventListener(fetchGroupsListener)
    }

    override fun onStart() {
        super.onStart()
        changeStatusBarColors()
        initRecyclerView()
        newContact()
        mainItemsClicked()
        binding.logoutButton.setOnClickListener { logOut() }
    }


    private fun startConversationActivity(
        key: String,
        userName: String,
        userImage: String,
        chatType: String,
        userUid: String
    ) {
        val conversationFragment = ConversationFragment.newInstance(key,userName, userImage, chatType, userUid)
        intent.putExtra("ChatID", key)
        intent.putExtra("userName", userName)
        intent.putExtra("userImage", userImage)
        intent.putExtra("chatType", chatType)
        intent.putExtra("userUid", userUid)
        if (supportFragmentManager.findFragmentById(android.R.id.content) == null) {
            supportFragmentManager.beginTransaction()
                .add(android.R.id.content, conversationFragment)
                .addToBackStack(null)
                .commit()
        }
    }


    private fun offlineMode(){
        FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().uid!!)
            .child("mood").setValue("Last seen ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())}")
    }

    private fun onlineMode(){
        FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().uid!!)
            .child("mood").setValue("online")
    }


    private fun logOut() {
        offlineMode()
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(applicationContext, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
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
        deleteListeners()
        if (FirebaseAuth.getInstance().uid != null)
            offlineMode()
    }



    override fun onResume() {
        super.onResume()
        fetchConversations()
        onlineMode()
    }

    override fun onBackPressed() {
        if(supportFragmentManager.fragments.isNotEmpty()){
            supportFragmentManager.popBackStack()
            return
        }
        super.onBackPressed()
    }

}
