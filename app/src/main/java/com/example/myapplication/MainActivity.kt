package com.example.myapplication

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.myapplication.ui.LaundryLoopApp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private var navController: NavHostController? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        val targetDestination = intent.getStringExtra(EXTRA_TARGET_DESTINATION)
        setContent {
            MyApplicationTheme {
                        val controller = rememberNavController()
                        navController = controller
                        LaundryLoopApp(navController = controller, startDestination = targetDestination)
            }
        }
    }
    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        val targetDestination = intent?.getStringExtra(EXTRA_TARGET_DESTINATION)
        if (targetDestination != null && navController != null) {
            navController?.navigate(targetDestination) {
                popUpTo(navController!!.graph.startDestinationId) { inclusive = false }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), 1001)
            }
        }
    }

    companion object {
        const val EXTRA_TARGET_DESTINATION: String = "target_destination"
    }
}