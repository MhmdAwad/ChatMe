//package com.mhmdawad.chatme.ui.fragments.main_fragments
//
//import android.Manifest
//import com.mhmdawad.chatme.R
//
//import android.app.AlertDialog
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat.checkSelfPermission
//import androidx.fragment.app.Fragment
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.database.*
//import com.mhmdawad.chatme.adapters.MainChatAdapter
//import com.mhmdawad.chatme.pojo.MainChatData
//import com.mhmdawad.chatme.pojo.UserChatData
//import com.mhmdawad.chatme.ui.activities.conversation.ConversationActivity
//import com.mhmdawad.chatme.ui.fragments.contact.ContactsFragment
//import com.mhmdawad.chatme.ui.fragments.settings.SettingsFragment
//import com.mhmdawad.chatme.utils.Contacts
//import com.mhmdawad.chatme.utils.RecyclerViewClick
//import com.squareup.picasso.Picasso
//import kotlinx.android.synthetic.main.fragment_main_messages.view.*
//
//
//class MainMessagesFragment : AppCompatActivity(), RecyclerViewClick {
//
//    private lateinit var rootView: View
//    private lateinit var chatInfoListener: ValueEventListener
//    private lateinit var chatInfoChild: DatabaseReference
//    private lateinit var databaseRef: DatabaseReference
//    private lateinit var fetchConversationChild: DatabaseReference
//    private lateinit var fetchListener: ValueEventListener
//    private lateinit var fetchGroupsChild: DatabaseReference
//    private lateinit var fetchGroupsListener: ValueEventListener
//
//    companion object {
//        lateinit var chatAdapter: MainChatAdapter
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.fragment_main_messages)
//
//        initRecyclerView()
//        rootView.findUserFab.setOnClickListener {
//            if (checkPermission())
//                profileContacts()
//            else
//                ActivityCompat.requestPermissions(
//                    this,
//                    arrayOf(
//                        Manifest.permission.READ_CONTACTS
//                        , Manifest.permission.WRITE_CONTACTS
//                    ), 100
//                )
//        }
//    }
//
//
//    private fun initRecyclerView() {
//        databaseRef = FirebaseDatabase.getInstance().reference
//        chatAdapter = MainChatAdapter(this)
//        rootView.mainPageChatRV.apply {
//            layoutManager = LinearLayoutManager(
//                applicationContext,
//                LinearLayoutManager.VERTICAL, false
//            )
//            adapter =
//                chatAdapter
//        }
//    }
//
//    private fun fetchGroups(map: ArrayList<UserChatData>) {
//        fetchGroupsChild = databaseRef.child("Users").child(FirebaseAuth.getInstance().uid!!)
//            .child("group")
//        fetchGroupsListener = fetchGroupsChild.addValueEventListener(object : ValueEventListener {
//            override fun onCancelled(p0: DatabaseError) {
//                return
//            }
//
//            override fun onDataChange(p0: DataSnapshot) {
//                if(p0.exists()){
//                    for (data in p0.children)
//                        map.add(UserChatData("group", data.getValue(String::class.java)!!))
//
//                }
//                getChatInfo(map)
//            }
//        })
//    }
//
//    private fun fetchConversations() {
//        fetchConversationChild = databaseRef.child("Users").child(FirebaseAuth.getInstance().uid!!)
//            .child("chat")
//        fetchListener = fetchConversationChild.addValueEventListener(object : ValueEventListener {
//            override fun onCancelled(p0: DatabaseError) {}
//
//            override fun onDataChange(p0: DataSnapshot) {
//                val map = ArrayList<UserChatData>()
//                if (p0.exists()) {
//                    for (data in p0.children)
//                        map.add(UserChatData(data.key!!, data.getValue(String::class.java)!!))
//
//                }
//                fetchGroups(map)
//            }
//        })
//    }
//
//
//    private fun checkPermission(): Boolean {
//        val readContact = Manifest.permission.READ_CONTACTS
//        val writeContact = Manifest.permission.WRITE_CONTACTS
//        return (checkSelfPermission(this, writeContact) == PackageManager.PERMISSION_GRANTED
//                || checkSelfPermission(this, readContact) == PackageManager.PERMISSION_GRANTED)
//    }
//
//    private fun profileContacts() {
//        if (this.supportFragmentManager.findFragmentById(android.R.id.content) == null) {
//            this.supportFragmentManager.beginTransaction()
//                .add(android.R.id.content, ContactsFragment())
//                .addToBackStack(null)
//                .commit()
//        }
//    }
//
//    private fun getChatInfo(myChat: ArrayList<UserChatData>) {
//        val chatList: ArrayList<MainChatData> = ArrayList()
//        chatInfoChild = databaseRef.child("Chats")
//        chatInfoListener = chatInfoChild.addValueEventListener(object : ValueEventListener {
//            override fun onCancelled(p0: DatabaseError) {}
//
//            override fun onDataChange(p0: DataSnapshot) {
//                if (p0.exists()) {
//                    for (data in myChat) {
//                        if (p0.child(data.userChat).child("Info").exists()) {
//                            val chatData =
//                                p0.child(data.userChat).child("Info").getValue(MainChatData::class.java)!!
//                            if (chatData.chatType == "direct")
//                                chatData.offlineUserName =
//                                    getContactName(chatData.usersPhone[data.userUid]!!)
//
//                            chatData.userUid = data.userUid
//                            chatList.add(chatData)
//                            chatAdapter.addMainChats(chatList)
//                        }
//                    }
//                }
//                chatList.clear()
//            }
//        })
//    }
//
//    private fun getContactName(phoneNumber: String): String {
//        if(checkPermission())
//            return Contacts.getContactName(phoneNumber, this.applicationContext)
//        else
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(
//                    Manifest.permission.READ_CONTACTS
//                    , Manifest.permission.WRITE_CONTACTS
//                ), 100
//            )
//        return phoneNumber
//    }
//
//    override fun onChatClickedString(key: String, userName: String, userImage: String, chatType:String, userUid: String) {
//        startConversationActivity(key, userName, userImage, chatType, userUid)
//    }
//
//    override fun openUserImage(userImage: String, userName: String) {
//        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
//        val viewGroup: ViewGroup = this.findViewById(android.R.id.content)
//        val dialogView: View =
//            LayoutInflater.from(viewGroup.context)
//                .inflate(R.layout.show_image_screen, viewGroup, false)
//        val userNameDialog: TextView = dialogView.findViewById(R.id.userNameDialog)
//        val userImageDialog: ImageView = dialogView.findViewById(R.id.userImageDialog)
//
//        userNameDialog.text = userName
//        if (userImage.startsWith("https://firebasestorage"))
//            Picasso.get().load(userImage).into(userImageDialog)
//        else
//            userImageDialog.setImageResource(R.drawable.ic_default_user)
//
//        builder.setView(dialogView)
//        val alertDialog: AlertDialog = builder.create()
//        alertDialog.show()
//    }
//
//    private fun deleteListeners() {
//        fetchConversationChild.removeEventListener(fetchListener)
//        chatInfoChild.removeEventListener(chatInfoListener)
//        fetchGroupsChild.removeEventListener(fetchGroupsListener)
//    }
//
//    override fun onStart() {
//        super.onStart()
//        fetchConversations()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        deleteListeners()
//    }
//
//
//
//    private fun startConversationActivity(
//        key: String,
//        userName: String,
//        userImage: String,
//        chatType: String,
//        userUid: String
//    ) {
//        val intent = Intent(this.applicationContext, ConversationActivity::class.java)
//        intent.putExtra("ChatID", key)
//        intent.putExtra("userName", userName)
//        intent.putExtra("userImage", userImage)
//        intent.putExtra("chatType", chatType)
//        intent.putExtra("userUid", userUid)
//        startActivity(intent)
//    }
//
//    private fun profileSettings() {
//        if (supportFragmentManager.findFragmentById(android.R.id.content) == null) {
//            supportFragmentManager.beginTransaction()
//                .add(android.R.id.content, SettingsFragment())
//                .addToBackStack(null)
//                .commit()
//        }
//    }
//
//    override fun onPause() {
//        super.onPause()
//        if (FirebaseAuth.getInstance().uid != null)
//            FirebaseDatabase.getInstance().reference.child("Users")
//                .child(FirebaseAuth.getInstance().uid!!)
//                .child("mood").setValue("")
//    }
//
//    override fun onResume() {
//        super.onResume()
//        FirebaseDatabase.getInstance().reference.child("Users")
//            .child(FirebaseAuth.getInstance().uid!!)
//            .child("mood").setValue("online")
//    }
//
//
//}
