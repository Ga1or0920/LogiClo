package com.example.myapplication.ui.feedback

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import com.example.myapplication.ui.providers.LocalAppContainer
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

/**
 * シンプルな保留フィードバック画面（UI実装）
 * - 実運用ではViewModelを介して対象アイテム・しきい値を取得・更新します。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingFeedbackRoute(
    navController: NavHostController,
    itemId: String? = null,
    itemName: String = "アイテム",
    currentUpperLimit: Int = 22,
    suggestedUpperLimit: Int = 20,
    onUpdateThreshold: (String, Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    
    var topSelection by remember { mutableStateOf<com.example.myapplication.domain.model.WearFeedbackRating?>(null) }
    var bottomSelection by remember { mutableStateOf<com.example.myapplication.domain.model.WearFeedbackRating?>(null) }
    
    val appContainer = LocalAppContainer.current
    val scope = rememberCoroutineScope()

    var displayName by remember { mutableStateOf(itemName) }
    var currentMax by remember { mutableStateOf(currentUpperLimit) }
    var currentMin by remember { mutableStateOf(currentUpperLimit - 4) }
    val suggestedHotLimit = remember(currentMax) { (currentMax - 2).coerceAtLeast(10) }
    val suggestedColdLimit = remember(currentMin) { (currentMin + 2).coerceAtMost(40) }

    LaunchedEffect(itemId) {
        itemId?.let { id ->
            try {
                val repo = appContainer.closetRepository
                val item = repo.getItem(id)
                if (item != null) {
                    displayName = item.name ?: displayName
                    item.comfortMaxCelsius?.let { currentMax = it.toInt() }
                    item.comfortMinCelsius?.let { currentMin = it.toInt() }
                }
            } catch (e: Exception) {
                // ignore - keep defaults
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "昨日の服はどうでしたか？", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = displayName, style = MaterialTheme.typography.titleMedium)
                Text(text = "適正気温帯: ${currentMin}℃〜${currentMax}℃", style = MaterialTheme.typography.bodySmall)
            }
        }

        

        // Per-part rating controls
        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                displayName.let { name ->
                    Text(text = name, style = MaterialTheme.typography.titleMedium)
                }
                // Top
                Text(text = "トップ", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val topWarmSelected = topSelection == com.example.myapplication.domain.model.WearFeedbackRating.TOO_WARM
                    val topJustSelected = topSelection == com.example.myapplication.domain.model.WearFeedbackRating.JUST_RIGHT
                    val topColdSelected = topSelection == com.example.myapplication.domain.model.WearFeedbackRating.TOO_COLD

                    Button(
                        onClick = { topSelection = com.example.myapplication.domain.model.WearFeedbackRating.TOO_WARM },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (topWarmSelected) MaterialTheme.colorScheme.primary else Color.White,
                            contentColor = if (topWarmSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    ) { Text(text = "暑い 🥵") }

                    Button(
                        onClick = { topSelection = com.example.myapplication.domain.model.WearFeedbackRating.JUST_RIGHT },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (topJustSelected) MaterialTheme.colorScheme.primary else Color.White,
                            contentColor = if (topJustSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    ) { Text(text = "普通 🙂") }

                    Button(
                        onClick = { topSelection = com.example.myapplication.domain.model.WearFeedbackRating.TOO_COLD },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (topColdSelected) MaterialTheme.colorScheme.primary else Color.White,
                            contentColor = if (topColdSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    ) { Text(text = "寒い 🥶") }
                }
                // Bottom
                Text(text = "ボトムス", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val bottomWarmSelected = bottomSelection == com.example.myapplication.domain.model.WearFeedbackRating.TOO_WARM
                    val bottomJustSelected = bottomSelection == com.example.myapplication.domain.model.WearFeedbackRating.JUST_RIGHT
                    val bottomColdSelected = bottomSelection == com.example.myapplication.domain.model.WearFeedbackRating.TOO_COLD

                    Button(
                        onClick = { bottomSelection = com.example.myapplication.domain.model.WearFeedbackRating.TOO_WARM },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (bottomWarmSelected) MaterialTheme.colorScheme.primary else Color.White,
                            contentColor = if (bottomWarmSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    ) { Text(text = "暑い 🥵") }

                    Button(
                        onClick = { bottomSelection = com.example.myapplication.domain.model.WearFeedbackRating.JUST_RIGHT },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (bottomJustSelected) MaterialTheme.colorScheme.primary else Color.White,
                            contentColor = if (bottomJustSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    ) { Text(text = "普通 🙂") }

                    Button(
                        onClick = { bottomSelection = com.example.myapplication.domain.model.WearFeedbackRating.TOO_COLD },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (bottomColdSelected) MaterialTheme.colorScheme.primary else Color.White,
                            contentColor = if (bottomColdSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    ) { Text(text = "寒い 🥶") }
                }

                // notes removed per UX request
            }
        }

        // Submit button
        Button(onClick = {
            // submit per-part ratings
            val repo = appContainer.wearFeedbackRepository
            scope.launch {
                try {
                    itemId?.let { id ->
                        repo.submitFeedback(id, topSelection, bottomSelection, null)
                    }
                    Toast.makeText(context, "フィードバックを送信しました", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "送信に失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                navController.popBackStack()
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text(text = "送信する")
        }
    }

    
}
