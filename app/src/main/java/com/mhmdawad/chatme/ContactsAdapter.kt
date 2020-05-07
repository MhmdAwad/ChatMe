package com.mhmdawad.chatme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mhmdawad.chatme.pojo.UserData
import com.mhmdawad.chatme.utils.RecyclerViewClick

class ContactsAdapter(private val clickedItem: RecyclerViewClick) : RecyclerView.Adapter<ContactsAdapter.ContactsViewHolder>() {

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
    inner class ContactsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener{
        private var name: TextView = itemView.findViewById(R.id.contactNameTxt)
        private var number: TextView = itemView.findViewById(R.id.contactPhoneTxt)
        private var inviteFriend: Button = itemView.findViewById(R.id.inviteFriend)

        init {
            itemView.setOnClickListener(this)
        }
        fun bind(user: UserData){
            name.text = user.Name
            number.text = user.Number
            if(!user.haveAccount)
                inviteFriend.visibility = View.VISIBLE
            else
                inviteFriend.visibility = View.GONE
        }

        override fun onClick(v: View?) {
            clickedItem.onItemClickedPosition(adapterPosition)
        }
    }
}