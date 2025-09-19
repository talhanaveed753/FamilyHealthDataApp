package com.example.healthconnect_tablet.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.healthconnect_tablet.data.model.UserHealthSummary
import java.text.SimpleDateFormat
import java.util.*

import com.example.healthconnect_tablet.data.repository.CardColorManager

// Use colors from CardColorManager for consistency
private val SoftCardColors = CardColorManager.AVAILABLE_COLORS

/**
 * Card component displaying a family member's health data
 * Optimized for tablet display with charts and key metrics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyMemberCard(
    userHealthSummary: UserHealthSummary,
    onCardClick: (() -> Unit)? = null,
    onColorChange: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val user = userHealthSummary.user
    val todayData = userHealthSummary.todayData
    var showColorPicker by remember { mutableStateOf(false) }
    
    // Parse the user's color or use default
    val cardColor = try {
        Color(android.graphics.Color.parseColor(user.cardColor))
    } catch (e: Exception) {
        Color(android.graphics.Color.parseColor(CardColorManager.DEFAULT_COLOR))
    }
    
    Card(
        onClick = { onCardClick?.invoke() },
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp,
            hoveredElevation = 3.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = cardColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile section
                ProfileSection(
                    name = user.name,
                    profileImageUrl = user.profileImageUrl,
                    lastActive = user.lastActive
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Health metrics section
                if (todayData != null) {
                    HealthMetricsSection(
                        steps = todayData.steps,
                        heartRate = todayData.heartRate,
                        sleepHours = todayData.sleepHours,
                        caloriesBurned = todayData.caloriesBurned,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    NoDataSection(modifier = Modifier.weight(1f))
                }
            }
            
            // Color picker icon in top right corner
            IconButton(
                onClick = { showColorPicker = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Change card color",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
    
    // Color picker dialog
    if (showColorPicker) {
        ColorPickerDialog(
            currentColor = user.cardColor,
            onColorSelected = { selectedColor ->
                onColorChange?.invoke(selectedColor)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }
}

@Composable
private fun ColorPickerDialog(
    currentColor: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Choose Card Color",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFF2E2E2E),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SoftCardColors.forEach { colorHex ->
                    val color = Color(android.graphics.Color.parseColor(colorHex))
                    val isSelected = colorHex == currentColor
                    
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { onColorSelected(colorHex) }
                            .then(
                                if (isSelected) Modifier.background(
                                    Color.White.copy(alpha = 0.8f),
                                    CircleShape
                                ) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        modifier = Modifier.widthIn(max = 500.dp)
    )
}

@Composable
private fun ProfileSection(
    name: String,
    profileImageUrl: String,
    lastActive: Date?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile image
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(30.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HealthMetricsSection(
    steps: Int,
    heartRate: Int,
    sleepHours: Float,
    caloriesBurned: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Heart Rate Section
        MetricSection(
            icon = Icons.Default.Favorite,
            value = if (heartRate > 0) "$heartRate" else "--",
            unit = "BPM",
            color = Color(0xFFF44336) // Health red for heart rate
        ) {
            // Heart rate additional info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(Color(0xFFF44336).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            )
            Text(
                text = "Active: 20m ago",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp
        )

        // Sleep Section
        MetricSection(
            icon = Icons.Default.Bedtime,
            value = if (sleepHours > 0) String.format("%.1f", sleepHours) else "--",
            unit = "hours",
            color = Color(0xFF673AB7) // Health purple for sleep
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(8) { index ->
                    val alpha = if (sleepHours >= (index + 1)) 1f else 0.3f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(20.dp)
                            .background(Color(0xFF673AB7).copy(alpha = alpha * 0.2f), RoundedCornerShape(4.dp))
                    )
                }
            }
            if (sleepHours > 0) {
                Text(
                    text = "10:30 PM - 6:00 AM",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp
        )

        // Steps Section
        MetricSection(
            icon = Icons.Default.DirectionsWalk,
            value = if (steps > 0) String.format("%,d", steps) else "--",
            unit = "steps",
            color = Color(0xFF4CAF50) // Health green for steps
        ) {
            val progress = (steps.toFloat() / 10000f).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = Color(0xFF4CAF50), // Health green
                trackColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${String.format("%.1f", steps / 1000f)}km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${caloriesBurned}cal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetricSection(
    icon: ImageVector,
    value: String,
    unit: String,
    color: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        content()
    }
}

@Composable
private fun MetricItem(
    icon: ImageVector,
    value: String,
    label: String,
    subLabel: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 36.dp)  // Aligns with the value text
        )
        Text(
            text = subLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 36.dp)  // Aligns with the value text
        )
    }
}

@Composable
private fun MetricRow(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    isLarge: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(if (isLarge) 24.dp else 20.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column {
            Text(
                text = value,
                style = if (isLarge) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SmallMetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun NoDataSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhoneAndroid,
            contentDescription = "No data",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No data for today",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Please sync data using\nthe FamilySync app on your phone",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun WeeklyTrendChart(
    weeklyData: List<Float>,
    modifier: Modifier = Modifier
) {
    if (weeklyData.isEmpty()) return
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "7-day trend",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            SimpleLineChart(
                data = weeklyData,
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SimpleLineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    if (data.isEmpty()) return
    
    Canvas(modifier = modifier) {
        val maxValue = data.maxOrNull() ?: 1f
        val minValue = data.minOrNull() ?: 0f
        val range = maxValue - minValue
        
        if (range == 0f) return@Canvas
        
        val stepX = size.width / (data.size - 1).coerceAtLeast(1)
        val path = Path()
        
        data.forEachIndexed { index, value ->
            val x = index * stepX
            val y = size.height - ((value - minValue) / range) * size.height
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
            
            // Draw points
            drawCircle(
                color = color,
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }
        
        // Draw line
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
    }
} 