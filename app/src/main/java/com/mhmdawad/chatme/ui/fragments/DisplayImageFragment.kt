package com.mhmdawad.chatme.ui.fragments

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.storage.FirebaseStorage

import com.mhmdawad.chatme.R
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_display_image.view.*
import java.io.Serializable
import java.util.*

class DisplayImageFragment : Fragment(){

    private lateinit var rootView: View
    private lateinit var imageUri: Uri
    private lateinit var newMessageInstance: NewMessage

    companion object{
        fun newInstance(imageUri: String,message:String, hideViews: Boolean, instance: NewMessage): DisplayImageFragment {
            val fragment =
                DisplayImageFragment()
            val args = Bundle()
            args.putString("Image", imageUri)
            args.putBoolean("hide", hideViews)
            args.putString("message", message)
            args.putSerializable("instance", instance)
            fragment.arguments = args
            return fragment
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView =  inflater.inflate(R.layout.fragment_display_image, container, false)

        imageUri = Uri.parse(arguments!!.getString("Image"))
        newMessageInstance = arguments!!.getSerializable("instance") as NewMessage
        val hideViews= arguments!!.getBoolean("hide")
        val message= arguments!!.getString("message")
        if(hideViews){
            rootView.sendMediaFab.visibility =View.GONE
            rootView.mediaEditTxt.visibility =View.GONE
            rootView.captionTextView.visibility =View.VISIBLE
            rootView.captionTextView.text = message
            Picasso.get().load(imageUri).into(rootView.mediaImgView)
        }else{
            rootView.sendMediaFab.setOnClickListener {
                addMedia(rootView.mediaEditTxt.text.toString())
            }
            rootView.mediaImgView.setImageURI(imageUri)
        }




        return rootView
    }

    private fun addMedia(message: String) {
        val fileName = "media/ ${UUID.randomUUID()}"
        val filepath = FirebaseStorage.getInstance().reference.child(fileName)
        filepath.putFile(imageUri).addOnSuccessListener {
            filepath.downloadUrl.addOnSuccessListener {
                Toast.makeText(activity!!.applicationContext, "Image Sent", Toast.LENGTH_SHORT)
                    .show()
                newMessageInstance.createMediaMessage(message, it.toString())
                activity!!.supportFragmentManager.popBackStack()
            }
        }.addOnFailureListener {
            Toast.makeText(
                activity!!.applicationContext,
                "Error with upload the image",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    interface NewMessage : Serializable{
        fun createMediaMessage(message:String, mediaPath: String)
    }
}
