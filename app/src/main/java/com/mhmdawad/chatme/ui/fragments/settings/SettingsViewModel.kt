package com.mhmdawad.chatme.ui.fragments.settings

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class SettingsViewModel(ss: String) : ViewModel() {

    var statusText = MutableLiveData<String>()
    var profileImagePath =MutableLiveData<String>()
    val toastMsg = MutableLiveData<String>()


    fun getUserData() {
        FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().uid!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    return
                }

                override fun onDataChange(p0: DataSnapshot) {
                    statusText.value = p0.child("Status").getValue(String::class.java)!!
                    profileImagePath.value = p0.child("Image").getValue(String::class.java)!!
                }
            })
    }

    fun changeProfileImage(imagePath: Uri) {
        val fileName = "profileImages/ ${UUID.randomUUID()}"
        val filepath = FirebaseStorage.getInstance().reference.child(fileName)
        filepath.putFile(imagePath).addOnSuccessListener {
            toastMsg.value = "Image Changed"
            filepath.downloadUrl.addOnSuccessListener {
                FirebaseDatabase.getInstance().reference.child("Users")
                    .child(FirebaseAuth.getInstance().uid!!)
                    .child("Image").setValue(it.toString())
                getUserData()
                addNewImageToChats(it.toString(), "chat")
                addNewImageToChats(it.toString(), "group")
            }
            deleteOldProfileImage()
        }.addOnFailureListener {
            toastMsg.value = "Error with upload the image"
        }
    }

    private fun addNewImageToChats(newImagePath: String, chatType: String) {
        FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().uid!!)
            .child(chatType).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    return
                }

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.exists())
                        for (data in p0.children) {
                            FirebaseDatabase.getInstance().reference.child("Chats")
                                .child(data.getValue(String::class.java)!!)
                                .child("Info").child("usersImage")
                                .child(FirebaseAuth.getInstance().uid!!).setValue(newImagePath)
                        }
                }
            })
    }


    private fun deleteOldProfileImage() {
        if (profileImagePath.value != "")
            FirebaseStorage.getInstance().getReferenceFromUrl(profileImagePath.value.toString()).delete()
                .addOnSuccessListener {
                    Log.d("SettingsFragment", "deleted successfully $profileImagePath")
                }
                .addOnFailureListener {
                    Log.d("SettingsFragment", "error with deleting $profileImagePath")
                }
    }

    fun changeUserStatus(status: String) {
        FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().uid!!)
            .child("Status").setValue(status).addOnSuccessListener {
                getUserData()
            }
    }


}