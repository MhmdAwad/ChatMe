package com.mhmdawad.chatme.ui.fragments.settings

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mhmdawad.chatme.utils.CircleTransform
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.databinding.FragmentSettingsBinding
import com.mhmdawad.chatme.pojo.ConversationInfo
import com.mhmdawad.chatme.ui.fragments.conversation.ConversationViewModel
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.include_toolbar.view.*
import java.io.File


class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var oldStatus: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings, container, false)
        settingsViewModel = ViewModelProvider(this, getViewModelFactory()).get(SettingsViewModel::class.java)
        binding.lifecycleOwner = this
        return binding.root
    }

    private fun getViewModelFactory(): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    ""

                ) as T
            }
        }
    }

    private fun statusListener() {
        binding.changeProfileStatus.setOnClickListener {
            if (!binding.profileStatus.isEnabled) {
                oldStatus = binding.profileStatus.text.toString()
                binding.profileStatus.isEnabled = true
                binding.changeProfileStatus.setImageResource(R.drawable.ic_done)
            } else {
                settingsViewModel.changeUserStatus(binding.profileStatus.text.toString())
                binding.profileStatus.isEnabled = false
                binding.changeProfileStatus.setImageResource(R.drawable.ic_edit)
            }
        }
    }

    private fun toastMessages() {
        settingsViewModel.toastMsg.observe(this, androidx.lifecycle.Observer {
            Toast.makeText(activity!!.applicationContext, it, Toast.LENGTH_SHORT).show()
        })
    }

    private fun profileStatus() {
        settingsViewModel.statusText.observe(this, androidx.lifecycle.Observer {
            binding.profileStatus.setText(it)
        })
    }

    private fun chooseImage() {
        binding.changeProfileImage.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), 101)
        }
    }

    private fun backPress() {
        binding.settingsToolbar.logoutButton.setOnClickListener {
            activity!!.supportFragmentManager.popBackStack()
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK && data != null && data.data != null) {
            settingsViewModel.changeProfileImage(data.data!!)
        }
    }

    private fun profileImage() {
        settingsViewModel.profileImagePath.observe(this, androidx.lifecycle.Observer {
            if (it.startsWith("https://firebasestorage"))
                Picasso.get().load(it).transform(CircleTransform()).into(binding.imageView)
            else
                binding.imageView.setImageResource(R.drawable.ic_default_user)
        })
    }


    override fun onStart() {
        super.onStart()
        chooseImage()
        backPress()
        settingsViewModel.getUserData()
        toastMessages()
        statusListener()
        profileStatus()
        profileImage()
    }
}
