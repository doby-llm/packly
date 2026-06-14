package com.dobyllm.packly.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.R
import com.dobyllm.packly.ui.token.PacklyRadius
import com.dobyllm.packly.ui.token.PacklySpacing

@Composable
fun PacklySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
) {
    val shape = RoundedCornerShape(PacklyRadius.full)
    val resolvedPlaceholder = placeholder ?: stringResource(R.string.search_items_placeholder)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.surfaceContainerLow, shape)
            .semantics { contentDescription = resolvedPlaceholder },
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = PacklySpacing.marginMobile),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(resolvedPlaceholder, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    innerTextField()
                }
            }
        },
    )
}

@Composable
fun PacklyFilterButton(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
) {
    val enabled = onClick != null
    val filterDescription = stringResource(R.string.a11y_filter_items)
    val filtersUnavailableDescription = stringResource(R.string.a11y_filters_unavailable)
    val containerColor = if (isActive) MaterialTheme.colorScheme.primaryFixed else MaterialTheme.colorScheme.surfaceContainer
    val contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryFixed else MaterialTheme.colorScheme.onSurface
    IconButton(
        onClick = { onClick?.invoke() },
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = modifier
            .size(48.dp)
            .semantics {
                contentDescription = if (enabled) filterDescription else filtersUnavailableDescription
                if (!enabled) disabled()
            },
    ) {
        Icon(Icons.Rounded.FilterList, contentDescription = null)
    }
}

@Composable
fun PacklySearchFilterRow(
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    hasActiveFilters: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
    ) {
        PacklySearchBar(query = query, onQueryChange = onQueryChange, modifier = Modifier.weight(1f))
        PacklyFilterButton(onClick = onFilterClick, isActive = hasActiveFilters)
    }
}
