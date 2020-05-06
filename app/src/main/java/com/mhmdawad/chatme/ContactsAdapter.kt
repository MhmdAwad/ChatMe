package com.mhmdawad.chatme

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactsAdapter : RecyclerView.Adapter<ContactsAdapter.ContactsViewHolder>() {

    private val contactList: ArrayList<UserData> = ArrayList()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactsViewHolder =
        ContactsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.contacts_rv_items, parent, false))

    override fun getItemCount(): Int = contactList.size

    override fun onBindViewHolder(holder: ContactsViewHolder, position: Int) = holder.bind(contactList[position])

    fun addContacts(list: ArrayList<UserData>){
        contactList.clear()
        contactList.addAll(list)
        notifyDataSetChanged()
    }
    inner class ContactsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var name: TextView = itemView.findViewById(R.id.contactNameTxt)
        private var number: TextView = itemView.findViewById(R.id.contactPhoneTxt)

        fun bind(user: UserData){
            name.text = user.name
            number.text = user.number
            Log.d("TAG", "name: $name \n number: $number")
        }
    }
}