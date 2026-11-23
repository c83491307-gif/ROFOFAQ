package com.creazzy.rofof.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.creazzy.rofof.feature.dashboard.DashboardScreen
import com.creazzy.rofof.feature.purchases.PurchaseManagementScreen

@Composable
fun ROFOFRootScreen() {
    var destination by remember { mutableStateOf("dashboard") }
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = { destination = "dashboard" }) {
                Text("الرئيسية · Home", color = if (destination == "dashboard") Color(0xFF147D64) else MaterialTheme.colorScheme.onSurface)
            }
            TextButton(onClick = { destination = "purchases" }) {
                Text("المشتريات المشتركة · Shared", color = if (destination == "purchases") Color(0xFF147D64) else MaterialTheme.colorScheme.onSurface)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (destination == "dashboard") DashboardScreen()
            else PurchaseManagementScreen(onBack = { destination = "dashboard" })
        }
    }
}
