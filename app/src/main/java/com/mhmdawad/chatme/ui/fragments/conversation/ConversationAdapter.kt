package com.mhmdawad.chatme.ui.fragments.conversation


import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.mhmdawad.chatme.BR
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.pojo.MessageData
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class ConversationAdapter(
    private val chatType: String,
    private val conversationViewModel: ConversationViewModel
) :
    RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    private val usersImages = HashMap<String, String>()
    private val usersNames = HashMap<String, String>()
    private var unseenNumber: Int = 0
    private val conversationList: ArrayList<MessageData> = ArrayList()

    override fun getItemViewType(position: Int): Int {
        return if (FirebaseAuth.getInstance().uid == conversationList[position].senderUid) {
            R.layout.sent_message_rv_item
        } else {
            R.layout.receive_message_rv_items
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val binding =
            DataBindingUtil.inflate<ViewDataBinding>(LayoutInflater.from(parent.context), viewType, parent, false)
        return ConversationViewHolder(binding)
    }



    override fun getItemCount(): Int = conversationList.size


    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int)
            = holder.bind(conversationList[position])


    inner class ConversationViewHolder(private val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: MessageData) {

            val unseenMessage = (conversationList.size - unseenNumber) <= adapterPosition && unseenNumber != 0
            binding.setVariable(BR.messageData, user)
            binding.setVariable(BR.usersImages, usersImages)
            binding.setVariable(BR.usersNames, usersNames)
            binding.setVariable(BR.chatType, chatType)
            binding.setVariable(BR.seenMessage,unseenMessage)
            binding.setVariable(BR.myUid, FirebaseAuth.getInstance().uid)
            binding.setVariable(BR.conversationVM, conversationViewModel)
            binding.executePendingBindings()
        }
    }

    fun unSeenMessages(num: Int) {
        unseenNumber = num
        notifyDataSetChanged()
    }

    fun addUsersImage(usersImages: HashMap<String, String>) {
        this.usersImages.putAll(usersImages)
    }

    fun addUsersName(groupUsersName: HashMap<String, String>) {
        usersNames.putAll(groupUsersName)
    }
    fun addMessage(message: ArrayList<MessageData>) {
        val index = conversationList.size-1
        conversationList.addAll(message)
        if(index > 1)
            notifyItemRangeInserted(index,message.size )
        else
            notifyDataSetChanged()
    }
}