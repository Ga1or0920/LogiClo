package com.example.myapplication.ui.feedback

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
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
    itemId: String? = "cardigan-001",
    itemName: String = "カーディガン",
    currentUpperLimit: Int = 22,
    suggestedUpperLimit: Int = 20,
    onUpdateThreshold: (String, Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var selectedFeeling by remember { mutableStateOf<String?>(null) }
    var showConfirm by remember { mutableStateOf(false) }
    val appContainer = LocalAppContainer.current
    val scope = rememberCoroutineScope()

    var displayName by remember { mutableStateOf(itemName) }
    var currentLimit by remember { mutableStateOf(currentUpperLimit) }
    val suggestedLimit = remember(currentLimit) { (currentLimit - 2).coerceAtLeast(10) }

    LaunchedEffect(itemId) {
        itemId?.let { id ->
            try {
                val repo = appContainer.closetRepository
                val item = repo.getItem(id)
                if (item != null) {
                    displayName = item.name ?: displayName
                    item.comfortMaxCelsius?.let { currentLimit = it.toInt() }
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
                Text(text = "適正上限: ${currentLimit}℃", style = MaterialTheme.typography.bodySmall)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    selectedFeeling = "hot"
                    showConfirm = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "🥵 暑かった")
            }

            OutlinedButton(
                onClick = {
                    Toast.makeText(context, "ありがとうございます（記録されました）", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "🙂 ちょうど良かった")
            }

            OutlinedButton(
                onClick = {
                    Toast.makeText(context, "ありがとうございます（記録されました）", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "🥶 寒かった")
            }
        }
    }

    if (showConfirm && selectedFeeling == "hot") {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
                title = { Text(text = "${displayName}の設定を更新しますか？") },
            text = {
                Text(text = "${displayName}の適正上限を ${currentLimit}℃ → ${suggestedLimit}℃ に下げますか？")
            },
            confirmButton = {
                Button(onClick = {
                    showConfirm = false
                    itemId?.let { id ->
                        // リポジトリ経由でアイテムの comfortMaxCelsius を更新
                        scope.launch {
                            try {
                                val repo = appContainer.closetRepository
                                val item = repo.getItem(id)
                                if (item != null) {
                                    val updated = item.copy(comfortMaxCelsius = suggestedLimit.toDouble())
                                    repo.upsert(updated)
                                }
                                onUpdateThreshold(id, suggestedLimit)
                            } catch (e: Exception) {
                                // 更新失敗はトーストで通知
                                Toast.makeText(context, "更新に失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    Toast.makeText(context, "更新しました。次回から提案が変わります。", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }) {
                    Text(text = "更新する")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirm = false }) {
                    Text(text = "やめる")
                }
            }
        )
    }
}
