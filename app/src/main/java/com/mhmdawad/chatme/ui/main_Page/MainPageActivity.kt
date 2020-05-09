package com.mhmdawad.chatme.ui.main_Page

import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mhmdawad.chatme.MainChatAdapter
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.pojo.MainChatData
import com.mhmdawad.chatme.ui.contact.ContactsFragment
import com.mhmdawad.chatme.ui.conversation.ConversationActivity
import com.mhmdawad.chatme.ui.main.MainActivity
import com.mhmdawad.chatme.ui.profile_setting.SettingsFragment
import com.mhmdawad.chatme.utils.RecyclerViewClick


class MainPageActivity : AppCompatActivity(), RecyclerViewClick {
    private lateinit var mainPageChatRV: RecyclerView
    private lateinit var chatAdapter: MainChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_page)

        val findUserFab = findViewById<FloatingActionButton>(R.id.findUserFab)
        checkPermission()
        initRecyclerView()
        fetchConversations()

        findUserFab.setOnClickListener {
            checkPermission()
            profileContacts()
        }
    }

    private fun fetchConversations() {
        FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().uid!!)
            .child("chat").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {
                    getChatInfo(p0)
                }
            })
    }

    private fun profileContacts(){
        if (supportFragmentManager.findFragmentById(android.R.id.content) == null) {
            supportFragmentManager.beginTransaction()
                .add(android.R.id.content, ContactsFragment())
                .addToBackStack(null)
                .commit()
            supportActionBar!!.hide()
        }
    }
    private fun getChatInfo(dataSnapShot: DataSnapshot) {
        val chatObject: ArrayList<MainChatData> = ArrayList()
        FirebaseDatabase.getInstance().reference.child("Chats")
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.exists()) {
                        for (data in dataSnapShot.children) {
                            val chatData =
                                p0.child(data.getValue(String::class.java)!!).child("Info")
                                    .getValue(MainChatData::class.java)!!
                            chatData.offlineUserName =
                                getContactName(chatData.usersPhone[data.key]!!)
                            chatObject.add(chatData)
                            chatAdapter.addMainChats(chatObject)
                        }
                        chatObject.clear()
                    }
                }
            })
    }

    private fun getContactName(number: String): String {
        var name = "null"
        val projection = arrayOf(
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup._ID
        )
        val contactUri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        val cursor: Cursor? =
            contentResolver.query(contactUri, projection, null, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                name =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME))
            }
            cursor.close()
        }
        return name
    }

    private fun startConversationActivity(key: String, userName: String) {
        val intent = Intent(
            applicationContext,
            ConversationActivity::class.java
        )
        intent.putExtra("chatID", key)
        intent.putExtra("userName", userName)
        startActivity(intent)
    }

    private fun initRecyclerView() {
        chatAdapter = MainChatAdapter(this)
        mainPageChatRV = findViewById(R.id.mainPageChatRV)
        mainPageChatRV.apply {
            layoutManager = LinearLayoutManager(
                applicationContext,
                LinearLayoutManager.VERTICAL, false
            )
            adapter = chatAdapter
        }
    }

    private fun signOut() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val readContact = android.Manifest.permission.READ_CONTACTS
            val writeContact = android.Manifest.permission.WRITE_CONTACTS
            if (checkCallingOrSelfPermission(writeContact) != PackageManager.PERMISSION_GRANTED ||
                checkCallingOrSelfPermission(readContact) != PackageManager.PERMISSION_GRANTED
            )
                requestPermissions(arrayOf(readContact, writeContact), 100)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_page_menu, menu)
        val searchItem = menu?.findItem(R.id.searchMenuItem)
        val searchView: SearchView = searchItem?.actionView as SearchView
        searchView.imeOptions = EditorInfo.IME_ACTION_DONE

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                chatAdapter.filter.filter(newText)
                return false
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.signOutMenuItem -> signOut()
            R.id.settingsMenuItem -> profileSettings()
        }
        return true
    }

    private fun profileSettings() {
        if (supportFragmentManager.findFragmentById(android.R.id.content) == null) {
            supportFragmentManager.beginTransaction()
                .add(android.R.id.content, SettingsFragment())
                .addToBackStack(null)
                .commit()
            supportActionBar!!.hide()
        }
    }

    override fun onItemClickedString(key: String, userName: String) {
        startConversationActivity(key, userName)
    }

    override fun onBackPressed() {
        val fragments = supportFragmentManager.fragments
        for (fragment in fragments) {
            if (fragment != null) {
                if (fragment is SettingsFragment)
                    fragment.onBackPressed()
                else if (fragment is ContactsFragment)
                    fragment.onBackPressed()
                return
            }
        }
        super.onBackPressed()
    }

    interface OnBackButtonPressed {
        fun onBackPressed()
    }
}
