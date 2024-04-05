package pl.rodzon.chatwithme.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pl.rodzon.chatwithme.R
import pl.rodzon.chatwithme.activities.chat_screen.MessageChatApiInterface
import pl.rodzon.chatwithme.activities.main_screen.MainActivity
import pl.rodzon.chatwithme.adapter_classes.UserAdapter
import pl.rodzon.chatwithme.api_interface.UserApiInterface
import pl.rodzon.chatwithme.model.users.Chatlist
import pl.rodzon.chatwithme.model.users.UserJson
import pl.rodzon.chatwithme.model.users.UsersJson
import pl.rodzon.chatwithme.model.users.UsersWithMessagesJson
import pl.rodzon.chatwithme.utils.ChatContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


/**
 * A simple [Fragment] subclass.
 * Use the [ChatFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ChatFragment : Fragment() {
    private lateinit var currentLoggedUser: UserJson
    private var userAdapter: UserAdapter? = null
    private var mUsers: List<UserJson>? = null
    private var usersChatList: List<Chatlist>? = null
    lateinit var recyclerViewChatlist: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_chat, container, false)

        recyclerViewChatlist = view.findViewById(R.id.recycler_view_chatlist)
        recyclerViewChatlist.setHasFixedSize(true)
        recyclerViewChatlist.layoutManager = LinearLayoutManager(context)

        val activity: MainActivity? = activity as MainActivity?
        currentLoggedUser = activity!!.getCurrentUser()

        usersChatList = ArrayList()
        mUsers = ArrayList()

        getAllUsersFromRoomIdsByRoomId(view)
        //retrieveChatList(view)

        return view
    }

    private fun retrieveChatList(view: View) {
        getUsersFromServer(view)
    }

    private fun getAllUsersFromRoomIdsByRoomId(view: View) {
        val retrofitBuilder = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(ChatContext.getServerMessageURL())
            .build()
            .create(MessageChatApiInterface::class.java)

        val retrofitData = retrofitBuilder.getAllUsersFromRoomIdByUserId(currentLoggedUser.userID)

        retrofitData.enqueue(object : Callback<UsersWithMessagesJson?> {
            override fun onResponse(
                call: Call<UsersWithMessagesJson?>,
                response: Response<UsersWithMessagesJson?>
            ) {
                if (response.code() == 200) {
                    (usersChatList as ArrayList).clear()
                    val responseBody = response.body()
                    val jsonUsersIds = responseBody!!.usersIDs

                    for (userId in jsonUsersIds) {
                        (usersChatList as ArrayList).add(Chatlist(userId))
                    }

                } else {
                    Toast.makeText(
                        view.context,
                        "An error occurred on fetch users. Try again later.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<UsersWithMessagesJson?>, t: Throwable) {
                println(t.message)
                Toast.makeText(
                    view.context,
                    "Cannot connect to the server. Check your network connection.",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
        getUsersFromServer(view)
    }

    private fun getUsersFromServer(view: View) {
        val retrofitBuilder = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(ChatContext.getServerAuthorizationURL())
            .build()
            .create(UserApiInterface::class.java)

        val retrofitData = retrofitBuilder.getUsers(false)

        retrofitData.enqueue(object : Callback<UsersJson?> {
            override fun onResponse(call: Call<UsersJson?>, response: Response<UsersJson?>) {
                if (response.code() == 200) {
                    (mUsers as ArrayList).clear()
                    val responseBody = response.body()
                    val jsonUsers = responseBody!!.users

                    for (eachChatList in usersChatList!!) {
                        for (jsonUser in jsonUsers) {
                            if (eachChatList.getId() == jsonUser.userID) {
                                (mUsers as ArrayList).add(
                                    UserJson(
                                        jsonUser.userID,
                                        jsonUser.username,
                                        jsonUser.publicKey,
                                        jsonUser.picture,
                                    )
                                )
                            }
                        }
                    }
                    userAdapter = UserAdapter(
                        view.context,
                        (mUsers as ArrayList<UserJson>),
                        true,
                        currentLoggedUser
                    )
                    recyclerViewChatlist.adapter = userAdapter

                    userAdapter!!.notifyDataSetChanged()

                } else {
                    Toast.makeText(
                        view.context,
                        "An error occurred on fetch users. Try again later.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<UsersJson?>, t: Throwable) {
                println(t.message)
                Toast.makeText(
                    view.context,
                    "Cannot connect to the server. Check your network connection.",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }
}