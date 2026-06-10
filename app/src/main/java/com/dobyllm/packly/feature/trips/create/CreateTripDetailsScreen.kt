package com.dobyllm.packly.feature.trips.create

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dobyllm.packly.R
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.ui.token.PacklyRadius
import com.dobyllm.packly.ui.token.PacklySpacing

private const val CREATE_TRIP_TOTAL_STEPS = 4

@Composable
fun CreateTripDetailsScreen(
    doc: PacklyAppDocument,
    draftState: CreateTripDraftState,
    contentPadding: PaddingValues,
    onNext: () -> Unit,
    onCloseConfirmed: () -> Unit,
) {
    val trimmedName = draftState.name.trim()
    val isNameMissing = trimmedName.isEmpty()
    val isDuplicateName = draftState.duplicateNameIn(doc)
    val canContinue = !isNameMissing && !isDuplicateName

    BackHandler { draftState.requestClose(onCloseConfirmed) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp),
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = PacklySpacing.marginMobile,
                    top = PacklySpacing.sm,
                    end = PacklySpacing.marginMobile,
                    bottom = PacklySpacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(PacklySpacing.md),
            ) {
                item {
                    CreateTripProgressHeader(
                        step = 1,
                        title = stringResource(R.string.create_trip_step1_title),
                        body = stringResource(R.string.create_trip_step1_body),
                    )
                }
                item {
                    CreateTripTextField(
                        value = draftState.name,
                        onValueChange = draftState::updateName,
                        label = stringResource(R.string.create_trip_name_label),
                        placeholder = stringResource(R.string.create_trip_name_placeholder),
                        supportingText = when {
                            isDuplicateName -> stringResource(R.string.error_duplicate_trip_name)
                            isNameMissing -> stringResource(R.string.create_trip_name_required_supporting)
                            else -> stringResource(R.string.field_required)
                        },
                        isError = isDuplicateName,
                        singleLine = true,
                    )
                }
                item {
                    CreateTripTextField(
                        value = draftState.destination,
                        onValueChange = draftState::updateDestination,
                        label = stringResource(R.string.create_trip_destination_label_optional),
                        placeholder = stringResource(R.string.create_trip_destination_placeholder),
                        singleLine = true,
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = PacklySpacing.marginMobile, vertical = PacklySpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(PacklySpacing.xs),
                ) {
                    if (!canContinue) {
                        Text(
                            text = if (isDuplicateName) {
                                stringResource(R.string.create_trip_disabled_duplicate_name)
                            } else {
                                stringResource(R.string.create_trip_disabled_missing_name)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        enabled = canContinue,
                        onClick = onNext,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(PacklyRadius.default),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Text(stringResource(R.string.action_next))
                    }
                }
            }
        }
    }

    if (draftState.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = draftState::keepEditing,
            title = { Text(stringResource(R.string.create_trip_discard_title)) },
            text = { Text(stringResource(R.string.create_trip_discard_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        draftState.discard()
                        onCloseConfirmed()
                    },
                ) {
                    Text(stringResource(R.string.create_trip_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = draftState::keepEditing) {
                    Text(stringResource(R.string.create_trip_keep_editing))
                }
            },
        )
    }
}

@Composable
fun CreateTripStepPlaceholderScreen(
    step: Int,
    contentPadding: PaddingValues,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(PacklySpacing.marginMobile),
        contentAlignment = Alignment.TopCenter,
    ) {
        CreateTripProgressHeader(
            step = step,
            title = stringResource(R.string.create_trip_step_placeholder_title),
            body = stringResource(R.string.create_trip_step_placeholder_body),
        )
    }
}

@Composable
private fun CreateTripProgressHeader(
    step: Int,
    title: String,
    body: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PacklySpacing.sm)) {
        Text(
            text = stringResource(R.string.create_trip_step_label, step, CREATE_TRIP_TOTAL_STEPS),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        LinearProgressIndicator(
            progress = { step / CREATE_TRIP_TOTAL_STEPS.toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
        Spacer(modifier = Modifier.padding(top = PacklySpacing.xs))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CreateTripTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        singleLine = singleLine,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PacklyRadius.default),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    )
}
