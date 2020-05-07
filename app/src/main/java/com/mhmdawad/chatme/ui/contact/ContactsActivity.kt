package com.mhmdawad.chatme.ui.contact

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.mhmdawad.chatme.utils.CountryISO
import com.mhmdawad.chatme.utils.RecyclerViewClick


class ContactsActivity : AppCompatActivity(), RecyclerViewClick {

    private lateinit var cursor: Cursor
    private lateinit var contactsRV: RecyclerView
    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var usersList: ArrayList<UserData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        initContactsRecyclerView()
        getContactsList()
    }

    private fun initContactsRecyclerView() {
        contactsAdapter = ContactsAdapter(this)
        contactsRV = findViewById(R.id.contactsRV)
        contactsRV.apply {
            layoutManager =
                LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, false)
            adapter = contactsAdapter
        }
    }

    private fun getContactsList() {
        usersList = ArrayList()
        cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            null
        )!!

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
            val data = UserData("", name, number)
            usersList.add(data)
            getUserInfo(data)
            contactsAdapter.addContacts(usersList)
        }
        cursor.close()

    }


    private fun getUserInfo(userData: UserData) {
        val userDB = FirebaseDatabase.getInstance().reference.child("Users")
        val query = userDB.orderByChild("Phone").equalTo(userData.Number)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    val index = usersList.indexOfFirst {
                        it.Number == userData.Number
                    }
                    usersList[index].haveAccount = true

                    for (data in p0.children) {
                        usersList[index].uid = data.child("Uid").value.toString()
                    }
                }

            }
        })
    }

    private fun getCountryISO(): String {
        val tm =
            this.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        var countryCodeValue = tm.networkCountryIso
        if (countryCodeValue == "")
            countryCodeValue = tm.simCountryIso

        return CountryISO.getPhone(countryCodeValue)!!

    }

    override fun onResume() {
        super.onResume()
        getContactsList()
    }

    override fun onStop() {
        super.onStop()
        cursor.close()
    }

    private fun createNewConversation(pos: Int):String {
        val key = FirebaseDatabase.getInstance().reference.child("chat").push().key!!
        FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().uid!!)
            .child("chat").child(usersList[pos].uid).setValue(key)
        FirebaseDatabase.getInstance().reference.child("Users").child(usersList[pos].uid)
            .child("chat").child(FirebaseAuth.getInstance().uid!!).setValue(key)
        FirebaseDatabase.getInstance().reference.child("Chats").child(key)
            .child("Info").setValue(MainChatData("","","","",""
            ,"",""))
        return key
    }

    private fun checkConversationStatus(pos: Int) {
        val myDatabase = FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().uid!!).child("chat")
        myDatabase.addValueEventListener(object: ValueEventListener {
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
                if(!isExist)
                     key = createNewConversation(pos)
             startConversationActivity(key)
            }
        })
    }

    private fun startConversationActivity(key: String){
        val intent = Intent(applicationContext,
            ConversationActivity::class.java)
        intent.putExtra("chatID", key)
        startActivity(intent)
    }

    override fun onItemClickedPosition(pos: Int) {
        if (usersList[pos].haveAccount) {
            checkConversationStatus(pos)
                Toast.makeText(
                    applicationContext,
                    "item clicked is ${usersList[pos].Name}",
                    Toast.LENGTH_SHORT
                ).show()
        } else {
            Toast.makeText(applicationContext, "invite ${usersList[pos].Name}", Toast.LENGTH_SHORT)
                .show()
        }

    }
}
