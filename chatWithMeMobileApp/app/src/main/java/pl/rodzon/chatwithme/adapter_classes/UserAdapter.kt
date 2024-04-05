package pl.rodzon.chatwithme.adapter_classes

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import pl.rodzon.chatwithme.R
import pl.rodzon.chatwithme.activities.chat_screen.MessageChatActivity
import pl.rodzon.chatwithme.activities.main_screen.MainActivity
import pl.rodzon.chatwithme.model.users.UserJson

class UserAdapter(mContext: Context, mUsers: List<UserJson>, isChatCheck: Boolean, currentLoggedUser: UserJson) : RecyclerView.Adapter<UserAdapter.ViewHolder?>() {

    private val mContext: Context
    private val mUsers: List<UserJson>
    private val isChatCheck: Boolean
    private val currentLoggedUser: UserJson

    init {
        this.mContext = mContext
        this.mUsers = mUsers
        this.isChatCheck = isChatCheck
        this.currentLoggedUser = currentLoggedUser
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(mContext).inflate(R.layout.user_search_item_layout, parent, false)
        return UserAdapter.ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mUsers.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user: UserJson = mUsers[position]
        holder.userNameTxt.text = user.username
        Picasso.get().load(user.picture).placeholder(R.drawable.ic_profile).into(holder.profileImageView)

        holder.itemView.setOnClickListener() {
            val options = arrayOf<CharSequence>(
                "Send Message",
                "Visit Profile"
            )
            val builder: AlertDialog.Builder = AlertDialog.Builder(mContext)
            builder.setTitle("What do you want?")
            builder.setItems(options, DialogInterface.OnClickListener {dialog, position ->
                if (position == 0) {
                    val intent = Intent(mContext, MessageChatActivity::class.java)
                    intent.putExtra("user", user as java.io.Serializable)
                    intent.putExtra("currentLoggedUser", currentLoggedUser as java.io.Serializable)
                    mContext.startActivity(intent)
                    (mContext as Activity).finish()
                }
                if (position == 1) {

                }
            })
            builder.show()
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        var userNameTxt: TextView
        var profileImageView: CircleImageView
        var onlineImageView: CircleImageView
        var offlineImageView: CircleImageView
        var lastMessageTxt: TextView

        init {
            userNameTxt = itemView.findViewById(R.id.username)
            profileImageView = itemView.findViewById(R.id.profile_image)
            onlineImageView = itemView.findViewById(R.id.image_online)
            offlineImageView = itemView.findViewById(R.id.image_offline)
            lastMessageTxt = itemView.findViewById(R.id.message_last)
        }
    }
}