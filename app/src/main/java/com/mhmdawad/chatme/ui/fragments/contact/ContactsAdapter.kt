package com.mhmdawad.chatme.ui.fragments.contact

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.databinding.ContactsRvItemsBinding
import com.mhmdawad.chatme.pojo.UserData
import com.mhmdawad.chatme.utils.CircleTransform
import com.mhmdawad.chatme.utils.RecyclerViewClick
import com.squareup.picasso.Picasso

class ContactsAdapter(private val contactsViewModel: ContactsViewModel) : RecyclerView.Adapter<ContactsAdapter.ContactsViewHolder>() {

    private val contactList: ArrayList<UserData> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactsViewHolder {
        val binding: ContactsRvItemsBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.contacts_rv_items,
            parent,
            false
        )
        return ContactsViewHolder(binding)
    }

    override fun getItemCount(): Int = contactList.size

    override fun onBindViewHolder(holder: ContactsViewHolder, position: Int) = holder.bind(contactList[position])

    inner class ContactsViewHolder(private val binding: ContactsRvItemsBinding) : RecyclerView.ViewHolder(binding.root){

        fun bind(user: UserData){
            binding.userData = user
            binding.contactsVM = contactsViewModel
            binding.executePendingBindings()
        }
    }

    fun addContacts(list: ArrayList<UserData>){
        contactList.clear()
        list.sortBy {it.Name}
        contactList.addAll(list.distinct())
        notifyDataSetChanged()
    }
}