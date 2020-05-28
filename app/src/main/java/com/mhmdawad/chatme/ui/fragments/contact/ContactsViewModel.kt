package com.mhmdawad.chatme.ui.fragments.contact

import android.content.ContentResolver
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mhmdawad.chatme.pojo.ConversationChatData
import com.mhmdawad.chatme.pojo.MainChatData
import com.mhmdawad.chatme.pojo.UserData
import com.mhmdawad.chatme.utils.CountryISO

class ContactsViewModel(
    private val contentResolver: ContentResolver,
    private val telephonyManager: TelephonyManager
) : ViewModel() {


    private lateinit var myPhoneNumber: String
    private val conversationChatData = MutableLiveData<ConversationChatData>()
    private val contactsMutableLiveData = MutableLiveData<ArrayList<UserData>>()
    private val usersList = ArrayList<UserData>()
    fun getContactsLiveData(): LiveData<ArrayList<UserData>> = contactsMutableLiveData
    fun getConversationLiveData(): LiveData<ConversationChatData> = conversationChatData


    fun getContactsList() {
        getMyPhoneNumber("", null, false)
        val cursor = contentResolver.query(
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
                            userData.image = data.child("Image").value.toString()
                            userData.Statue = data.child("Status").value.toString()
                            userData.haveAccount = true
                            if (userData.Number != myPhoneNumber)
                                usersList.add(userData)
                        }
                        contactsMutableLiveData.postValue(usersList)
                    }
                }
            })
    }

    private fun getCountryISO(): String {
        var countryCodeValue = telephonyManager.networkCountryIso
        if (countryCodeValue == "")
            countryCodeValue = telephonyManager.simCountryIso

        return CountryISO.getPhone(countryCodeValue)!!
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
                user.image
            ),
            userUid = user.uid
        )

        FirebaseDatabase.getInstance().reference.child("Chats").child(key)
            .child("Info").setValue(data)

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


    fun checkConversationStatus(user: UserData) {
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

                    conversationChatData.value =
                        ConversationChatData(key, user.Name, user.image, "direct", user.uid)
                }
            })
    }
}