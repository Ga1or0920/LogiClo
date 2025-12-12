package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.myapplication.data.AppContainer
import com.example.myapplication.ui.LaundryLoopApp
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var appContainer: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        auth = Firebase.auth
        appContainer = (application as LaundryLoopApplication).container

        val intent = intent
        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                LaundryLoopApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null || intent.data == null) {
            return
        }
        val link = intent.data.toString()
        if (auth.isSignInWithEmailLink(link)) {
            CoroutineScope(Dispatchers.Main).launch {
                val email = appContainer.userPreferencesRepository.getEmailForSignIn()
                if (email == null) {
                    Toast.makeText(this@MainActivity, "メールアドレスが見つかりません。", Toast.LENGTH_SHORT).show()
                } else {
                    auth.signInWithEmailLink(email, link)
                        .addOnCompleteListener(this@MainActivity) { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this@MainActivity, "ログインしました。", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@MainActivity, "ログインに失敗しました。", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
        }
    }

    companion object {
        const val EXTRA_TARGET_DESTINATION: String = "target_destination"
    }
}
