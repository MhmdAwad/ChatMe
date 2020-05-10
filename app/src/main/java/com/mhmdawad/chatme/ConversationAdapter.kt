package com.mhmdawad.chatme

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.mhmdawad.chatme.pojo.MessageData
import com.mhmdawad.chatme.utils.RecyclerViewClick
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ConversationAdapter : RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    companion object {
        private const val VIEW_TYPE_MESSAGE_SENT = 1
        private const val VIEW_TYPE_MESSAGE_RECEIVED = 2
    }

    private val conversationList: ArrayList<MessageData> = ArrayList()

    override fun getItemViewType(position: Int): Int {
        return if (FirebaseAuth.getInstance().uid == conversationList[position].senderUid) {
            VIEW_TYPE_MESSAGE_SENT
        } else {
            VIEW_TYPE_MESSAGE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        return if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            ConversationViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.sent_message_rv_item,
                    parent,
                    false
                )
            )
        } else
            ConversationViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.receive_message_rv_items,
                    parent,
                    false
                )
            )
    }


    override fun getItemCount(): Int = conversationList.size

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) =
        holder.bind(conversationList[position])

    fun addMessage(message: ArrayList<MessageData>) {
        conversationList.clear()
        conversationList.addAll(message)
        conversationList.sortedBy{it.date}
        notifyDataSetChanged()
    }

    private var unseenNumber: Int = 0
    fun unSeenMessages(num: Int) {
        unseenNumber = num
        notifyDataSetChanged()
    }

    inner class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var messageBody: TextView = itemView.findViewById(R.id.text_message_body)
        private var messageDate: TextView = itemView.findViewById(R.id.messageDate)
        private var messageSeen: ImageView = itemView.findViewById(R.id.messageSeen)

        fun bind(user: MessageData) {
            messageBody.text = user.message
            messageDate.text = getDateFormat(user.date)
            Log.d("SeenX", "${(conversationList.size - unseenNumber) <= adapterPosition} - $adapterPosition - ${conversationList.size} - $unseenNumber")
            if((conversationList.size - unseenNumber) <= adapterPosition && unseenNumber != 0){
                messageSeen.setImageResource(R.drawable.ic_conversation_sent_message)
            }else{
                messageSeen.setImageResource(R.drawable.ic_conversation_seen_message)
            }
        }
    }

    private fun getDateFormat(date: String): String {
        val input = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
        val output = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val formatDate = input.parse(date)
        return output.format(formatDate!!)
    }
}