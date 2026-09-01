package com.dobyllm.packly.navigation

import java.util.concurrent.atomic.AtomicBoolean

/** Coordinates the single commit-and-navigate operation for an edit-trip session. */
internal class EditTripSaveAction(
    private val commitDraft: (onCommitted: () -> Unit) -> Unit,
    private val navigateToPackingMode: () -> Unit,
) {
    private val hasSaved = AtomicBoolean(false)

    fun invoke() {
        if (!hasSaved.compareAndSet(false, true)) return

        commitDraft(navigateToPackingMode)
    }
}
