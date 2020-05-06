package com.mhmdawad.chatme

import android.database.Cursor
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ContactsActivity : AppCompatActivity() {

    private var cursor: Cursor? = null
    private lateinit var contactsRV: RecyclerView
    private lateinit var contactsAdapter:ContactsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        initContactsRecyclerView()
        getContactsList()
    }

    private fun initContactsRecyclerView(){
        contactsAdapter = ContactsAdapter()
        contactsRV = findViewById(R.id.contactsRV)
        contactsRV.apply {  layoutManager = LinearLayoutManager(applicationContext,LinearLayoutManager.VERTICAL,false)
            adapter = contactsAdapter

        }
    }
    private fun getContactsList(){
        val usersList:ArrayList<UserData> = ArrayList()
        cursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,null,null,null,null)
        while (cursor?.moveToNext()!!){
            val name = cursor?.getString(cursor?.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)!!)
            val number = cursor?.getString(cursor?.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)!!)
            usersList.add(UserData(name!!, number!!))
        }
        contactsAdapter.addContacts(usersList)
    }

    override fun onResume() {
        super.onResume()
        getContactsList()
    }

    override fun onDestroy() {
        super.onDestroy()
        cursor?.close()
    }
}
