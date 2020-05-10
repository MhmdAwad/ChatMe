package com.mhmdawad.chatme.ui.contact

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mhmdawad.chatme.ContactsAdapter
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.pojo.MainChatData
import com.mhmdawad.chatme.pojo.UserData
import com.mhmdawad.chatme.ui.conversation.ConversationActivity
import com.mhmdawad.chatme.ui.main_Page.MainPageActivity
import com.mhmdawad.chatme.utils.CountryISO
import com.mhmdawad.chatme.utils.RecyclerViewClick
import kotlinx.android.synthetic.main.fragment_contacts.view.*
import kotlinx.android.synthetic.main.include_toolbar.view.*


class ContactsFragment : Fragment(), RecyclerViewClick, MainPageActivity.OnBackButtonPressed {

    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var usersList: ArrayList<UserData>
    private lateinit var myPhoneNumber: String
    private lateinit var rootView: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_contacts, container, false)
        initViews()
        initContactsRecyclerView()
        getContactsList()
        return rootView
    }

    private fun initViews() {
        rootView.toolbarBackPress.setOnClickListener { onBackPressed() }
        rootView.toolbarName.text = "Contacts"
    }

    private fun initContactsRecyclerView() {
        contactsAdapter = ContactsAdapter(this)
        rootView.contactsRV.apply {
            layoutManager =
                LinearLayoutManager(
                    activity!!.applicationContext,
                    LinearLayoutManager.VERTICAL,
                    false
                )
            adapter = contactsAdapter
        }
    }

    private fun getContactsList() {
        usersList = ArrayList()
        getMyPhoneNumber("", 0, false)
        val cursor = activity!!.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            null
        )

        if (cursor != null) {
            while (cursor.moveToNext()) {
                val name =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                var number =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                number = number.replace(" ", "")
                number = number.replace("-", "")
                number = number.replace(")", "")
                number = number.replace("(", "")
                if (number[0] != '+') {
                    number = getCountryISO() + number
                }
                val data = UserData("", name, number, "")
                getUserInfo(data)
            }
            cursor.close()
        }
    }

    private fun getUserInfo(userData: UserData) {
        FirebaseDatabase.getInstance().reference.child("Users")
            .orderByChild("Phone").equalTo(userData.Number)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.exists()) {
                        for (data in p0.children) {
                            userData.uid = data.child("Uid").value.toString()
                            userData.Image = data.child("Image").value.toString()
                        }
                        userData.haveAccount = true
                        if (userData.Number != myPhoneNumber)
                            usersList.add(userData)
                        contactsAdapter.addContacts(usersList)
                    }

                }
            })
    }

    private fun getCountryISO(): String {
        val tm =
            activity!!.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        var countryCodeValue = tm.networkCountryIso
        if (countryCodeValue == "")
            countryCodeValue = tm.simCountryIso

        return CountryISO.getPhone(countryCodeValue)!!

    }

    override fun onResume() {
        super.onResume()
        getContactsList()
    }

    private fun createNewConversation(pos: Int): String {
        val key = FirebaseDatabase.getInstance().reference.child("chat").push().key!!
        FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().uid!!)
            .child("chat").child(usersList[pos].uid).setValue(key)
        FirebaseDatabase.getInstance().reference.child("Users").child(usersList[pos].uid)
            .child("chat").child(FirebaseAuth.getInstance().uid!!).setValue(key)
        getMyPhoneNumber(key, pos, true)
        return key
    }

    private fun getMyPhoneNumber(key: String, pos: Int, addUserPhone: Boolean) {
        FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().uid!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    return
                }

                override fun onDataChange(p0: DataSnapshot) {
                    myPhoneNumber = p0.child("Phone").getValue(String::class.java)!!
                    val myImage = p0.child("Image").getValue(String::class.java)!!
                    if (addUserPhone)
                        addUsersData(key, myPhoneNumber, myImage, pos)
                }
            })
    }

    private fun reuseHashMap(
        uid1: String,
        uid2: String,
        value1: String,
        value2: String
    ): HashMap<String, String> {
        val phones = HashMap<String, String>()
        phones[uid1] = value1
        phones[uid2] = value2
        return phones
    }

    private fun addUsersData(key: String, myPhone: String, myImage: String, pos: Int) {
        val data = MainChatData(
            key,
            "",
            usersPhone = reuseHashMap(
                FirebaseAuth.getInstance().uid!!,
                usersList[pos].uid,
                myPhone,
                usersList[pos].Number
            )
            ,
            unreadMessage = reuseHashMap(
                FirebaseAuth.getInstance().uid!!,
                usersList[pos].uid,
                "0",
                "0"
            )
            ,
            usersImage = reuseHashMap(
                FirebaseAuth.getInstance().uid!!,
                usersList[pos].uid,
                myImage,
                usersList[pos].Image
            )
        )

        FirebaseDatabase.getInstance().reference.child("Chats").child(key)
            .child("Info").setValue(data)

    }

    private fun checkConversationStatus(pos: Int) {
        FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().uid!!).child("chat")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {
                    var isExist = false
                    var key = ""
                    if (p0.exists()) {
                        for (data in p0.children) {
                            if (data.key == usersList[pos].uid) {
                                isExist = true
                                key = p0.child(data.key.toString()).getValue(String::class.java)!!
                                break
                            }
                        }
                    }
                    if (!isExist)
                        key = createNewConversation(pos)

                    startConversationActivity(key, usersList[pos].Name)
                }
            })
    }

    private fun startConversationActivity(key: String, name: String) {
        val intent = Intent(
            activity!!.applicationContext,
            ConversationActivity::class.java
        )
        intent.putExtra("chatID", key)
        intent.putExtra("userName", name)
        startActivity(intent)
    }

    override fun onItemClickedPosition(pos: Int) {
        if (usersList[pos].haveAccount) {
            checkConversationStatus(pos)
            Toast.makeText(
                activity!!.applicationContext,
                "item clicked is ${usersList[pos].Name}",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                activity!!.applicationContext,
                "invite ${usersList[pos].Name}",
                Toast.LENGTH_SHORT
            )
                .show()
        }
    }

    override fun onBackPressed() {
        activity!!.supportFragmentManager.popBackStack()
        (activity as MainPageActivity).supportActionBar!!.show()
    }
}