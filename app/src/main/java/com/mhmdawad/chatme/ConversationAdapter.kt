package com.mhmdawad.chatme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.mhmdawad.chatme.pojo.MessageData
import com.mhmdawad.chatme.utils.RecyclerViewClick

class ConversationAdapter() : RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    companion object {
    private const val VIEW_TYPE_MESSAGE_SENT = 1
    private const val VIEW_TYPE_MESSAGE_RECEIVED = 2
}

    private val conversationList: ArrayList<MessageData> = ArrayList()

    override fun getItemViewType(position: Int): Int {
        return if(FirebaseAuth.getInstance().uid == conversationList[position].senderUid){
            VIEW_TYPE_MESSAGE_SENT
        }else{
            VIEW_TYPE_MESSAGE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        return if(viewType == VIEW_TYPE_MESSAGE_SENT){
            ConversationViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.sent_message_rv_item, parent, false))
        }else
            ConversationViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.receive_message_rv_items, parent, false))
    }


    override fun getItemCount(): Int = conversationList.size

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) = holder.bind(conversationList[position])

    fun addMessage(message: ArrayList<MessageData>){
        conversationList.clear()
        conversationList.addAll(message)
        notifyDataSetChanged()
    }
    inner class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private var messageBody: TextView = itemView.findViewById(R.id.text_message_body)

        fun bind(user: MessageData){
            messageBody.text = user.message
        }
    }

}