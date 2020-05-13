package com.mhmdawad.chatme.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.pojo.UserData
import com.mhmdawad.chatme.utils.CircleTransform
import com.mhmdawad.chatme.utils.RecyclerViewClick
import com.squareup.picasso.Picasso

class ContactsAdapter(private val clickedItem: RecyclerViewClick) : RecyclerView.Adapter<ContactsAdapter.ContactsViewHolder>() {

    private val contactList: ArrayList<UserData> = ArrayList()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactsViewHolder =
        ContactsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.contacts_rv_items, parent, false))

    override fun getItemCount(): Int = contactList.size

    override fun onBindViewHolder(holder: ContactsViewHolder, position: Int) = holder.bind(contactList[position])

    fun addContacts(list: ArrayList<UserData>){
        contactList.clear()
        list.sortBy {it.Name}
        contactList.addAll(list.distinct())
        notifyDataSetChanged()
    }
    inner class ContactsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener{
        private var name: TextView = itemView.findViewById(R.id.contactNameTxt)
        private var number: TextView = itemView.findViewById(R.id.contactPhoneTxt)
        private var image: ImageView = itemView.findViewById(R.id.includeLayout)
        private var container: ConstraintLayout = itemView.findViewById(R.id.container)

        init {
            itemView.setOnClickListener(this)
        }
        fun bind(user: UserData){
            if(user.Image.startsWith("https://firebasestorage"))
                Picasso.get().load(user.Image).transform(CircleTransform()).into(image)
            else
                image.setImageResource(R.drawable.ic_default_user)

            name.text = user.Name
            number.text = user.Statue
            if(user.haveAccount)
                container.visibility = View.VISIBLE
            else
                container.visibility  = View.GONE
        }

        override fun onClick(v: View?) {
            clickedItem.onItemClickedPosition(adapterPosition)
        }
    }
}