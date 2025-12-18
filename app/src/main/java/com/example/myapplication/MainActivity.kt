package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.myapplication.ui.logiclo.LogiCloApp
import com.example.myapplication.ui.logiclo.LogiCloViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: LogiCloViewModel by viewModels {
        LogiCloViewModel.Companion.Factory(
            (application as LaundryLoopApplication).container.closetRepository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LogiCloApp(viewModel = viewModel)
        }
    }

    companion object {
        const val EXTRA_TARGET_DESTINATION = "target_destination"
        const val EXTRA_TARGET_ITEM_ID = "target_item_id"
    }
}
