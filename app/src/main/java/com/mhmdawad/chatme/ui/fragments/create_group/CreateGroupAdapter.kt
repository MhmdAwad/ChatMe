package com.mhmdawad.chatme.ui.fragments.create_group

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.databinding.ContactsRvItemsBinding
import com.mhmdawad.chatme.databinding.CreateGroupRvItemsBinding
import com.mhmdawad.chatme.pojo.UserData
import com.mhmdawad.chatme.utils.CircleTransform
import com.squareup.picasso.Picasso

class CreateGroupAdapter(private val contactList: ArrayList<UserData>) :
    RecyclerView.Adapter<CreateGroupAdapter.CreateGroupViewHolder>() {

    private val checkedContactList= ArrayList<UserData>()

    init {
        contactList.sortBy { it.Name }
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreateGroupViewHolder {
        val binding: CreateGroupRvItemsBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.create_group_rv_items,
            parent,
            false
        )
        return CreateGroupViewHolder(binding)
    }


    override fun getItemCount(): Int = contactList.size

    override fun onBindViewHolder(holder: CreateGroupViewHolder, position: Int) = holder.bind(contactList[position])


    fun getCheckedList() = checkedContactList

    inner class CreateGroupViewHolder(private val binding: CreateGroupRvItemsBinding) : RecyclerView.ViewHolder(binding.root){
        private var checkBox: CheckBox = itemView.findViewById(R.id.checkBox)

        fun bind(user: UserData){
            binding.usersData = user
            binding.executePendingBindings()

            checkBox.setOnClickListener {
                if(checkBox.isChecked) {
                    checkedContactList.add(contactList[adapterPosition])
                    Log.d("TT", "add ${contactList[adapterPosition].Name }")
                }
                else {
                    checkedContactList.removeAll { it.Number == contactList[adapterPosition].Number }
                    Log.d("TT", "remove ${contactList[adapterPosition].Name }")
                }
            }
        }

    }
}