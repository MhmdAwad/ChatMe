package com.mhmdawad.chatme.ui.fragments.contact

import android.content.Context
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.databinding.FragmentContactsBinding
import com.mhmdawad.chatme.pojo.ConversationChatData
import com.mhmdawad.chatme.pojo.UserData
import com.mhmdawad.chatme.ui.fragments.conversation.ConversationFragment
import com.mhmdawad.chatme.ui.fragments.create_group.CreateGroupFragment
import kotlinx.android.synthetic.main.include_toolbar.view.*


class ContactsFragment : Fragment() {

    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var binding: FragmentContactsBinding
    private lateinit var contactsViewModel: ContactsViewModel


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_contacts, container, false)
        contactsViewModel = ViewModelProvider(this, getViewModelFactory()).get(ContactsViewModel::class.java)
        return binding.root
    }


    private fun getViewModelFactory(): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return ContactsViewModel(
                    activity!!.contentResolver,
                    activity!!.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                ) as T
            }
        }
    }
    private fun initViews() {
        Log.d("ss", "HI")
        binding.contactToolbar.logoutButton.setOnClickListener { activity!!.supportFragmentManager.popBackStack() }
        binding.contactToolbar.toolbarName.text = resources.getString(R.string.contacts)
    }

    private fun createNewGroup(){
        binding.newGroup.setOnClickListener {
            contactsViewModel.getContactsLiveData().observe(this, Observer {
            activity!!.supportFragmentManager.popBackStack()
            val fragment = CreateGroupFragment.newInstance(it)
            activity!!.supportFragmentManager.beginTransaction()
                .add(android.R.id.content, fragment)
                .addToBackStack(null)
                .commit()
        }) }
    }

    private fun fillContactsAdapter(){
        contactsViewModel.getContactsLiveData().observe(this, Observer {
            contactsAdapter.addContacts(it)
        })
    }


    private fun initContactsRecyclerView() {
        contactsAdapter =
            ContactsAdapter(contactsViewModel)
        binding.contactsRV.apply {
            layoutManager =
                LinearLayoutManager(
                    activity!!.applicationContext,
                    LinearLayoutManager.VERTICAL,
                    false
                )
            adapter = contactsAdapter
        }
    }

    private fun startConversationActivity(conversationChatData: ConversationChatData) {
        val conversationFragment =
            ConversationFragment.newInstance(conversationChatData.key, conversationChatData.userName, conversationChatData.userImage,
                conversationChatData.chatType,conversationChatData.userUid)
        activity!!.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, conversationFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun observeConversationChat(){
        contactsViewModel.getConversationLiveData().observe(this, Observer {
            startConversationActivity(it)
        })
    }

    override fun onStart() {
        super.onStart()
        initViews()
        initContactsRecyclerView()
        contactsViewModel.getContactsList()
        createNewGroup()
        observeConversationChat()
        fillContactsAdapter()
    }
}