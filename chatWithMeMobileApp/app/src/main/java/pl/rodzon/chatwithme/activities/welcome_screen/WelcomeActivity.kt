package pl.rodzon.chatwithme.activities.welcome_screen

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import org.json.JSONObject
import pl.rodzon.chatwithme.R
import pl.rodzon.chatwithme.activities.login_screen.LoginActivity
import pl.rodzon.chatwithme.activities.main_screen.MainActivity
import pl.rodzon.chatwithme.activities.register_screen.RegisterActivity
import pl.rodzon.chatwithme.model.users.UserJson
import pl.rodzon.chatwithme.utils.ChatContext
import pl.rodzon.chatwithme.utils.LocalStorageAccess

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val registerButton: Button = findViewById(R.id.register_welcome_btn)
        val loginButton: Button = findViewById(R.id.login_welcome_btn)

        registerButton.setOnClickListener() {
            val intent = Intent(this@WelcomeActivity, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }

        loginButton.setOnClickListener() {
            val intent = Intent(this@WelcomeActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun checkIfUserIsLogged() {
        if (LocalStorageAccess.doesFileExistInInternalStorage(this@WelcomeActivity, "user.txt")) {
            val userString = LocalStorageAccess.loadUserInformationFromInternalStorage(this@WelcomeActivity, "user.txt")
            val userJSONObject = JSONObject(userString)
            val userJson = UserJson(
                userJSONObject.getString("userID"),
                userJSONObject.getString("username"),
                userJSONObject.getString("publicKey"),
                userJSONObject.getString("picture")
            )
            ChatContext.setPrivateKey(LocalStorageAccess.loadPrivateKeyFromInternalStorage(this@WelcomeActivity, userJson.username + "_private_key.txt"))

            val intent = Intent(this@WelcomeActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("user", userJson as java.io.Serializable)
            startActivity(intent)
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        checkIfUserIsLogged()
    }
}