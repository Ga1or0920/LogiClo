package com.example.myapplication.ui.port

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties

@Composable
fun FilterChipCustom(
    label: String,
    isSelected: Boolean,
    onTap: () -> Unit
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onTap,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
fun SlidingToggle(
    height: Double,
    labels: List<String>,
    icons: List<ImageVector>? = null,
    selectedIndex: Int,
    onChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = MaterialTheme.colorScheme.surfaceVariant
    val toggleColor = MaterialTheme.colorScheme.surface
    val selectedTextColor = MaterialTheme.colorScheme.onSurface
    val unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .height(height.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(containerColor)
            .padding(4.dp)
    ) {
        val targetBias = if (selectedIndex == 0) -1f else 1f
        val animatedBias by animateFloatAsState(
            targetValue = targetBias,
            animationSpec = tween(durationMillis = 200),
            label = "bias"
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .align(BiasAlignment(animatedBias, 0f))
                .shadow(2.dp, RoundedCornerShape(percent = 50))
                .background(toggleColor, RoundedCornerShape(percent = 50))
        )

        Row(modifier = Modifier.fillMaxSize()) {
            labels.forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(interactionSource = null, indication = null) { onChanged(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (icons != null) {
                            Icon(
                                imageVector = icons[index],
                                contentDescription = null,
                                tint = if (selectedIndex == index) selectedTextColor else unselectedTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = label,
                            color = if (selectedIndex == index) selectedTextColor else unselectedTextColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimeMenuButton(
    label: String,
    selectedId: String,
    options: List<Map<String, String>>,
    onSelected: (String, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.height(40.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = true)
        ) {
            options.forEach { opt ->
                val id = opt["id"] ?: ""
                val optLabel = opt["label"] ?: ""
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (id == selectedId) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(optLabel)
                        }
                    },
                    onClick = {
                        onSelected(id, optLabel)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun IconToggleGroup(
    selectedIndex: Int,
    onChanged: (Int) -> Unit,
    icons: List<ImageVector>
) {
    val activeBg = MaterialTheme.colorScheme.surface
    val activeIcon = MaterialTheme.colorScheme.onSurface
    val inactiveIcon = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.height(40.dp)
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            icons.forEachIndexed { index, icon ->
                val isSelected = selectedIndex == index
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) activeBg else Color.Transparent)
                        .clickable { onChanged(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) activeIcon else inactiveIcon,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (index < icons.size - 1) Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

@Composable
fun OutfitCardItem(
    item: ClothingItem,
    label: String,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(item.icon, contentDescription = null, tint = item.color, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = AppColors.TextGrey)
                )
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Tag(text = "残り${item.maxWears - item.currentWears}回")
                    if (item.maxWears == 1) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Tag(text = "毎回洗う", color = AppColors.AccentBlue)
                    }
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Gray)
            }
        }
    }
}

@Composable
fun Tag(
    text: String,
    color: Color = Color.Gray
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(color = color, fontWeight = FontWeight.Bold)
        )
    }
}
