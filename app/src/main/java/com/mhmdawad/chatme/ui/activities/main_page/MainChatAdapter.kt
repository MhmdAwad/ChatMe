package com.mhmdawad.chatme.ui.activities.main_page


import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.*
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.databinding.MainChatRvItemsBinding
import com.mhmdawad.chatme.pojo.ConversationChatData
import com.mhmdawad.chatme.pojo.MainChatData
import kotlin.collections.ArrayList

class MainChatAdapter(private val mainPageViewModel: MainPageViewModel) :
    RecyclerView.Adapter<MainChatAdapter.ChatsViewHolder>(), Filterable {

    private var chatList: ArrayList<MainChatData> = ArrayList()
    private lateinit var chatListFull: ArrayList<MainChatData>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatsViewHolder {
        val binding: MainChatRvItemsBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.main_chat_rv_items,
            parent,
            false
        )
        return ChatsViewHolder(binding)
    }

    override fun getItemCount(): Int = chatList.size

    override fun onBindViewHolder(holder: ChatsViewHolder, position: Int) =
        holder.bind(chatList[position])

    fun addMainChats(user: ArrayList<MainChatData>) {
        chatList.clear()
        chatList.addAll(user)
        chatListFull = ArrayList(chatList)
        chatList.sortByDescending { it.lastMessageDate }
        notifyDataSetChanged()
    }


    inner class ChatsViewHolder(private val binding: MainChatRvItemsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: MainChatData) {
            if(user.chatType == "direct"){
                directChat(user)
            }else{
                groupChat(user)
            }
            binding.mainChatVM = mainPageViewModel
            binding.myUid = FirebaseAuth.getInstance().uid
            binding.mainChatData = user
            binding.executePendingBindings()
        }

        private fun directChat(user: MainChatData){
            val conversation = ConversationChatData(user.chatID, user.offlineUserName, user.usersImage[user.userUid]!!,user.chatType,user.userUid)
            binding.conversationData = conversation
        }

        private fun groupChat(user: MainChatData){
            val conversation = ConversationChatData(user.chatID, user.groupName, user.groupImage,user.chatType,"")
            binding.conversationData = conversation
        }
    }

    override fun getFilter(): Filter = chatFilter

    private val chatFilter: Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredChatList = ArrayList<MainChatData>()
            if (constraint == null || constraint.isEmpty())
                filteredChatList.addAll(chatListFull)
            else {
                val filteredString = constraint.toString().toLowerCase().trim()
                for (chatItem in chatListFull) {
                    if (chatItem.offlineUserName.toLowerCase().contains(filteredString))
                        filteredChatList.add(chatItem)

                }
            }
            val results = FilterResults()
            results.values = filteredChatList
            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            chatList = results?.values as ArrayList<MainChatData>
            notifyDataSetChanged()
        }
    }
}


