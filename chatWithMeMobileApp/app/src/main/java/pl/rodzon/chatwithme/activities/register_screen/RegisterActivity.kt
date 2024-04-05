package pl.rodzon.chatwithme.activities.register_screen

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
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


class RegisterActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val toolbar: Toolbar = findViewById(R.id.toolbar_register)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = "Register"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener() {
            val intent = Intent(this@RegisterActivity, WelcomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        val registerButton: Button = findViewById(R.id.register_btn)

        registerButton.setOnClickListener() {
            this.registerUser()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun registerUser() {
        val username: String = findViewById<EditText>(R.id.username_register).text.toString()
        val password: String = findViewById<EditText>(R.id.password_register).text.toString()
        val confirmPassword: String = findViewById<EditText>(R.id.confirm_password_register).text.toString()

        if (username == "") {
            Toast.makeText(this@RegisterActivity, "Please write username.", Toast.LENGTH_SHORT).show()
        } else if (password == "") {
            Toast.makeText(this@RegisterActivity, "Please write password.", Toast.LENGTH_SHORT).show()
        } else if (confirmPassword == "") {
            Toast.makeText(this@RegisterActivity, "Please write confirm password.", Toast.LENGTH_SHORT).show()
        } else if (password != confirmPassword){
            Toast.makeText(this@RegisterActivity, "Password not match to confirm password.", Toast.LENGTH_SHORT).show()
        } else if (!NetworkCheck.isNetworkAvailable(this@RegisterActivity)) {
            Toast.makeText(this@RegisterActivity, "You are offline.", Toast.LENGTH_SHORT).show()
        } else if (LocalStorageAccess.doesFileExistInInternalStorage(this@RegisterActivity, username + "_private_key.txt")) {
            Toast.makeText(this@RegisterActivity, "This username already exists.", Toast.LENGTH_SHORT).show()
        } else {
            SSHUtils.generatePrivateAndPublicKey()
            LocalStorageAccess.savePrivateKeyToInternalStorage(this@RegisterActivity, username + "_private_key.txt", ChatContext.getPrivateKey())
            ChatContext.setPrivateKey("")
            this.registerUserToServer(username, AuthUtils.encryptPassword(password), ChatContext.getPublicKey())
        }
    }

    private fun registerUserToServer(username: String, password: String, publicKey: String) {
        val retrofitBuilder = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(ChatContext.getServerAuthorizationURL())
            .build()
            .create(RegisterApiInterface::class.java)

        val retrofitData = retrofitBuilder.register(username, password, publicKey)

        retrofitData.enqueue(object : Callback<UserJson?> {
            override fun onResponse(call: Call<UserJson?>, response: Response<UserJson?>) {
                if (response.code() == 201) {
                    val responseBody = response.body()
                    val userJson = UserJson(
                        responseBody!!.userID,
                        responseBody.username,
                        responseBody.publicKey,
                        responseBody.picture
                    )
                    ChatContext.setPrivateKey(LocalStorageAccess.loadPrivateKeyFromInternalStorage(this@RegisterActivity, username + "_private_key.txt"))
                    LocalStorageAccess.saveLoggedUserInformationToLocalStorage(this@RegisterActivity, "user.txt", userJson)
                    startMyService()
                    val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.putExtra("user", userJson as java.io.Serializable)
                    startActivity(intent)
                    finish()
                    runOnUiThread {
                        Toast.makeText(
                            applicationContext,
                            "Register successfully.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else if (response.code() == 409) {
                    println(response.message())
                    LocalStorageAccess.deleteFileFromInternalStorage(this@RegisterActivity, username + "_private_key.txt")
                    runOnUiThread {
                        Toast.makeText(
                            applicationContext,
                            "This username already exists.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    LocalStorageAccess.deleteFileFromInternalStorage(this@RegisterActivity, username + "_private_key.txt")
                    runOnUiThread {
                        Toast.makeText(
                            applicationContext,
                            "An error occurred on registration process. Try again later.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            override fun onFailure(call: Call<UserJson?>, t: Throwable) {
                LocalStorageAccess.deleteFileFromInternalStorage(this@RegisterActivity, username + "_private_key.txt")
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
}