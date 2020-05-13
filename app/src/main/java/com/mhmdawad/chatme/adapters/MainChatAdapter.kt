package com.mhmdawad.chatme.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.pojo.MainChatData
import com.mhmdawad.chatme.utils.CircleTransform
import com.mhmdawad.chatme.utils.RecyclerViewClick
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainChatAdapter(private val clickedItem: RecyclerViewClick) :
    RecyclerView.Adapter<MainChatAdapter.ChatsViewHolder>(), Filterable {

    private var chatList: ArrayList<MainChatData> = ArrayList()
    private lateinit var chatListFull: ArrayList<MainChatData>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatsViewHolder =
        ChatsViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.main_chat_rv_items,
                parent,
                false
            )
        )

    override fun getItemCount(): Int = chatList.size

    override fun onBindViewHolder(holder: ChatsViewHolder, position: Int) {
        holder.bind(chatList[position])
    }

    fun addMainChats(user: ArrayList<MainChatData>) {
        chatList.clear()
        chatList.addAll(user)
        chatListFull = ArrayList(chatList)
        chatList.sortByDescending { it.lastMessageDate }
        notifyDataSetChanged()
    }

    inner class ChatsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var userName: TextView = itemView.findViewById(R.id.userName)
        private var lastMessage: TextView = itemView.findViewById(R.id.lastMessage)
        private var lastMessageDate: TextView = itemView.findViewById(R.id.lastMessageDate)
        private var unreadMessages: TextView = itemView.findViewById(R.id.unreadMessages)
        private var userImage: ImageView = itemView.findViewById(R.id.includeLayout)
        private var seenImageView: ImageView = itemView.findViewById(R.id.seenImageView)
        private var lastImageView: ImageView = itemView.findViewById(R.id.lastImageView)
        private val viewLine: View = itemView.findViewById(R.id.viewLine)
        private val container: ConstraintLayout = itemView.findViewById(R.id.container)
        private val insideContainer: ConstraintLayout = itemView.findViewById(R.id.constraintLayout)

        init {
            clickViews()
        }

        var selectedUserName = ""
        fun bind(user: MainChatData) {
            if (user.lastMessage != "" || user.mediaType == "Photo" || user.mediaType == "Voice Record") {
                container.visibility = View.VISIBLE

                when (user.mediaType) {
                    "Photo" -> {
                        lastImageView.visibility = View.VISIBLE
                        user.lastMessage = "Photo"
                    }
                    "Voice Record" -> {
                        lastImageView.visibility = View.VISIBLE
                        user.lastMessage = "Voice Record"
                    }
                    else -> lastImageView.visibility = View.GONE
                }

                if (user.unreadMessage[FirebaseAuth.getInstance().uid]!!.toInt() != 0) {
                    seenImageView.setImageResource(R.drawable.ic_conversation_seen_message)
                    lastMessageDate.setTextColor(Color.parseColor("#25D366"))
                    if (user.mediaType == "Voice Record")
                        lastImageView.setImageResource(R.drawable.seen_record)
                } else {
                    seenImageView.setImageResource(R.drawable.ic_conversation_sent_message)
                    lastMessageDate.setTextColor(Color.parseColor("#808080"))
                    if (user.mediaType == "Voice Record")
                        lastImageView.setImageResource(R.drawable.gray_microphone)
                }
                if (user.lastSender == FirebaseAuth.getInstance().uid!!) {
                    seenImageView.visibility = View.VISIBLE
                } else
                    seenImageView.visibility = View.GONE

                if (user.usersImage[chatList[adapterPosition].userUid]!!.startsWith("https://firebasestorage"))
                    Picasso.get().load(user.usersImage[chatList[adapterPosition].userUid])
                        .transform(CircleTransform()).into(userImage)
                else
                    userImage.setImageResource(R.drawable.ic_default_user)

                viewLine.visibility =
                    if (adapterPosition == chatList.size - 1) View.GONE else View.VISIBLE

                selectedUserName = if (chatList[adapterPosition].offlineUserName == "null")
                    chatList[adapterPosition].usersPhone[chatList[adapterPosition].userUid]!!
                else
                    chatList[adapterPosition].offlineUserName

                userName.text = selectedUserName
                lastMessage.text = user.lastMessage
                lastMessageDate.text = getDateFormat(user.lastMessageDate)

                val unread = user.unreadMessage[FirebaseAuth.getInstance().uid]
                if (unread == "0")
                    unreadMessages.visibility = View.GONE
                else {
                    unreadMessages.visibility = View.VISIBLE
                    unreadMessages.text = unread

                }
            } else
                container.visibility = View.GONE
        }

        private fun clickViews() {
            userImage.setOnClickListener {

                clickedItem.openUserImage(
                    chatList[adapterPosition].usersImage[chatList[adapterPosition].userUid]!!,
                    selectedUserName
                )
            }
            insideContainer.setOnClickListener {
                clickedItem.onChatClickedString(
                    chatList[adapterPosition].chatID,
                    userName.text.toString(),
                    chatList[adapterPosition].usersImage[chatList[adapterPosition].userUid]!!
                )
            }

        }
    }

    private fun getDateFormat(date: String): String {
        val input = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
        val check = SimpleDateFormat("dd", Locale.getDefault())
        val checkDate = input.parse(date)
        val output: SimpleDateFormat
        output =
            if (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) < (check.format(checkDate!!).toInt()))
                SimpleDateFormat("yy/MM/dd", Locale.getDefault())
            else
                SimpleDateFormat("hh:mm a", Locale.getDefault())

        val formatDate = input.parse(date)
        return output.format(formatDate!!)
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
