package pl.rodzon.chatwithme.adapter_classes

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.RelativeLayout.LayoutParams
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import pl.rodzon.chatwithme.R
import pl.rodzon.chatwithme.activities.chat_screen.MessageChatApiInterface
import pl.rodzon.chatwithme.activities.chat_screen.ViewFullImageActivity
import pl.rodzon.chatwithme.model.message.MessageJson
import pl.rodzon.chatwithme.model.users.UserJson
import pl.rodzon.chatwithme.utils.ChatContext
import pl.rodzon.chatwithme.utils.ImageUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.time.format.DateTimeFormatter

class ChatsAdapter(mContext: Context, mChatList: List<MessageJson>, imageUrl: String, currentLoggedUser: UserJson) : RecyclerView.Adapter<ChatsAdapter.ViewHolder?>() {

    private val mContext: Context
    private val mChatList: List<MessageJson>
    private val imageUrl: String
    private val currentLoggedUser: UserJson

    init {
        this.mContext = mContext
        this.mChatList = mChatList
        this.imageUrl = imageUrl
        this.currentLoggedUser = currentLoggedUser
    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ViewHolder {
        return if (position == 1) {
            val view: View = LayoutInflater.from(mContext).inflate(pl.rodzon.chatwithme.R.layout.message_item_right, parent, false)
            ViewHolder(view)
        } else {
            val view: View = LayoutInflater.from(mContext).inflate(pl.rodzon.chatwithme.R.layout.message_item_left, parent, false)
            ViewHolder(view)
        }
    }

    override fun getItemCount(): Int {
        return mChatList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val messagesJson: MessageJson = mChatList[position]

        Picasso.get().load(imageUrl).placeholder(R.drawable.ic_profile).into(holder.profile_image)

        //image messages
        if (messagesJson.image != null) { //TODO warunek na czy jest to wiadomosc image
            //image message - right side
            if (messagesJson.roomID!!.startsWith(currentLoggedUser.userID)) {
                holder.show_text_message!!.visibility = View.GONE
                holder.right_image_view!!.visibility = View.VISIBLE
                Picasso.get().load(ImageUtil.saveBase64ImageToFile(holder.itemView.context, messagesJson.image!!, System.currentTimeMillis().toString() + ".jpg")!!).into(holder.right_image_view!!)

                holder.right_image_view!!.setOnClickListener {
                    val options = arrayOf<CharSequence>("View Full Image", "Delete image", "Delete image for yourself", "Cancel")
                    var builder: AlertDialog.Builder = AlertDialog.Builder(holder.itemView.context)
                    builder.setTitle("What do you want?")
                    builder.setItems(options, DialogInterface.OnClickListener { dialog, which ->
                        if (which == 0) {
                            ChatContext.setViewFullImage(messagesJson.image!!)
                            val intent = Intent(holder.itemView.context, ViewFullImageActivity::class.java)
                            holder.itemView.context.startActivity(intent)
                        } else if (which == 1) {
                            deleteMessageByCreationTime(position, holder)
                            notifyDataSetChanged()
                        } else if (which == 2) {
                            deleteMessage(position, holder)
                            notifyDataSetChanged()
                        }
                    })
                    builder.show()
                }
            }
            //image message - left side
            else if (!messagesJson.roomID!!.startsWith(currentLoggedUser.userID)) {
                holder.show_text_message!!.visibility = View.GONE
                holder.left_image_view!!.visibility = View.VISIBLE
                Picasso.get().load(ImageUtil.saveBase64ImageToFile(holder.itemView.context, messagesJson.image!!, System.currentTimeMillis().toString() + ".jpg")!!).into(holder.left_image_view!!)

                holder.left_image_view!!.setOnClickListener {
                    val options = arrayOf<CharSequence>("View Full Image", "Delete image for yourself", "Cancel")
                    var builder: AlertDialog.Builder = AlertDialog.Builder(holder.itemView.context)
                    builder.setTitle("What do you want?")
                    builder.setItems(options, DialogInterface.OnClickListener { dialog, which ->
                        if (which == 0) {
                            ChatContext.setViewFullImage(messagesJson.image!!)
                            val intent = Intent(mContext, ViewFullImageActivity::class.java)
                            mContext.startActivity(intent)
                        } else if (which == 1) {
                            deleteMessage(position, holder)
                            notifyDataSetChanged()
                        }
                    })
                    builder.show()
                }
            }
        }
        //text messages
        else {
            if (messagesJson.roomID!!.startsWith(currentLoggedUser.userID)) {
                holder.right_image_view!!.visibility = View.GONE
                holder.show_text_message!!.visibility = View.VISIBLE
                holder.show_text_message!!.text = messagesJson.text

                holder.show_text_message!!.setOnClickListener {
                    val options = arrayOf<CharSequence>("Delete message", "Delete message for yourself", "Cancel")
                    var builder: AlertDialog.Builder = AlertDialog.Builder(holder.itemView.context)
                    builder.setTitle("What do you want?")
                    builder.setItems(options, DialogInterface.OnClickListener { dialog, which ->
                        if (which == 0) {
                            deleteMessageByCreationTime(position, holder)
                            notifyDataSetChanged()
                        } else if (which == 1) {
                            deleteMessage(position, holder)
                            notifyDataSetChanged()
                        }
                    })
                    builder.show()
                }
            }  else if (!messagesJson.roomID!!.startsWith(currentLoggedUser.userID)) {
                holder.left_image_view!!.visibility = View.GONE
                holder.show_text_message!!.visibility = View.VISIBLE
                holder.show_text_message!!.text = messagesJson.text

                holder.show_text_message!!.setOnClickListener {
                    val options = arrayOf<CharSequence>("Delete message for yourself", "Cancel")
                    var builder: AlertDialog.Builder = AlertDialog.Builder(holder.itemView.context)
                    builder.setTitle("What do you want?")
                    builder.setItems(options, DialogInterface.OnClickListener { dialog, which ->
                        if (which == 0) {
                            deleteMessage(position, holder)
                            notifyDataSetChanged()
                        }
                    })
                    builder.show()
                }
            }
        }

        //sent and seen message
        if (position == mChatList.size - 1) {
            if (false)  { //jak wiadomość jest już wyświetlona
                holder.text_seen!!.text = "Seen"
                if (messagesJson.image != null) { // jeśli wiadomość to zdjęcie
                    val lp: RelativeLayout.LayoutParams? = holder.text_seen!!.layoutParams as LayoutParams?
                    lp!!.setMargins(0, 245, 10, 0)
                    holder.text_seen!!.layoutParams = lp
                }
            } else { // jeśli wiadomość nie jest jeszcze wyświetlona
                holder.text_seen!!.text = "Sent"
                if (messagesJson.image != null) { // jeśli wiadomość to zdjęcie
                    val lp: RelativeLayout.LayoutParams? = holder.text_seen!!.layoutParams as LayoutParams?
                    lp!!.setMargins(0, 245, 10, 0)
                    holder.text_seen!!.layoutParams = lp
                }
            }
        } else {
            holder.text_seen!!.visibility = View.GONE
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var profile_image: CircleImageView? = null
        var show_text_message: TextView? = null
        var left_image_view: ImageView? = null
        var text_seen: TextView? = null
        var right_image_view: ImageView? = null

        init {
            profile_image = itemView.findViewById(R.id.profile_image)
            show_text_message = itemView.findViewById(R.id.show_text_message)
            left_image_view = itemView.findViewById(R.id.left_image_view)
            text_seen = itemView.findViewById(R.id.text_seen)
            right_image_view = itemView.findViewById(R.id.right_image_view)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (mChatList[position].roomID!!.startsWith(currentLoggedUser.userID)) {
            1
        } else {
            0
        }
    }

    private fun deleteMessage(position: Int, holder: ChatsAdapter.ViewHolder) {
        val retrofit = Retrofit.Builder()
            .baseUrl(ChatContext.getServerMessageURL())
            .build()

        val service = retrofit.create(MessageChatApiInterface::class.java)

        val call = service.deleteMessage(mChatList[position].id.toString())
        call!!.enqueue(object : Callback<Void?> {
                override fun onResponse(call: Call<Void?>, response: Response<Void?>) {
                    if (response.code() == 204) {
                        Toast.makeText(holder.itemView.context, "Message deleted", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(holder.itemView.context, "Error", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Void?>, t: Throwable) {
                    Toast.makeText(holder.itemView.context, "Error", Toast.LENGTH_SHORT).show()
                }
            })

        (mChatList as ArrayList).removeAt(position)

    }

    private fun deleteMessageByCreationTime(position: Int, holder: ChatsAdapter.ViewHolder) {
        val retrofit = Retrofit.Builder()
            .baseUrl(ChatContext.getServerMessageURL())
            .build()

        val service = retrofit.create(MessageChatApiInterface::class.java)

        val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val timeString: String = mChatList[position].messageTimeLocalDateTime!!.format(dateTimeFormatter)

        val call = service.deleteMessageByCreationTime(timeString, currentLoggedUser.username)
        call!!.enqueue(object : Callback<Void?> {
            override fun onResponse(call: Call<Void?>, response: Response<Void?>) {
                if (response.code() == 204) {
                    Toast.makeText(holder.itemView.context, "Message deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(holder.itemView.context, "Error", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void?>, t: Throwable) {
                Toast.makeText(holder.itemView.context, "Error", Toast.LENGTH_SHORT).show()
            }
        })

        (mChatList as ArrayList).removeAt(position)

    }
}