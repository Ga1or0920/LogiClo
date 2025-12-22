package com.example.myapplication.ui.logiclo.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.logiclo.UiClothingItem
import com.example.myapplication.ui.theme.TextGrey

@Composable
fun SlidingToggle(
    labels: List<String>,
    selectedIndex: Int,
    onChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<ImageVector>? = null
) {
    val bias by animateFloatAsState(targetValue = if (selectedIndex == 0) -1f else 1f)

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        // Thumb
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .align(if (bias == -1f) Alignment.CenterStart else Alignment.CenterEnd)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            labels.forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable { onChanged(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if(icons != null){
                            Icon(
                                imageVector = icons[index],
                                contentDescription = label,
                                modifier = Modifier.size(16.dp),
                                tint = if(selectedIndex == index) MaterialTheme.colorScheme.onSurface else TextGrey
                            )
                        }
                        Text(
                            text = label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selectedIndex == index) MaterialTheme.colorScheme.onSurface else TextGrey
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
    options: List<Pair<String, String>>,
    onSelected: (id: String, label: String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(
            onClick = { expanded = true },
            shape = CircleShape,
            colors = ButtonDefaults.textButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (id, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelected(id, text)
                        expanded = false
                    },
                    leadingIcon = {
                        val icon = if (id == selectedId) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked
                        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
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
    icons: List<ImageVector>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        icons.forEachIndexed { index, icon ->
            val isSelected = selectedIndex == index
            val backgroundColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
            val iconColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onSurface else TextGrey)

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(backgroundColor)
                    .clickable { onChanged(index) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = iconColor
                )
            }
        }
    }
}

@Composable
fun OutfitCardItem(
    item: UiClothingItem,
    label: String,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = item.icon),
                contentDescription = item.name,
                tint = item.color,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = TextGrey, fontWeight = FontWeight.Bold)
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Tag(text = "残り${item.maxWears - item.currentWears}回")
                    if (item.maxWears == 1) {
                        Tag(text = "毎回洗う", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextGrey)
            }
        }
    }
}

@Composable
fun Tag(text: String, color: Color = TextGrey) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    )
}

@Composable
fun FilterChip(
    label: String,
    isSelected: Boolean,
    onTap: () -> Unit
) {
    val backgroundColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant)
    val textColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant)

    Button(
        onClick = onTap,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor, contentColor = textColor),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
