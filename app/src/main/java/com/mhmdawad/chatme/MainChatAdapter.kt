package com.mhmdawad.chatme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mhmdawad.chatme.pojo.MainChatData
import com.mhmdawad.chatme.utils.RecyclerViewClick

class MainChatAdapter(private val clickedItem: RecyclerViewClick) : RecyclerView.Adapter<MainChatAdapter.ChatsViewHolder>() {

    private val chatList: ArrayList<MainChatData> = ArrayList()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatsViewHolder =
        ChatsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.main_chat_rv_items, parent, false))

    override fun getItemCount(): Int = chatList.size

    override fun onBindViewHolder(holder: ChatsViewHolder, position: Int) = holder.bind(chatList[position])

    fun addMainChats(user: ArrayList<MainChatData>){
        chatList.clear()
        chatList.addAll(user)
        notifyDataSetChanged()
    }
    inner class ChatsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener{
        private var userName: TextView = itemView.findViewById(R.id.userName)
        private var lastMessage: TextView = itemView.findViewById(R.id.lastMessage)
        private var lastMessageDate: TextView = itemView.findViewById(R.id.lastMessageDate)
        private var unreadMessages: TextView = itemView.findViewById(R.id.unreadMessages)
        private var userImage: ImageView = itemView.findViewById(R.id.userImage)

        init {
            itemView.setOnClickListener(this)
        }
        fun bind(user: MainChatData){
            userName.text = user.userName
            lastMessage.text = user.lastMessage
            lastMessageDate.text = user.lastMessageDate
            unreadMessages.text = user.meUnreadMessages
        }

        override fun onClick(v: View?) {
            clickedItem.onItemClickedString(chatList[adapterPosition].chatID)
        }
    }
}