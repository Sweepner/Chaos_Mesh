package pl.rodzon.chatwithme.activities.login_screen

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import pl.rodzon.chatwithme.R
import pl.rodzon.chatwithme.activities.main_screen.MainActivity
import pl.rodzon.chatwithme.activities.welcome_screen.WelcomeActivity
import pl.rodzon.chatwithme.model.users.UserJson
import pl.rodzon.chatwithme.utils.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginActivity : AppCompatActivity() {
    private lateinit var keySaver: KeySaver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val toolbar: Toolbar = findViewById(R.id.toolbar_login)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = "Login"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener() {
            val intent = Intent(this@LoginActivity, WelcomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        val loginButton: Button = findViewById(R.id.login_btn)

        loginButton.setOnClickListener() {
            loginUser()
        }
    }

    private fun loginUser() {
        val username: String = findViewById<EditText>(R.id.username_login).text.toString()
        val password: String = findViewById<EditText>(R.id.password_login).text.toString()

        if (username == "") {
            Toast.makeText(this@LoginActivity, "Please write username.", Toast.LENGTH_SHORT).show()
        } else if (password == "") {
            Toast.makeText(this@LoginActivity, "Please write password.", Toast.LENGTH_SHORT).show()
        } else if (!NetworkCheck.isNetworkAvailable(this@LoginActivity)) {
            Toast.makeText(this@LoginActivity, "You are offline.", Toast.LENGTH_SHORT).show()
        } else {
            this.login(username, password)
        }
    }

    private fun login(username: String, password: String) {
        val retrofitBuilder = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(ChatContext.getServerAuthorizationURL())
            .build()
            .create(LoginApiInterface::class.java)

        val retrofitData = retrofitBuilder.login(username, AuthUtils.encryptPassword(password))

        retrofitData.enqueue(object : Callback<UserJson?> {
            override fun onResponse(call: Call<UserJson?>, response: Response<UserJson?>) {
                if (response.code() == 200) {
                    if (LocalStorageAccess.doesFileExistInInternalStorage(this@LoginActivity, username + "_private_key.txt")) {
                        val responseBody = response.body()
                        ChatContext.setPrivateKey(LocalStorageAccess.loadPrivateKeyFromInternalStorage(this@LoginActivity, username + "_private_key.txt"))

                        val userJson = UserJson(
                            responseBody!!.userID,
                            responseBody.username,
                            responseBody.publicKey,
                            responseBody.picture
                        )
                        LocalStorageAccess.saveLoggedUserInformationToLocalStorage(this@LoginActivity, "user.txt", userJson)
                        startMyService()
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.putExtra("user", userJson as java.io.Serializable)
                        startActivity(intent)
                        finish()
                    } else {
                        val popupView: View = layoutInflater.inflate(R.layout.popup_import_private_key, null)
                        val popupWindow = PopupWindow(
                            popupView,
                            Toolbar.LayoutParams.WRAP_CONTENT,
                            Toolbar.LayoutParams.WRAP_CONTENT
                        )
                        popupWindow.isFocusable = true

                        val btnLogout: Button = popupView.findViewById(R.id.btnLogout)
                        val btnImport: Button = popupView.findViewById(R.id.btnImportKey)

                        btnLogout.setOnClickListener {
                            popupWindow.dismiss()
                            val intent = Intent(this@LoginActivity, WelcomeActivity::class.java)
                            startActivity(intent)
                            finish()
                        }

                        btnImport.setOnClickListener {
                            ChatContext.setTempUsername(username)
                            ChatContext.setTempPassword(password)
                            keySaver = KeySaver(this@LoginActivity, username)
                            /*keySaver = KeySaver.getInstance()
                            keySaver.setActivity(this@LoginActivity)
                            keySaver.setUsername(username)*/
                            keySaver.loadKey(AuthUtils.encryptPassword(password))
                        }

                        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)
                    }
                } else if (response.code() == 401) {
                    runOnUiThread {
                        Toast.makeText(
                            applicationContext,
                            "Wrong credential.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                else {
                    runOnUiThread {
                        Toast.makeText(
                            applicationContext,
                            "An error occurred on login process. Try again later.",
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

    private fun startMyService() {
        val serviceIntent = Intent(this, MyBackgroundService::class.java)
        startForegroundService(serviceIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == KeySaver.REQUEST_CODE_LOAD && resultCode == RESULT_OK) {
            val uri: Uri = data!!.data!!
            val loadKeyFromFile = keySaver.loadKeyFromFile(uri)

            ChatContext.setPrivateKey(loadKeyFromFile)
            LocalStorageAccess.savePrivateKeyToInternalStorage(this@LoginActivity, ChatContext.getTempUsername() + "_private_key.txt", ChatContext.getPrivateKey())
            Toast.makeText(
                this@LoginActivity,
                "Private key has been imported.",
                Toast.LENGTH_SHORT
            ).show()

            login(ChatContext.getTempUsername()!!, ChatContext.getTempPassword()!!)
            ChatContext.setTempUsername(null)
            ChatContext.setTempPassword(null)
        }
    }
}
