package com.dobyllm.packly.feature.options

import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.R
import com.dobyllm.packly.core.model.LanguagePreference
import com.dobyllm.packly.ui.token.PacklyElevation
import com.dobyllm.packly.ui.token.PacklyRadius
import com.dobyllm.packly.ui.token.PacklySpacing

@Composable
fun OptionsScreen(
    languagePreference: LanguagePreference,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onLanguagePreferenceChange: (LanguagePreference) -> Unit,
) {
    var isLanguagePickerExpanded by rememberSaveable { mutableStateOf(false) }
    val languageSummary = stringResource(languagePreference.labelRes)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(PacklySpacing.marginMobile),
        verticalArrangement = Arrangement.spacedBy(PacklySpacing.md),
    ) {
        item {
            OptionsHeader()
        }

        item {
            SettingsSection(
                title = stringResource(R.string.options_section_preferences),
            ) {
                SettingsRow(
                    row = SettingsRowData(
                        icon = Icons.Rounded.Language,
                        titleRes = R.string.options_language_title,
                        summary = languageSummary,
                    ),
                    expanded = isLanguagePickerExpanded,
                    onClick = { isLanguagePickerExpanded = !isLanguagePickerExpanded },
                )

                if (isLanguagePickerExpanded) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    LanguagePicker(
                        languagePreference = languagePreference,
                        onLanguagePreferenceChange = onLanguagePreferenceChange,
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionsHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs)) {
        Text(
            text = stringResource(R.string.options_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.options_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable Column.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            shape = RoundedCornerShape(PacklyRadius.xl),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = PacklyElevation.card,
            shadowElevation = PacklyElevation.card,
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsRow(
    row: SettingsRowData,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val title = stringResource(row.titleRes)
    val rowDescription = stringResource(R.string.a11y_options_language_row, title, row.summary)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = rowDescription }
            .padding(horizontal = PacklySpacing.marginMobile, vertical = PacklySpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(PacklyRadius.full),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = row.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = row.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Icon(
            imageVector = if (expanded) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LanguagePicker(
    languagePreference: LanguagePreference,
    onLanguagePreferenceChange: (LanguagePreference) -> Unit,
) {
    Column(
        modifier = Modifier.padding(
            start = PacklySpacing.marginMobile,
            end = PacklySpacing.marginMobile,
            top = PacklySpacing.sm,
            bottom = PacklySpacing.marginMobile,
        ),
        verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
    ) {
        Text(
            text = stringResource(R.string.options_language_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LanguagePreference.entries.forEach { option ->
            LanguageOptionRow(
                option = option,
                selected = languagePreference == option,
                onClick = { onLanguagePreferenceChange(option) },
            )
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
        shape = RoundedCornerShape(PacklyRadius.lg),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = PacklySpacing.sm, vertical = PacklySpacing.base),
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

private data class SettingsRowData(
    val icon: ImageVector,
    @StringRes val titleRes: Int,
    val summary: String,
)

private val LanguagePreference.labelRes: Int
    get() = when (this) {
        LanguagePreference.System -> R.string.language_system_default
        LanguagePreference.English -> R.string.language_english
        LanguagePreference.Spanish -> R.string.language_spanish
        LanguagePreference.German -> R.string.language_german
    }
