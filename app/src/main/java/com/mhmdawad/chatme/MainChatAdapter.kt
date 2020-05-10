package com.mhmdawad.chatme

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mhmdawad.chatme.pojo.MainChatData
import com.mhmdawad.chatme.utils.RecyclerViewClick
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

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
        private var userImage: ImageView = itemView.findViewById(R.id.includeImage)
        private val viewLine: View = itemView.findViewById(R.id.viewLine)
        private val container: ConstraintLayout = itemView.findViewById(R.id.container)
        private val insideContainer: ConstraintLayout = itemView.findViewById(R.id.constraintLayout)

        init {
            clickViews()
        }

        fun bind(user: MainChatData) {
            if (user.lastMessage != "") {
                container.visibility = View.VISIBLE

                if (user.usersImage[chatList[adapterPosition].userUid]!!.startsWith("https://firebasestorage"))
                    Picasso.get().load(user.usersImage[chatList[adapterPosition].userUid]).into(userImage)

                viewLine.visibility =
                    if (adapterPosition == chatList.size - 1) View.GONE else View.VISIBLE

                if (chatList[adapterPosition].offlineUserName == "null")
                    userName.text =
                        chatList[adapterPosition].usersPhone[chatList[adapterPosition].userUid]
                else
                    userName.text = chatList[adapterPosition].offlineUserName

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

                clickedItem.openUserImage(chatList[adapterPosition].usersImage[chatList[adapterPosition].userUid]!!)
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
