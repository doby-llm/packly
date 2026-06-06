package com.dobyllm.packly.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dobyllm.packly.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.ui.token.PacklyRadius

@Composable
fun QuantityBadge(quantity: Int, modifier: Modifier = Modifier) {
    val quantityDescription = stringResource(R.string.a11y_quantity, quantity)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(PacklyRadius.full))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .semantics { contentDescription = quantityDescription }
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = stringResource(R.string.quantity_times, quantity),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
