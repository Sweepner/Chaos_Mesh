package pl.rodzon.chatwithme.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pl.rodzon.chatwithme.R
import pl.rodzon.chatwithme.activities.main_screen.MainActivity
import pl.rodzon.chatwithme.adapter_classes.UserAdapter
import pl.rodzon.chatwithme.api_interface.UserApiInterface
import pl.rodzon.chatwithme.model.users.UserJson
import pl.rodzon.chatwithme.model.users.UsersJson
import pl.rodzon.chatwithme.utils.ChatContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


/**
 * A simple [Fragment] subclass.
 * Use the [SearchFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SearchFragment : Fragment() {
    private lateinit var currentLoggedUser: UserJson
    private var userAdapter: UserAdapter? = null
    private var mUsers: List<UserJson>? = null
    private var recyclerView: RecyclerView? = null
    private var searchEditText: EditText? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_search, container, false)

        val activity: MainActivity? = activity as MainActivity?
        currentLoggedUser = activity!!.getCurrentUser()

        recyclerView = view.findViewById(R.id.searchList)
        recyclerView!!.setHasFixedSize(true)
        recyclerView!!.layoutManager = LinearLayoutManager(context)

        mUsers = ArrayList()
        retrieveAllUsers(view)

        searchEditText = view.findViewById(R.id.searchUsersET)

        searchEditText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                searchUserInDatabase(p0.toString(), view)
            }

            override fun afterTextChanged(p0: Editable?) {

            }

        })

        return view
    }

    private fun retrieveAllUsers(view: View) {
        (mUsers as ArrayList).clear()
        getUsersFromServer(view)
        userAdapter = UserAdapter(context!!, mUsers!!, false, currentLoggedUser)
        recyclerView!!.adapter = userAdapter
    }

    private fun searchUserInDatabase(str: String, view: View) {
        (mUsers as ArrayList).clear()
        searchForUsers(str, view)
        /*userAdapter = UserAdapter(context!!, mUsers!!, false)
        recyclerView!!.adapter = userAdapter*/
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
                    val responseBody = response.body() //TODO fora i dodać one by one
                    val jsonUsers = responseBody!!.users
                    for (jsonUser in jsonUsers) {
                        (mUsers as ArrayList).add(UserJson(
                            jsonUser.userID,
                            jsonUser.username,
                            jsonUser.publicKey,
                            jsonUser.picture,
                        ))
                    }
                } else {
                        Toast.makeText(
                            view.context,
                            "An error occurred on registration process. Try again later.",
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

    private fun searchForUsers(str: String, view: View) {
        val retrofitBuilder = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(ChatContext.getServerAuthorizationURL())
            .build()
            .create(UserApiInterface::class.java)

        val retrofitData = retrofitBuilder.getUsers(str, false)

        retrofitData.enqueue(object : Callback<UsersJson?> {
            override fun onResponse(call: Call<UsersJson?>, response: Response<UsersJson?>) {
                if (response.code() == 200) {
                    val responseBody = response.body()
                    val jsonUsers = responseBody!!.users
                    for (jsonUser in jsonUsers) {
                        (mUsers as ArrayList).add(UserJson(
                            jsonUser.userID,
                            jsonUser.username,
                            jsonUser.publicKey,
                            jsonUser.picture,
                        ))
                    }
                    userAdapter!!.notifyDataSetChanged()
                } else {
                    Toast.makeText(
                        view.context,
                        "An error occurred on registration process. Try again later.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<UsersJson?>, t: Throwable) {
                Toast.makeText(
                    view.context,
                    "Cannot connect to the server. Check your network connection.",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

}