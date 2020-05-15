package com.mhmdawad.chatme.ui.fragments.contact

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mhmdawad.chatme.adapters.ContactsAdapter
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.pojo.MainChatData
import com.mhmdawad.chatme.pojo.UserData
import com.mhmdawad.chatme.ui.activities.conversation.ConversationFragment
import com.mhmdawad.chatme.ui.fragments.create_group.CreateGroupFragment
import com.mhmdawad.chatme.utils.CountryISO
import com.mhmdawad.chatme.utils.RecyclerViewClick
import kotlinx.android.synthetic.main.activity_conversation.view.*
import kotlinx.android.synthetic.main.fragment_contacts.view.*
import kotlinx.android.synthetic.main.include_toolbar.view.*


class ContactsFragment : Fragment(), RecyclerViewClick {

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
        createNewGroup()
        return rootView
    }

    private fun initViews() {
        rootView.logoutButton.setOnClickListener { activity!!.supportFragmentManager.popBackStack() }
        rootView.toolbarName.text = "Contacts"
    }

    private fun createNewGroup(){
        rootView.newGroup.setOnClickListener {
            activity!!.supportFragmentManager.popBackStack()
            val fragment = CreateGroupFragment.newInstance(usersList)
            activity!!.supportFragmentManager.beginTransaction()
                .add(android.R.id.content, fragment)
                .addToBackStack(null)
                .commit()
        }
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
        getMyPhoneNumber("", null, false)

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
                if (number[0] != '+')
                    number = getCountryISO() + number

                val data = UserData("", name, number, "", "")
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
                            userData.Statue = data.child("Status").value.toString()
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

    private fun createNewConversation(user: UserData): String {
        val key = FirebaseDatabase.getInstance().reference.child("chat").push().key!!
        FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().uid!!)
            .child("chat").child(user.uid).setValue(key)
        FirebaseDatabase.getInstance().reference.child("Users").child(user.uid)
            .child("chat").child(FirebaseAuth.getInstance().uid!!).setValue(key)
        getMyPhoneNumber(key, user, true)
        return key
    }

    private fun getMyPhoneNumber(key: String, user: UserData?, addUserPhone: Boolean) {
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
                        addUsersData(key, myPhoneNumber, myImage, user!!)
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

    private fun addUsersData(key: String, myPhone: String, myImage: String, user: UserData) {
        val data = MainChatData(
            key,
            "",
            usersPhone = reuseHashMap(
                FirebaseAuth.getInstance().uid!!,
                user.uid,
                myPhone,
                user.Number
            )
            ,
            unreadMessage = reuseHashMap(
                FirebaseAuth.getInstance().uid!!,
                user.uid,
                "0",
                "0"
            )
            ,
            usersImage = reuseHashMap(
                FirebaseAuth.getInstance().uid!!,
                user.uid,
                myImage,
                user.Image
            )
        )

        FirebaseDatabase.getInstance().reference.child("Chats").child(key)
            .child("Info").setValue(data)

    }

    private fun checkConversationStatus(user: UserData) {
        FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().uid!!).child("chat")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {
                    var isExist = false
                    var key = ""
                    if (p0.exists()) {
                        for (data in p0.children) {
                            if (data.key == user.uid) {
                                isExist = true
                                key = p0.child(data.key.toString()).getValue(String::class.java)!!
                                break
                            }
                        }
                    }
                    if (!isExist)
                        key = createNewConversation(user)


                    startConversationActivity(key, user.Name, user.Image, user.uid)
                }
            })
    }

    private fun startConversationActivity(key: String, name: String, image: String, uid: String) {
        val intent = Intent(activity!!.applicationContext, ConversationFragment::class.java)
        intent.putExtra("ChatID", key)
        intent.putExtra("userName", name)
        intent.putExtra("userImage", image)
        intent.putExtra("chatType", "direct")
        intent.putExtra("userUid", uid)

        startActivity(intent)
//        onBackPressed()
    }

    override fun onItemClickedPosition(data: UserData) {
        checkConversationStatus(data)
    }

//    override fun onBackPressed() {
//        activity!!.supportFragmentManager.popBackStack()
//        (activity as AppCompatActivity).supportActionBar!!.show()
//    }
}