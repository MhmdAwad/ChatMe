package com.mhmdawad.chatme.ui.fragments.create_group

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager

import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.databinding.FragmentCreateGroupBinding
import com.mhmdawad.chatme.pojo.UserData
import com.mhmdawad.chatme.ui.fragments.conversation.ConversationFragment
import com.mhmdawad.chatme.utils.RecyclerViewClick

class CreateGroupFragment : Fragment(), RecyclerViewClick {

    private lateinit var binding: FragmentCreateGroupBinding
    private lateinit var dialog: Dialog
    private lateinit var createGroupViewModel: CreateGroupViewModel

    companion object {
        fun newInstance(serializableList: List<UserData>) = CreateGroupFragment().apply {
            arguments = bundleOf("contactsList" to serializableList)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_create_group, container, false)
        createGroupViewModel = ViewModelProvider(this).get(CreateGroupViewModel::class.java)
        binding.lifecycleOwner = this

        initRecyclerView()
        createGroupNameDialog()
        startConversationActivity()
        dialog = Dialog(activity!!)

        return binding.root
    }

    private fun initRecyclerView() {
        val usersList = arguments!!.getParcelableArrayList<UserData>("contactsList")!!.distinct()
        val contactsAdapter =
            CreateGroupAdapter(usersList as ArrayList<UserData>)
        binding.groupRV.apply {
            layoutManager =
                LinearLayoutManager(
                    activity!!.applicationContext,
                    LinearLayoutManager.VERTICAL,
                    false
                )
            adapter = contactsAdapter
        }
        val checkedList = contactsAdapter.getCheckedList()
        createGroupViewModel.getMyInfo(checkedList)
        binding.groupFAB.setOnClickListener {
            if (checkedList.size < 1)
                Toast.makeText(
                    activity!!.applicationContext,
                    "Minimum Contacts is two!!",
                    Toast.LENGTH_SHORT
                ).show()
            else
                dialog.show()
        }
    }


    private fun createGroupNameDialog() {
        createGroupViewModel.createGroupMutableLiveData.observe(this, Observer {
            if (it != null) {
                val list = it
                dialog.setContentView(R.layout.group_name_dialog)
                val groupName = dialog.findViewById(R.id.groupName) as TextView
                val createGroup = dialog.findViewById(R.id.createGroup) as Button
                createGroup.setOnClickListener {
                    if (groupName.text.isEmpty())
                        Toast.makeText(
                            activity!!,
                            "Please specify a group name..",
                            Toast.LENGTH_SHORT
                        ).show()
                    else {
                        createGroupViewModel.createNewGroup(list, groupName.text.toString())
                        dialog.dismiss()
                    }
                }
            }
        })
    }

    private fun startConversationActivity() {
        createGroupViewModel.openConversation.observe(this, Observer {
            if (it != null) {
                    val conversationFragment =
                        ConversationFragment.newInstance(it.first, it.second, "", "group", "")
                    activity!!.supportFragmentManager.beginTransaction()
                        .add(android.R.id.content, conversationFragment)
                        .addToBackStack(null)
                        .commit()
            }
        })
    }


}
