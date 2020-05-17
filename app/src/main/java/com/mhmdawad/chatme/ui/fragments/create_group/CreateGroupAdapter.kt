package com.mhmdawad.chatme.ui.fragments.create_group

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.pojo.UserData
import com.mhmdawad.chatme.utils.CircleTransform
import com.squareup.picasso.Picasso

class CreateGroupAdapter :
    RecyclerView.Adapter<CreateGroupAdapter.CreateGroupViewHolder>() {

    private val contactList= ArrayList<UserData>()
    private val checkedContactList= ArrayList<UserData>()

    fun addItems(contactList: ArrayList<UserData>){
        this.contactList.clear()
        contactList.sortBy { it.Name }
        this.contactList.addAll(contactList.distinct())
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreateGroupViewHolder =
        CreateGroupViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.create_group_rv_items, parent, false))

    override fun getItemCount(): Int = contactList.size

    override fun onBindViewHolder(holder: CreateGroupViewHolder, position: Int) = holder.bind(contactList[position])


    fun getCheckedList() = checkedContactList

    inner class CreateGroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private var name: TextView = itemView.findViewById(R.id.contactNameTxt)
        private var number: TextView = itemView.findViewById(R.id.contactPhoneTxt)
        private var image: ImageView = itemView.findViewById(R.id.includeLayout)
        private var container: ConstraintLayout = itemView.findViewById(R.id.container)
        private var checkBox: CheckBox = itemView.findViewById(R.id.checkBox)

        fun bind(user: UserData){
            if(user.image.startsWith("https://firebasestorage"))
                Picasso.get().load(user.image).transform(CircleTransform()).into(image)
            else
                image.setImageResource(R.drawable.ic_default_user)

            name.text = user.Name
            number.text = user.Statue
            if(user.haveAccount)
                container.visibility = View.VISIBLE
            else
                container.visibility  = View.GONE

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