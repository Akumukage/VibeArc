package com.vibearc.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SettingsScreen(padding: PaddingValues) {
    val context = LocalContext.current
    var selectedIcon by remember { mutableStateOf(context.selectedLauncherIcon()) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    val colors = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { Text("Make VibeArc yours", style = MaterialTheme.typography.headlineMedium) }
        item { Text("Choose the icon shown on your home screen.", color = colors.onSurfaceVariant) }
        items(LauncherIconChoice.entries, key = LauncherIconChoice::name) { choice ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (choice == selectedIcon) colors.primaryContainer else colors.surfaceVariant,
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().clickable {
                    if (context.setLauncherIcon(choice)) {
                        selectedIcon = choice
                        resultMessage = "${choice.label} icon selected"
                    } else {
                        resultMessage = "Could not change the icon"
                    }
                },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(choice.label, fontWeight = FontWeight.Bold)
                        Text(if (choice == selectedIcon) "Selected" else "Tap to use", color = colors.onSurfaceVariant)
                    }
                    if (choice == selectedIcon) Text("✓", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
        resultMessage?.let { message -> item { Text(message, color = colors.primary) } }
        item {
            Text(
                "Some launchers take a moment to refresh the icon.",
                color = colors.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}
