package com.example.healthconnect_tablet.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import com.example.healthconnect_tablet.data.repository.HealthConnectRepository
import com.example.healthconnect_tablet.data.repository.FamilyManager
import com.example.healthconnect_tablet.ui.screen.FamilyDashboardScreen
import com.example.healthconnect_tablet.ui.screen.FamilySelectionScreen
import com.example.healthconnect_tablet.ui.screen.MemberDashboardScreen
import com.example.healthconnect_tablet.ui.theme.HealthConnect_TabletTheme
import kotlinx.coroutines.launch

/**
 * Main Activity for the Family Health Dashboard
 * Handles Health Connect permissions and navigation
 */
class MainActivity : ComponentActivity() {
    
    private lateinit var familyManager: FamilyManager
    
    // Health Connect permission launcher
    private val requestPermissionActivityContract = 
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Handle permission results
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                // Permissions granted, proceed with health data access
                setupHealthConnect()
            } else {
                // Some permissions denied, show explanation or alternative flow
                handlePermissionsDenied()
            }
        }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        familyManager = FamilyManager(this)
        
        // Check and request Health Connect permissions
        checkHealthConnectPermissions()
        
        setContent {
            HealthConnect_TabletTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent()
                }
            }
        }
    }
    
    @Composable
    private fun MainContent() {
        var hasSelectedFamily by remember { mutableStateOf(familyManager.hasSelectedFamily()) }
        var selectedMember by remember { mutableStateOf<Triple<String, String, String>?>(null) } // userId, userName, familyName
        
        when {
            selectedMember != null -> {
                val (userId, userName, familyName) = selectedMember!!
                MemberDashboardScreen(
                    userId = userId,
                    userName = userName,
                    familyName = familyName,
                    onBackPressed = {
                        selectedMember = null
                    }
                )
            }
            hasSelectedFamily -> {
                FamilyDashboardScreen(
                    onSwitchFamily = {
                        familyManager.clearSelectedFamily()
                        hasSelectedFamily = false
                    },
                    onMemberClick = { userId, userName ->
                        val familyName = familyManager.getSelectedFamily() ?: "Unknown"
                        selectedMember = Triple(userId, userName, familyName)
                    }
                )
            }
            else -> {
                FamilySelectionScreen(
                    onFamilySelected = { familyName ->
                        familyManager.setSelectedFamily(familyName)
                        hasSelectedFamily = true
                    }
                )
            }
        }
    }
    
    /**
     * Check if Health Connect permissions are granted
     */
    private fun checkHealthConnectPermissions() {
        lifecycleScope.launch {
            try {
                val healthConnectRepository = HealthConnectRepository(this@MainActivity)
                
                // Check if Health Connect is available
                if (!healthConnectRepository.isHealthConnectAvailable()) {
                    // Health Connect not available on this device
                    // Could show a dialog or redirect to Play Store
                    return@launch
                }
                
                // Check if permissions are already granted
                if (!healthConnectRepository.hasAllPermissions()) {
                    // Request permissions
                    requestHealthConnectPermissions()
                } else {
                    // Permissions already granted
                    setupHealthConnect()
                }
            } catch (e: Exception) {
                // Handle error checking permissions
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Request Health Connect permissions
     */
    private fun requestHealthConnectPermissions() {
        val permissions = HealthConnectRepository.HEALTH_PERMISSIONS.map { it.toString() }
        requestPermissionActivityContract.launch(permissions.toTypedArray())
    }
    
    /**
     * Setup Health Connect after permissions are granted
     */
    private fun setupHealthConnect() {
        // Health Connect is ready to use
        // The ViewModel will handle data synchronization
    }
    
    /**
     * Handle case when permissions are denied
     */
    private fun handlePermissionsDenied() {
        // Could show an explanation dialog or work with limited functionality
        // For now, we'll continue without Health Connect data
        // The app will still work with manually entered data from Firebase
    }
} 