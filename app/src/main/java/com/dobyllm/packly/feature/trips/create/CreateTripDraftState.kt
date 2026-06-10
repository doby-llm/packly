package com.dobyllm.packly.feature.trips.create

import androidx.compose.runtime.Stable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.core.model.TripStatus

@Stable
class CreateTripDraftState(
    initialName: String = "",
    initialDestination: String = "",
) {
    var name by mutableStateOf(initialName)
        private set

    var destination by mutableStateOf(initialDestination)
        private set

    var showDiscardDialog by mutableStateOf(false)
        private set

    val isDirty: Boolean
        get() = name.isNotBlank() || destination.isNotBlank()

    fun updateName(value: String) {
        name = value
    }

    fun updateDestination(value: String) {
        destination = value
    }

    fun requestClose(onCleanClose: () -> Unit) {
        if (isDirty) {
            showDiscardDialog = true
        } else {
            onCleanClose()
        }
    }

    fun keepEditing() {
        showDiscardDialog = false
    }

    fun discard() {
        name = ""
        destination = ""
        showDiscardDialog = false
    }

    fun duplicateNameIn(doc: PacklyAppDocument): Boolean {
        val trimmedName = name.trim()
        return trimmedName.isNotEmpty() && doc.trips.any { trip ->
            trip.status != TripStatus.Archived && trip.name.equals(trimmedName, ignoreCase = true)
        }
    }

    companion object {
        val Saver: Saver<CreateTripDraftState, List<String>> = Saver(
            save = { state -> listOf(state.name, state.destination) },
            restore = { saved ->
                CreateTripDraftState(
                    initialName = saved.getOrElse(0) { "" },
                    initialDestination = saved.getOrElse(1) { "" },
                )
            },
        )
    }
}

@Composable
fun rememberCreateTripDraftState(): CreateTripDraftState = rememberSaveable(saver = CreateTripDraftState.Saver) {
    CreateTripDraftState()
}
