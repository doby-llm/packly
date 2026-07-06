package com.dobyllm.packly.feature.options

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.R
import com.dobyllm.packly.core.model.LanguagePreference
import com.dobyllm.packly.core.model.PacklyCloudSyncConnectionStatus
import com.dobyllm.packly.core.model.PacklyCloudSyncSettings
import com.dobyllm.packly.ui.token.PacklyRadius
import com.dobyllm.packly.ui.token.PacklySpacing

@Composable
fun OptionsScreen(
    languagePreference: LanguagePreference,
    cloudSyncSettings: PacklyCloudSyncSettings = PacklyCloudSyncSettings(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onLanguagePreferenceChange: (LanguagePreference) -> Unit,
    onGoogleDriveSyncClick: () -> Unit = {},
) {
    var showLanguagePicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(PacklySpacing.marginMobile),
        verticalArrangement = Arrangement.spacedBy(PacklySpacing.md),
    ) {
        item {
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
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(PacklyRadius.lg),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHigh),
                shadowElevation = 1.dp,
            ) {
                Column(modifier = Modifier.padding(PacklySpacing.marginMobile), verticalArrangement = Arrangement.spacedBy(PacklySpacing.base)) {
                    Text(
                        text = stringResource(R.string.options_section_preferences),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Surface(
                        onClick = { showLanguagePicker = !showLanguagePicker },
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(PacklyRadius.md),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Row(
                            modifier = Modifier.padding(PacklySpacing.sm),
                            horizontalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.options_language_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(stringResource(languagePreference.labelRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (showLanguagePicker) {
                        Text(
                            text = stringResource(R.string.options_language_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
        }
        item {
            GoogleDriveSyncCard(
                settings = cloudSyncSettings,
                onSyncClick = onGoogleDriveSyncClick,
            )
        }
    }
}

@Composable
private fun GoogleDriveSyncCard(settings: PacklyCloudSyncSettings, onSyncClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(PacklyRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHigh),
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(PacklySpacing.marginMobile), verticalArrangement = Arrangement.spacedBy(PacklySpacing.base)) {
            Text(
                text = stringResource(R.string.options_section_cloud_sync),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.options_google_drive_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(settings.status.statusLabelRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    settings.lastError?.takeIf { it.isNotBlank() }?.let { error ->
                        Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Text(
                text = stringResource(R.string.options_google_drive_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onSyncClick,
                enabled = settings.status != PacklyCloudSyncConnectionStatus.Syncing,
            ) {
                Text(stringResource(R.string.action_sync_now))
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(option: LanguagePreference, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(PacklyRadius.lg),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
        shadowElevation = if (selected) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = PacklySpacing.sm, vertical = PacklySpacing.base),
            horizontalArrangement = Arrangement.spacedBy(PacklySpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null)
            Text(text = stringResource(option.labelRes), style = MaterialTheme.typography.bodyLarge, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
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

private val PacklyCloudSyncConnectionStatus.statusLabelRes: Int
    get() = when (this) {
        PacklyCloudSyncConnectionStatus.Disabled -> R.string.cloud_sync_disabled
        PacklyCloudSyncConnectionStatus.NotConfigured -> R.string.cloud_sync_not_configured
        PacklyCloudSyncConnectionStatus.Disconnected -> R.string.cloud_sync_disconnected
        PacklyCloudSyncConnectionStatus.Syncing -> R.string.cloud_sync_syncing
        PacklyCloudSyncConnectionStatus.Synced -> R.string.cloud_sync_synced
        PacklyCloudSyncConnectionStatus.Offline -> R.string.cloud_sync_offline
        PacklyCloudSyncConnectionStatus.NeedsAttention -> R.string.cloud_sync_needs_attention
    }
