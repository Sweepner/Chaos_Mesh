package pl.rodzon.chatwithme.activities.main_screen

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import pl.rodzon.chatwithme.R
import pl.rodzon.chatwithme.activities.login_screen.LoginApiInterface
import pl.rodzon.chatwithme.activities.welcome_screen.WelcomeActivity
import pl.rodzon.chatwithme.fragments.ChatFragment
import pl.rodzon.chatwithme.fragments.SearchFragment
import pl.rodzon.chatwithme.fragments.SettingsFragment
import pl.rodzon.chatwithme.model.users.UserJson
import pl.rodzon.chatwithme.utils.*
import pl.rodzon.chatwithme.utils.KeySaver.REQUEST_CODE_SAVE
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class MainActivity : AppCompatActivity() {
    private lateinit var currentLoggedUser: UserJson
    private lateinit var keySaver: KeySaver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.currentLoggedUser = intent.extras?.get("user") as UserJson

        if (!isMyServiceRunning(MyBackgroundService::class.java)) {
            startForegroundService(Intent(this, MyBackgroundService::class.java))
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar_main)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = ""

        val tabLayout: TabLayout = findViewById(R.id.tab_layout)
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        val viewPagerAdapter = ViewPagerAdapter(supportFragmentManager)

        viewPagerAdapter.addFragment(ChatFragment(), "Chats")
        viewPagerAdapter.addFragment(SearchFragment(), "Search")
        viewPagerAdapter.addFragment(SettingsFragment(), "Settings")

        viewPager.adapter = viewPagerAdapter

        tabLayout.setupWithViewPager(viewPager)

        //display username and profile picture
        var userTextView: TextView = findViewById(R.id.user_name)
        var userPicture: CircleImageView = findViewById(R.id.profile_image)
        userTextView.text = currentLoggedUser.username
        Picasso.get().load(currentLoggedUser.picture.toString()).placeholder(R.drawable.ic_profile)
            .into(userPicture)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_logout -> {
                ChatContext.setPrivateKey("")
                LocalStorageAccess.deleteFileFromInternalStorage(this@MainActivity, "user.txt")
                val intent = Intent(this@MainActivity, WelcomeActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                stopService(Intent(this, MyBackgroundService::class.java))
                finish()

                return true
            }

            R.id.action_export_key -> {
                val popupView: View = layoutInflater.inflate(R.layout.popup_password, null)
                val popupWindow = PopupWindow(
                    popupView,
                    Toolbar.LayoutParams.WRAP_CONTENT,
                    Toolbar.LayoutParams.WRAP_CONTENT
                )
                popupWindow.isFocusable = true
                val editTextPassword: EditText = popupView.findViewById(R.id.editTextPassword)
                val btnCancel: Button = popupView.findViewById(R.id.btnCancel)
                val btnAccept: Button = popupView.findViewById(R.id.btnAccept)

                btnAccept.setOnClickListener {
                    if (editTextPassword.text.toString().isEmpty()) {
                        Toast.makeText(
                            this@MainActivity,
                            "Password cannot be empty",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                    checkCredential(
                        currentLoggedUser.username,
                        editTextPassword.text.toString(),
                        popupWindow
                    )
                }

                btnCancel.setOnClickListener {
                    popupWindow.dismiss()
                }
                popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)
                return true
            }

            R.id.action_import_key -> {
                val popupView: View =
                    layoutInflater.inflate(R.layout.popup_import_private_key, null)
                val popupWindow = PopupWindow(
                    popupView,
                    Toolbar.LayoutParams.WRAP_CONTENT,
                    Toolbar.LayoutParams.WRAP_CONTENT
                )
                popupWindow.isFocusable = true

                val textViewWarning: TextView = popupView.findViewById(R.id.textViewWelcome)
                val textViewInfo: TextView = popupView.findViewById(R.id.textViewInfo)
                val btnCancel: Button = popupView.findViewById(R.id.btnLogout)
                val btnImport: Button = popupView.findViewById(R.id.btnImportKey)
                btnCancel.text = "Cancel"
                textViewWarning.text = "Import private key"
                textViewInfo.text =
                    "If you import wrong private key, you will not be able to decrypt messages. We recommend to export your private key before importing new one."

                popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)

                btnCancel.setOnClickListener {
                    popupWindow.dismiss()
                }

                btnImport.setOnClickListener {
                    keySaver = KeySaver(this@MainActivity, currentLoggedUser.username)
                    keySaver.loadKey("For now its not used")
                    popupWindow.dismiss()
                }
                return true
            }
        }
        return false
    }

    internal class ViewPagerAdapter(fragmentManager: FragmentManager) :
        FragmentPagerAdapter(fragmentManager) {

        private val fragments: ArrayList<Fragment>
        private val titles: ArrayList<String>

        init {
            fragments = ArrayList<Fragment>()
            titles = ArrayList<String>()
        }

        override fun getItem(position: Int): Fragment {
            return fragments[position]
        }

        override fun getCount(): Int {
            return fragments.size
        }

        fun addFragment(fragment: Fragment, title: String) {
            fragments.add(fragment)
            titles.add(title)
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return titles[position]
        }
    }

    fun getCurrentUser(): UserJson {
        return this.currentLoggedUser
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_SAVE && resultCode == RESULT_OK) {
            val uri: Uri = data!!.data!!
            keySaver.saveKeyToFile(uri)
            Toast.makeText(
                this@MainActivity,
                "Private key saved successfully",
                Toast.LENGTH_SHORT
            ).show()
        } else if (requestCode == KeySaver.REQUEST_CODE_LOAD && resultCode == RESULT_OK) {
            val uri: Uri = data!!.data!!
            val loadKeyFromFile = keySaver.loadKeyFromFile(uri)

            LocalStorageAccess.deleteFileFromInternalStorage(this@MainActivity, currentLoggedUser.username + "_private_key.txt")
            ChatContext.setPrivateKey(loadKeyFromFile)
            LocalStorageAccess.savePrivateKeyToInternalStorage(this@MainActivity, currentLoggedUser.username + "_private_key.txt", ChatContext.getPrivateKey())
            ChatContext.setIsWrongPrivateKey(false)
            Toast.makeText(
                this@MainActivity,
                "Private key has been imported.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun checkCredential(username: String, password: String, popupWindow: PopupWindow) {
        val retrofitBuilder = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(ChatContext.getServerAuthorizationURL())
            .build()
            .create(LoginApiInterface::class.java)

        val retrofitData = retrofitBuilder.login(username, AuthUtils.encryptPassword(password))

        retrofitData.enqueue(object : Callback<UserJson?> {
            override fun onResponse(call: Call<UserJson?>, response: Response<UserJson?>) {
                if (response.code() == 200) {
                    keySaver = KeySaver(
                        this@MainActivity,
                        ChatContext.getPrivateKey(),
                        currentLoggedUser.username
                    )
                    /*keySaver = KeySaver.getInstance()
                    keySaver.setActivity(this@MainActivity)
                    keySaver.setPrivateKey(ChatContext.getPrivateKey())
                    keySaver.setUsername(currentLoggedUser.username)*/
                    keySaver.saveKey(AuthUtils.encryptPassword(password))
                    popupWindow.dismiss()
                } else if (response.code() == 401) {
                    runOnUiThread {
                        Toast.makeText(
                            applicationContext,
                            "Wrong password.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            applicationContext,
                            "An error occurred on authorization process. Try again later.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            override fun onFailure(call: Call<UserJson?>, t: Throwable) {
                println(t.message)
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        "Cannot connect to the server. Check your network connection.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        })
    }
}