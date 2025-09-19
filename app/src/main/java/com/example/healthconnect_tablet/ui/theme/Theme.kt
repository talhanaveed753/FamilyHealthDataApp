package com.example.healthconnect_tablet.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val HealthDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4DD0E1),           // Medical teal light
    secondary = Color(0xFF81C784),         // Health green light
    tertiary = Color(0xFFFF8A65),          // Energy orange light
    background = Color(0xFF0F1419),        // Dark medical background
    surface = Color(0xFF1A1F24),           // Dark surface
    surfaceVariant = Color(0xFF2C3239),    // Dark surface variant
    onPrimary = Color(0xFF003D40),         // Dark teal for contrast
    onSecondary = Color(0xFF1B5E20),       // Dark green for contrast
    onTertiary = Color(0xFFBF360C),        // Dark orange for contrast
    onBackground = Color(0xFFE8F4FD),      // Light blue-white
    onSurface = Color(0xFFE8F4FD),         // Light blue-white
    onSurfaceVariant = Color(0xFFB0BEC5),  // Medium gray
    outline = Color(0xFF546E7A),           // Border color
    outlineVariant = Color(0xFF37474F)     // Subtle border
)

private val HealthLightColorScheme = lightColorScheme(
    primary = Color(0xFF00BCD4),           // Medical teal
    secondary = Color(0xFF4CAF50),         // Health green
    tertiary = Color(0xFFFF7043),          // Energy orange
    background = Color(0xFFF8F9FA),        // Clean white background
    surface = Color(0xFFFFFFFF),           // Pure white surface
    surfaceVariant = Color(0xFFF1F3F4),    // Light gray surface
    onPrimary = Color.White,               // White on teal
    onSecondary = Color.White,             // White on green
    onTertiary = Color.White,              // White on orange
    onBackground = Color(0xFF1A1C1E),      // Dark text
    onSurface = Color(0xFF1A1C1E),         // Dark text
    onSurfaceVariant = Color(0xFF44474E),  // Medium gray text
    outline = Color(0xFFCAC4D0),           // Border color
    outlineVariant = Color(0xFFE7E0EC),    // Subtle border
    // Health-specific surfaces
    primaryContainer = Color(0xFFE0F2F1), // Light teal container
    secondaryContainer = Color(0xFFE8F5E8), // Light green container
    tertiaryContainer = Color(0xFFFFE0B2), // Light orange container
    onPrimaryContainer = Color(0xFF00695C), // Dark teal text
    onSecondaryContainer = Color(0xFF2E7D32), // Dark green text
    onTertiaryContainer = Color(0xFFE65100)  // Dark orange text
)

@Composable
fun HealthConnect_TabletTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> HealthDarkColorScheme
        else -> HealthLightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use transparent status bar for edge-to-edge design
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            // Adjust status bar icons based on theme
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
} 