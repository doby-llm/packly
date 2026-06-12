package com.dobyllm.packly

import com.dobyllm.packly.core.model.PacklyList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ListActionsTest {
    @Test
    fun renameListForActionTrimsNameAndKeepsDescriptionAndEntries() {
        val original = activeList(name = "Weekend", description = "A light two-day starter list.")

        val renamed = original.renamedForListAction("  Long Weekend  ", now = "2026-06-03T09:30:00Z")

        assertEquals("Long Weekend", renamed.name)
        assertEquals(original.description, renamed.description)
        assertEquals(original.entries, renamed.entries)
        assertEquals(original.createdAt, renamed.createdAt)
        assertEquals("2026-06-03T09:30:00Z", renamed.updatedAt)
    }

    @Test
    fun duplicateListForActionCreatesActiveCopyWithIndependentIdAndCopyName() {
        val original = activeList(name = "Weekend", description = "A light two-day starter list.")

        val duplicate = original.duplicatedForListAction(
            newListId = "list_copy",
            now = "2026-06-03T09:30:00Z",
            existingNames = setOf("Weekend", "Weekend copy"),
            copyNameTemplates = englishCopyNameTemplates,
        )

        assertEquals("list_copy", duplicate.id)
        assertEquals("Weekend copy 2", duplicate.name)
        assertEquals(original.description, duplicate.description)
        assertEquals(original.entries, duplicate.entries)
        assertFalse(duplicate.isArchived)
        assertFalse(duplicate.isSeed)
        assertEquals("2026-06-03T09:30:00Z", duplicate.createdAt)
        assertEquals("2026-06-03T09:30:00Z", duplicate.updatedAt)
    }

    @Test
    fun duplicateListForActionUsesLocalizedCopyNameTemplatesAndPreservesBaseName() {
        val original = activeList(name = "  Fin de semana  ")

        val duplicate = original.duplicatedForListAction(
            newListId = "list_copy_es",
            now = "2026-06-03T09:30:00Z",
            existingNames = setOf("  Fin de semana  ", "  Fin de semana   copia"),
            copyNameTemplates = ListCopyNameTemplates(
                unnumberedTemplate = "%1${'$'}s copia",
                numberedTemplate = "%1${'$'}s copia %2${'$'}d",
            ),
        )

        assertEquals("  Fin de semana   copia 2", duplicate.name)
    }

    private fun activeList(
        name: String,
        description: String = "",
    ) = PacklyList(
        id = "list_weekend",
        name = name,
        description = description,
        entries = emptyList(),
        isSeed = true,
        isArchived = false,
        createdAt = "2026-06-01T09:00:00Z",
        updatedAt = "2026-06-01T09:00:00Z",
    )

    private companion object {
        val englishCopyNameTemplates = ListCopyNameTemplates(
            unnumberedTemplate = "%1${'$'}s copy",
            numberedTemplate = "%1${'$'}s copy %2${'$'}d",
        )
    }
}
