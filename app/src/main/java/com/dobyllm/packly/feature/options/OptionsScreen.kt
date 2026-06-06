package com.dobyllm.packly.feature.options

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.R
import com.dobyllm.packly.core.model.LanguagePreference
import com.dobyllm.packly.ui.token.PacklyRadius
import com.dobyllm.packly.ui.token.PacklySpacing

@Composable
fun OptionsScreen(
    languagePreference: LanguagePreference,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onLanguagePreferenceChange: (LanguagePreference) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(PacklySpacing.marginMobile),
        verticalArrangement = Arrangement.spacedBy(PacklySpacing.md),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs)) {
                Text(
                    text = stringResource(R.string.options_language_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.options_language_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs)) {
                LanguagePreference.entries.forEach { option ->
                    LanguageOptionRow(
                        option = option,
                        selected = languagePreference == option,
                        onClick = { onLanguagePreferenceChange(option) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(
    option: LanguagePreference,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(PacklyRadius.lg),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
        shadowElevation = if (selected) 2.dp else 0.dp,
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = PacklySpacing.sm, vertical = PacklySpacing.base),
            horizontalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null)
            Text(
                text = stringResource(option.labelRes),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

private val LanguagePreference.labelRes: Int
    get() = when (this) {
        LanguagePreference.System -> R.string.language_system_default
        LanguagePreference.English -> R.string.language_english
        LanguagePreference.Spanish -> R.string.language_spanish
        LanguagePreference.German -> R.string.language_german
    }
