package com.mhmdawad.chatme.ui.activities.main_page

import android.content.ContentResolver
import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.mhmdawad.chatme.pojo.ConversationChatData
import com.mhmdawad.chatme.pojo.MainChatData
import com.mhmdawad.chatme.utils.Contacts
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainPageViewModel(private val contentResolver: ContentResolver) : ViewModel() {

    private lateinit var databaseRef: DatabaseReference
    private lateinit var fetchConversationChild: DatabaseReference
    private lateinit var fetchListener: ValueEventListener
    private lateinit var fetchGroupsChild: DatabaseReference
    private lateinit var fetchGroupsListener: ValueEventListener
    private lateinit var chatInfoListener: ValueEventListener
    private lateinit var chatInfoChild: DatabaseReference
     var chatMutableLiveData = MutableLiveData<ConversationChatData>()
    private val logoutObserve = MutableLiveData<Boolean>().apply { value = false }
    private val chatsList = MutableLiveData<List<MainChatData>>()
    private val imageMutableLiveData = MutableLiveData<Pair<String, String>>()
    private var openContactsMutableLiveData = MutableLiveData<Boolean>().apply { value = false }
    fun openContactLiveData(): LiveData<Boolean> = openContactsMutableLiveData
    fun showImage(): LiveData<Pair<String, String>> = imageMutableLiveData
    fun logoutLiveData(): LiveData<Boolean> = logoutObserve
    fun openChatConversationLiveData(): LiveData<ConversationChatData> = chatMutableLiveData
    fun chatsLiveData(): LiveData<List<MainChatData>> = chatsList
    fun openContacts() { openContactsMutableLiveData.value = true }


    fun fetchConversations() {
        databaseRef = FirebaseDatabase.getInstance().reference
        fetchConversationChild = databaseRef.child("Users").child(FirebaseAuth.getInstance().uid!!)
            .child("chat")
        fetchListener = fetchConversationChild.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(p0: DataSnapshot) {
                val map = ArrayList<Pair<String, String>>()
                if (p0.exists()) {
                    for (data in p0.children)
                        map.add(Pair(data.key!!, data.getValue(String::class.java)!!))
                }

                fetchGroups(map)
            }
        })
    }
    private fun fetchGroups(map: ArrayList<Pair<String, String>>) {
        fetchGroupsChild = databaseRef.child("Users").child(FirebaseAuth.getInstance().uid!!)
            .child("group")
        fetchGroupsListener = fetchGroupsChild.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                return
            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    for (data in p0.children)
                        map.add(Pair("group", data.getValue(String::class.java)!!))

                }
                getChatsInfo(map)
            }
        })
    }

    private fun getChatsInfo(myChat: ArrayList<Pair<String, String>>) {
        val list: ArrayList<MainChatData> = ArrayList()
        chatInfoChild = databaseRef.child("Chats")
        chatInfoListener = chatInfoChild.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    for (data in myChat) {
                        if(p0.child(data.second).child("Info").exists()) {
                            val chatData =
                                p0.child(data.second).child("Info").getValue(MainChatData::class.java)!!
                            chatData.userUid = data.first
                            list.add(chatData)
                        }
                    }
                }
                getUsersNumbers(list)
                list.clear()
            }
        })
    }

    private fun getUsersNumbers(list: ArrayList<MainChatData>){
        for (chatData in list) {
            if (chatData.chatType == "direct")
                chatData.offlineUserName = getContactName(chatData.usersPhone[chatData.userUid]!!)
        }
        chatsList.value = list
    }
    private fun getContactName(phoneNumber: String): String {
        var userName = phoneNumber
        try {
            userName = Contacts.getContactName(phoneNumber, contentResolver)
        }
        catch (e: Exception) {
            Log.d("MainVM", "$e")
            return userName
        }
        return userName
    }

    fun openUserImage(userImage: String, userName: String) {
        imageMutableLiveData.value = Pair(userName, userImage)
    }

    fun openChatConversation(chatData: ConversationChatData) {
        chatMutableLiveData.postValue(chatData)
//        chatMutableLiveData.value = null
    }

    private fun deleteListeners() {
        fetchConversationChild.removeEventListener(fetchListener)
        chatInfoChild.removeEventListener(chatInfoListener)
        fetchGroupsChild.removeEventListener(fetchGroupsListener)
    }

    fun offlineMode() {
        FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().uid!!)
            .child("mood").setValue(
                "Last seen ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(
                    Date()
                )}"
            )
    }

    fun onlineMode() {
        FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().uid!!)
            .child("mood").setValue("online")
    }


    fun logOut() {
        offlineMode()
        FirebaseAuth.getInstance().signOut()
        logoutObserve.value = true
    }

    override fun onCleared() {
        super.onCleared()
        deleteListeners()
    }
}