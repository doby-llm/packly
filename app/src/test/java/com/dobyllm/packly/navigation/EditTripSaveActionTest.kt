package com.dobyllm.packly.navigation

import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditTripSaveActionTest {
    @Test
    fun invokeNavigatesOnlyAfterCommitCompletionAndIgnoresRepeatedActivation() {
        val events = mutableListOf<String>()
        var completeCommit: (() -> Unit)? = null
        val action = EditTripSaveAction(
            commitDraft = { onCommitted ->
                events += "commit-start"
                completeCommit = {
                    events += "commit-complete"
                    onCommitted()
                }
            },
            navigateToPackingMode = { events += "navigate" },
        )

        action.invoke()
        action.invoke()

        assertEquals(listOf("commit-start"), events)

        completeCommit?.invoke()
        action.invoke()

        assertEquals(listOf("commit-start", "commit-complete", "navigate"), events)
    }

    @Test
    fun navHostWiresCommitCompletionBeforeDiscardAndNavigation() {
        val navHostSource = projectFile("app/src/main/java/com/dobyllm/packly/navigation/PacklyNavHost.kt")
            .toFile()
            .readText(Charsets.UTF_8)
        val saveBlock = navHostSource
            .substringAfter("val editTripSave = remember(editTripId, vm, navController) {")
            .substringBefore("val tripFromListDefaultName")
        val completionIndex = saveBlock.indexOf("onCompleted = {")
        val discardIndex = saveBlock.indexOf("editTripDraftState.discard()", completionIndex)
        val navigationCallbackIndex = saveBlock.indexOf("onCommitted()", discardIndex)

        assertTrue("save must provide a persistence completion callback", completionIndex >= 0)
        assertTrue("draft must be discarded after persistence completes", discardIndex > completionIndex)
        assertTrue("navigation must follow draft discard", navigationCallbackIndex > discardIndex)
        assertTrue(navHostSource.contains("editTripDraftState.selectItem(itemId)"))
        assertTrue(navHostSource.contains("itemIds = editTripDraftState.selectedItemIds"))
        assertTrue(navHostSource.contains("itemQuantities = editTripDraftState.itemQuantities"))
        assertTrue(navHostSource.contains("popUpTo(PacklyRoute.EditTripLists) { inclusive = true }"))

        val viewModelSource = projectFile("app/src/main/java/com/dobyllm/packly/PacklyAppViewModel.kt")
            .toFile()
            .readText(Charsets.UTF_8)
        val updateTripContentsBlock = viewModelSource
            .substringAfter("fun updateTripContents(")
            .substringBefore("fun updateTripDeadline(")
        assertTrue(
            "updateTripContents must invoke completion only after updateDocument returns",
            updateTripContentsBlock.indexOf("repository.updateDocument") < updateTripContentsBlock.indexOf("onCompleted()"),
        )
    }

    @Test
    fun navHostUsesOneParentOwnedSaveWithoutLifecycleCallbackFallback() {
        val source = projectFile("app/src/main/java/com/dobyllm/packly/navigation/PacklyNavHost.kt").toFile().readText(Charsets.UTF_8)

        assertTrue(source.contains("val editTripId = backStackEntry?.arguments?.getString(\"tripId\").takeIf { isEditTripRoute }"))
        assertTrue(source.contains("val editTripSave = remember(editTripId, vm, navController)"))
        assertEquals(2, source.split("editTripSave::invoke").size - 1)
        assertFalse(source.contains("editTripSaveAction"))
        assertFalse(source.contains("DisposableEffect(id)"))
        assertFalse(source.contains("onDispose { editTripSaveAction = null }"))
        assertFalse(source.contains("?: navController.popBackStack()"))
    }

    private fun projectFile(relativePath: String): Path {
        val workingDirectory = Path.of(System.getProperty("user.dir"))
            .toAbsolutePath()
            .normalize()
        val searchedPaths = generateSequence(workingDirectory) { it.parent }
            .map { it.resolve(relativePath).normalize() }
            .toList()
        return searchedPaths.firstOrNull { Files.isRegularFile(it) }
            ?: throw FileNotFoundException("Could not find regular file $relativePath at $workingDirectory")
    }
}
