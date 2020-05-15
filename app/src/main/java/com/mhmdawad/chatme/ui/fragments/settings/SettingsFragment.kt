package com.mhmdawad.chatme.ui.fragments.settings

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.mhmdawad.chatme.utils.CircleTransform
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.ui.activities.main_page.MainPageActivity
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_settings.view.*
import kotlinx.android.synthetic.main.include_toolbar.view.*
import java.util.*


class SettingsFragment : Fragment(){

    private lateinit var rootView: View
    private lateinit var profileImagePath: String
    private lateinit var oldStatus: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_settings, container, false)
        getUserData()
        rootView.changeProfileImage.setOnClickListener { chooseImage() }
        rootView.changeProfileStatus.setOnClickListener {
            if(!rootView.profileStatus.isEnabled) {
                oldStatus = rootView.profileStatus.text.toString()
                rootView.profileStatus.isEnabled = true
                rootView.changeProfileStatus.setImageResource(R.drawable.ic_done)
            }else{
                changeUserStatus(rootView.profileStatus.text.toString())
                rootView.profileStatus.isEnabled = false
                rootView.changeProfileStatus.setImageResource(R.drawable.ic_edit)
            }
        }
        rootView.logoutButton.setOnClickListener { activity!!.supportFragmentManager.popBackStack() }
        return rootView
    }


    private fun chooseImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), 101)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK && data != null && data.data != null) {
            changeProfileImage(data.data!!)
        }
    }

    private fun changeProfileImage(imagePath: Uri) {
        val fileName = "profileImages/ ${UUID.randomUUID()}"
        val filepath = FirebaseStorage.getInstance().reference.child(fileName)
        filepath.putFile(imagePath).addOnSuccessListener {
                Toast.makeText(activity?.applicationContext, "Image Changed", Toast.LENGTH_SHORT)
                    .show()
            filepath.downloadUrl.addOnSuccessListener {
                FirebaseDatabase.getInstance().reference.child("Users").child(FirebaseAuth.getInstance().uid!!)
                    .child("Image").setValue(it.toString())
                getUserData()
                addNewImageToChats(it.toString(), "chat")
                addNewImageToChats(it.toString(), "group")
            }
                deleteOldProfileImage()
            }.addOnFailureListener {
                Toast.makeText(
                    activity,
                    "Error with upload the image",
                    Toast.LENGTH_SHORT
                ).show()
            }

    }

    private fun addNewImageToChats(newImagePath: String, chatType: String) {
        FirebaseDatabase.getInstance().reference.child("Users").child(FirebaseAuth.getInstance().uid!!)
            .child(chatType).addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {
                    return
                }

                override fun onDataChange(p0: DataSnapshot) {
                    if(p0.exists())
                        for(data in p0.children){
                            FirebaseDatabase.getInstance().reference.child("Chats").child(data.getValue(String::class.java)!!)
                                .child("Info").child("usersImage").child(FirebaseAuth.getInstance().uid!!).setValue(newImagePath)

                        }
                }
            })
    }

    private fun deleteOldProfileImage() {
        if (profileImagePath != "")
            FirebaseStorage.getInstance().getReferenceFromUrl(profileImagePath).delete()
                .addOnSuccessListener {
                    Log.d("SettingsFragment", "deleted successfully $profileImagePath")
                }
                .addOnFailureListener {
                    Log.d("SettingsFragment", "error with deleting $profileImagePath")
                }
    }

    private fun changeUserStatus(status: String) {
        FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().uid!!)
            .child("Status").setValue(status).addOnSuccessListener {
                getUserData()
            }
    }

    private fun getUserData() {
        FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().uid!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    return
                }

                override fun onDataChange(p0: DataSnapshot) {
                    rootView.profileStatus.setText(p0.child("Status").getValue(String::class.java))
                    profileImagePath = p0.child("Image").getValue(String::class.java)!!
                    if (profileImagePath != "" && profileImagePath.startsWith("https://firebasestorage")) {
                        Picasso.get().load(profileImagePath).transform(CircleTransform()).into(rootView.imageView)
                    }
                }
            })
    }



}
