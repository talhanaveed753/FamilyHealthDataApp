package com.example.healthconnect_tablet.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthconnect_tablet.data.repository.FamilyManager

/**
 * Screen for selecting a family and entering password
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilySelectionScreen(
    onFamilySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFamily by remember { mutableStateOf("family1") }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    val familyManager = FamilyManager(androidx.compose.ui.platform.LocalContext.current)
    val families = familyManager.getAvailableFamilies()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Header
        Text(
            text = "Welcome to HealthConnect",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "Select your family to access health data",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 48.dp)
        )
        
        // Family Selection
        Text(
            text = "Select Family",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        var expanded by remember { mutableStateOf(false) }
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            OutlinedTextField(
                value = selectedFamily,
                onValueChange = { },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                families.forEach { family ->
                    DropdownMenuItem(
                        text = { Text(family) },
                        onClick = { 
                            selectedFamily = family
                            expanded = false
                        }
                    )
                }
            }
        }
        
        // Password Input
        Text(
            text = "Family Password",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        OutlinedTextField(
            value = password,
            onValueChange = { 
                password = it
                showError = false
            },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            isError = showError
        )
        
        // Error Message
        if (showError) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        // Join Family Button
        Button(
            onClick = {
                if (familyManager.validateFamilyPassword(selectedFamily, password)) {
                    onFamilySelected(selectedFamily)
                } else {
                    showError = true
                    errorMessage = "Incorrect password for $selectedFamily"
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = password.isNotBlank()
        ) {
            Text(
                text = "Join Family",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Info Text
        Text(
            text = "This tablet will access health data for the selected family",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 32.dp)
        )
    }
} 