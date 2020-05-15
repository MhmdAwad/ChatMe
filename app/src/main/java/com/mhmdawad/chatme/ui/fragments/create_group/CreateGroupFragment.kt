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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.adapters.CreateGroupAdapter
import com.mhmdawad.chatme.pojo.MainChatData
import com.mhmdawad.chatme.pojo.UserData
import com.mhmdawad.chatme.ui.activities.conversation.ConversationFragment
import com.mhmdawad.chatme.utils.RecyclerViewClick
import kotlinx.android.synthetic.main.fragment_create_group.view.*

class CreateGroupFragment : Fragment(), RecyclerViewClick {

    private lateinit var rootView: View
    private lateinit var dialog: Dialog
    companion object {
        fun newInstance(serializableList: List<UserData>) = CreateGroupFragment().apply {
            arguments = bundleOf("contactsList" to serializableList)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_create_group, container, false)
        initRecyclerView()
        dialog = Dialog(activity!!)

        return rootView
    }

    private fun initRecyclerView() {
        val contactsAdapter = CreateGroupAdapter()
        rootView.groupRV.apply {
            layoutManager =
                LinearLayoutManager(
                    activity!!.applicationContext,
                    LinearLayoutManager.VERTICAL,
                    false
                )
            adapter = contactsAdapter
        }
        contactsAdapter.addItems(arguments!!.getParcelableArrayList<UserData>("contactsList")!!)
        val checkedList = contactsAdapter.getCheckedList()
        getMyInfo(checkedList)
        rootView.groupFAB.setOnClickListener {
            if (checkedList.size < 2)
                Toast.makeText(
                    activity!!.applicationContext,
                    "Minimum Contacts is two!!",
                    Toast.LENGTH_SHORT
                ).show()
            else
                dialog.show()
        }
    }

    private fun createNewGroup(list: List<UserData>, groupName:String) {
        val key = FirebaseDatabase.getInstance().reference.child("Chats").push().key!!
        val chat = MainChatData(
            chatID = key,
            lastMessage = "$groupName is Created",
            unreadMessage = usedHashMap(list, 0),
            usersPhone = usedHashMap(list, 1),
            offlineUserName = String(),
            usersImage = usedHashMap(list, 2),
            mediaType = String(),
            lastSender = String(),
            userUid = String(),
            chatType = "group",
            groupName = groupName,
            groupImage = ""
        )
        FirebaseDatabase.getInstance().reference.child("Chats").child(key).child("Info")
            .setValue(chat)
        addChatKeyToUsers(list, key, groupName)
    }

    private fun startConversationActivity(key: String, userName: String, userImage: String, chatType:String) {
        val intent = Intent(activity!!.applicationContext, ConversationFragment::class.java)
        intent.putExtra("ChatID", key)
        intent.putExtra("userName", userName)
        intent.putExtra("userImage", userImage)
        intent.putExtra("chatType", chatType)
        startActivity(intent)
        activity!!.supportFragmentManager.popBackStack()
    }

    private fun addChatKeyToUsers(list: List<UserData>, key: String, groupName: String) {
        for (i in list) {
            FirebaseDatabase.getInstance().reference.child("Users")
                .child(i.uid)
                .child("group").push().setValue(key)
        }
        startConversationActivity(key, groupName, "","group")
    }

    private fun createGroupNameDialog(list: List<UserData>) {
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
                createNewGroup(list, groupName.text.toString())
                dialog.dismiss()
            }
        }
    }

    private fun getMyInfo(list: ArrayList<UserData>) {
        FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().uid!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    return
                }

                override fun onDataChange(p0: DataSnapshot) {
                    val ui = p0.child("Uid").getValue(String::class.java)!!
                    val phone = p0.child("Phone").getValue(String::class.java)!!
                    val image = p0.child("Image").getValue(String::class.java)!!
                    list.add(UserData(ui, "", phone, image, "", true))

                    createGroupNameDialog(list)
                }
            })
    }

    private fun usedHashMap(list: List<UserData>, type: Int): HashMap<String, String> {
        val map = HashMap<String, String>()
        for (i in list.indices) {
            when (type) {
                0 -> map[list[i].uid] = "0"
                1 -> map[list[i].uid] = list[i].Number
                2 -> map[list[i].uid] = list[i].Image
            }

        }
        return map
    }
}
