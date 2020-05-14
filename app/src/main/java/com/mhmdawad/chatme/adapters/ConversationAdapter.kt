package com.mhmdawad.chatme.adapters

import android.graphics.Color
import android.media.MediaPlayer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.pojo.MessageData
import com.mhmdawad.chatme.utils.CircleTransform
import com.mhmdawad.chatme.utils.RecyclerViewClick
import com.squareup.picasso.Picasso
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class ConversationAdapter(
    private val clickedItem: RecyclerViewClick,
    private val chatType: String
) :
    RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    companion object {
        private const val VIEW_TYPE_MESSAGE_SENT = 1
        private const val VIEW_TYPE_MESSAGE_RECEIVED = 2
    }

    private var unseenNumber: Int = 0
    private var myImage: String = ""
    private var userImage: String = ""
    private val conversationList: ArrayList<MessageData> = ArrayList()
    private var mediaPlayer: MediaPlayer? = null
    private var timer: Timer? = null
    private var lastMessagesSize = 0
    private val userNameColors = HashMap<String, Int>()

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
        conversationList.sortedBy { it.date }
        if (lastMessagesSize < message.size && message[message.size - 1].senderUid != FirebaseAuth.getInstance().uid)
            clickedItem.receivedNewMessage()
        lastMessagesSize = message.size
        notifyDataSetChanged()
    }

    fun unSeenMessages(num: Int) {
        unseenNumber = num
        notifyDataSetChanged()
    }

    private val usersImages = HashMap<String, String>()
    private val usersNames = HashMap<String, String>()
    fun addUsersImage(usersImages: HashMap<String, String>) {
        this.usersImages.putAll(usersImages)
    }

    fun addUsersName(groupUsersName: HashMap<String, String>) {
        usersNames.putAll(groupUsersName)
    }

    inner class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
        , View.OnClickListener {
        private val messageBody: TextView = itemView.findViewById(R.id.text_message_body)
        private val imageBody: ImageView = itemView.findViewById(R.id.image_message_body)
        private val messageDate: TextView = itemView.findViewById(R.id.messageDate)
        private val messageSeen: ImageView = itemView.findViewById(R.id.messageSeen)
        private val recordContainer: LinearLayout = itemView.findViewById(R.id.recordContainer)
        private val recordImage: ImageView = itemView.findViewById(R.id.recordImage)
        private val recordPlay: ImageButton = itemView.findViewById(R.id.recordPlay)
        private val recordSeekBar: SeekBar = itemView.findViewById(R.id.recordSeekbar)
        private val recordMicImage: ImageView = itemView.findViewById(R.id.recordMicImage)
        private val groupUserName: TextView = itemView.findViewById(R.id.groupUserName)

        init {
            itemView.setOnClickListener(this)
            recordPlay.setOnClickListener {
                closeTimer(recordSeekBar)
                playRecord(conversationList[adapterPosition].mediaPath, recordSeekBar, recordPlay)
            }
        }

        fun bind(user: MessageData) {
            when (user.type) {
                "message" -> {
                    imageBody.visibility = View.GONE
                    recordContainer.visibility = View.GONE
                    messageBody.visibility = View.VISIBLE
                }
                "Voice Record" -> {
                    imageBody.visibility = View.GONE
                    recordContainer.visibility = View.VISIBLE
                    messageBody.visibility = View.GONE
                    if (usersImages[user.senderUid] != "")
                        Picasso.get().load(usersImages[user.senderUid]).transform(CircleTransform()).into(recordImage)
                    else
                        recordImage.setImageResource(R.drawable.ic_default_user)
                }
                "Photo" -> {
                    imageBody.visibility = View.VISIBLE
                    recordContainer.visibility = View.GONE
                    messageBody.visibility = View.VISIBLE
                    Picasso.get().load(user.mediaPath).into(imageBody)
                }
            }



            if (user.message == "")
                messageBody.visibility = View.GONE
            else {
                messageBody.text = user.message
                messageBody.visibility = View.VISIBLE
            }
            messageDate.text = getDateFormat(user.date)

            if (chatType == "direct")
                bindDirectChat()
            else if (chatType == "group")
                bindGroupChat(user)


        }

        private fun bindGroupChat(user: MessageData) {
            recordSeekBar.progressDrawable.setTint(Color.parseColor("#808080"))
            recordSeekBar.thumb.setTint(Color.parseColor("#808080"))
            recordMicImage.setImageResource(R.drawable.gray_microphone)
            if (user.senderUid != FirebaseAuth.getInstance().uid) {
                groupUserName.visibility = View.VISIBLE
                groupUserName.text = usersNames[user.senderUid]
                groupUserName.setTextColor(randomUserNameColor(user.senderUid))
            }
        }

        private fun bindDirectChat() {
            if ((conversationList.size - unseenNumber) <= adapterPosition && unseenNumber != 0) {
                bindSent()
                return
            } else {
                bindSeen()
            }
        }

        private fun bindSent() {
            recordSeekBar.progressDrawable.setTint(Color.parseColor("#25D366"))
            recordSeekBar.thumb.setTint(Color.parseColor("#25D366"))
            recordMicImage.setImageResource(R.drawable.sent_record)
            messageSeen.setImageResource(R.drawable.ic_done)
        }

        private fun bindSeen() {
            recordSeekBar.progressDrawable.setTint(Color.parseColor("#00BCD4"))
            recordSeekBar.thumb.setTint(Color.parseColor("#00BCD4"))
            recordMicImage.setImageResource(R.drawable.seen_record)
            messageSeen.setImageResource(R.drawable.ic_conversation_seen_message)
        }


        override fun onClick(v: View?) {
            if (conversationList[adapterPosition].mediaPath != "")
                if (conversationList[adapterPosition].type == "Photo") {
                    clickedItem.openUserImage(
                        conversationList[adapterPosition].mediaPath,
                        conversationList[adapterPosition].message
                    )
                }
        }
    }


    private fun playRecord(voiceLink: String, seekBar: SeekBar, recordPlay: ImageButton) {
        mediaPlayer = MediaPlayer()
        try {
            mediaPlayer?.setDataSource(voiceLink)
            mediaPlayer?.prepareAsync()
            mediaPlayer?.setOnPreparedListener {
                mediaPlayer?.start()
                seekBar.max = it.duration
                recordPlay.setImageResource(R.drawable.ic_pause_record)
                Log.d("Duration", "${it.duration}")
            }
        } catch (e: IOException) {
            Log.d("error", "$e")
        }
        mediaPlayer?.setOnCompletionListener {
            closeTimer(seekBar)
            mediaPlayer?.reset()
            recordPlay.setImageResource(R.drawable.ic_play_record)
        }
        seekBarDuration(seekBar)
    }


    private fun seekBarDuration(
        seekBar: SeekBar
    ) {
        timer = Timer()
        timer!!.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                seekBar.progress = mediaPlayer!!.currentPosition
            }
        }, 0, 100)
    }

    private fun closeTimer(seekBar: SeekBar) {
        if (timer != null) {
            timer!!.cancel()
            seekBar.progress = 0
        }
    }

    private fun getDateFormat(date: String): String {
        val input = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
        val output = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val formatDate = input.parse(date)
        return output.format(formatDate!!)
    }

    private fun randomUserNameColor(userName: String): Int {
        if (!userNameColors.containsKey(userName)) {
            val rnd = Random()
            val color = Color.argb(
                255,
                rnd.nextInt(256),
                rnd.nextInt(256),
                rnd.nextInt(256)
            )
            userNameColors[userName] = color
        }
        return userNameColors[userName]!!
    }

}


