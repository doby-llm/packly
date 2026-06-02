package com.dobyllm.packly.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PacklyProgress(packed: Int, total: Int, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val safeTotal = total.coerceAtLeast(1)
        Text("$packed of $total packed", style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(progress = { packed / safeTotal.toFloat() }, modifier = Modifier.fillMaxWidth())
    }
}
