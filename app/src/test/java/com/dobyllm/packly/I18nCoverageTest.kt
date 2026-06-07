package com.dobyllm.packly

import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class I18nCoverageTest {
    private val testWorkingDirectory: Path = Path.of(System.getProperty("user.dir"))
        .toAbsolutePath()
        .normalize()

    @Test
    fun englishSpanishAndGermanStringKeysMatch() {
        val defaultKeys = stringKeys("app/src/main/res/values/strings.xml")
        val spanishKeys = stringKeys("app/src/main/res/values-es/strings.xml")
        val germanKeys = stringKeys("app/src/main/res/values-de/strings.xml")

        assertEquals(defaultKeys, spanishKeys)
        assertEquals(defaultKeys, germanKeys)
    }

    @Test
    fun composeUiCodeDoesNotBypassStringResourcesForVisibleCopy() {
        val sourceRoot = projectPath("app/src/main/java/com/dobyllm/packly")
        val sourceFiles = Files.walk(sourceRoot)
            .filter { Files.isRegularFile(it) }
            .filter { it.toString().endsWith(".kt") }
            .filter { !it.toString().endsWith("ui/i18n/SeedDisplayNames.kt") }
            .toList()
        val uiLiteralPatterns = listOf(
            Regex("\\bText\\s*\\(\\s*\"[^\"]*[A-Za-z][^\"]*\""),
            Regex("\\bcontentDescription\\s*=\\s*\"[^\"]*[A-Za-z][^\"]*\""),
            Regex("\\bstateDescription\\s*=\\s*\"[^\"]*[A-Za-z][^\"]*\""),
            Regex("\\b(title|body|actionLabel|label|placeholder|supportingText|percentLabel|metadata)\\s*=\\s*\"[^\"]*[A-Za-z][^\"]*\""),
        )
        val allowedNonUiLiterals = listOf(
            "DateTimeFormatter.ofPattern",
            "item(key =",
            "const val",
            "getString(\"",
            "Regex(\"",
            "JsonPrimitive",
            "schemaVersion",
            "@Suppress",
            "CategoryToken(",
            "normalizedForDedupe",
            "stableRequestCode",
            "Text(\"$",
            "packing-progress",
        )

        val violations = sourceFiles.flatMap { file ->
            val relativePath = sourceRoot.relativize(file).toString()
            file.readUtf8Text().lineSequence().mapIndexedNotNull { index, line ->
                val trimmed = line.trim()
                val isAllowed = allowedNonUiLiterals.any(trimmed::contains)
                val isViolation = !isAllowed && uiLiteralPatterns.any { it.containsMatchIn(line) }
                if (isViolation) "$relativePath:${index + 1}: $trimmed" else null
            }.toList()
        }

        assertTrue(
            "User-visible Compose literals must use stringResource/pluralStringResource. Violations:\n" +
                violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }

    @Test
    fun tripAndListScreensUseLocalizedSeedDisplayHelpers() {
        val forbiddenSnippetsByFile = mapOf(
            "app/src/main/java/com/dobyllm/packly/feature/trips/TripDetailScreen.kt" to listOf(
                "Text(entry.nameSnapshot",
                "itemName = entry.nameSnapshot",
                "a11y_remove_named, entry.nameSnapshot",
                "item.name.contains(normalizedQuery",
                ".label.contains(normalizedQuery",
                "list.name.contains(normalizedQuery",
                "list.description.contains(normalizedQuery",
                "label = category.label",
                "a11y_remove_list_from_trip, list.name",
                "a11y_add_list_to_trip, list.name",
                "Text(list.name",
                "a11y_remove_named, list.name",
                "a11y_add_named, list.name",
                "a11y_remove_item_from_trip, item.name",
                "a11y_add_named, item.name",
                "Text(item.name",
                "text = category.label",
                "sourceList?.name?.let",
            ),
            "app/src/main/java/com/dobyllm/packly/feature/lists/ListDetailScreen.kt" to listOf(
                "title = item.name",
                "category?.label",
                "text = list.name",
                "text = list.description",
            ),
            "app/src/main/java/com/dobyllm/packly/feature/lists/AddItemsToListSheet.kt" to listOf(
                "it.id to it.label",
                "item.name.contains(query",
                "label = category.label",
                "title = item.name",
                "category?.label",
                "sortedBy { it.name.lowercase()",
            ),
            "app/src/main/java/com/dobyllm/packly/feature/lists/ListsScreen.kt" to listOf(
                "list_duplicated_snackbar, list.name",
                "archive_list_title, list.name",
                "rename_list_title, list.name",
                "it.id to it.label",
                "item.name.contains(itemQuery",
                "label = category.label",
                "title = item.name",
                "category?.label",
                "sortedBy { it.name.lowercase()",
            ),
            "app/src/main/java/com/dobyllm/packly/feature/items/ItemsScreen.kt" to listOf(
                "archive_item_title, item.name",
                "category_count_label, category.label",
                "name.contains(query",
            ),
            "app/src/main/java/com/dobyllm/packly/feature/items/EditItemSheet.kt" to listOf(
                "label = category.label",
            ),
            "app/src/main/java/com/dobyllm/packly/feature/trips/CreateTripScreen.kt" to listOf(
                "item.name.contains(itemQuery",
                "category_count_label, list.name",
                "selected_list_remove_label, list.name",
                "item_already_included_label, item.name",
                "TripReviewItem(itemId, entry.itemNameSnapshot",
                "TripReviewItem(item.id, item.name",
                "?.label ?: stringResource(R.string.unknown_category)",
            ),
            "app/src/main/java/com/dobyllm/packly/feature/packing/PackingModeScreen.kt" to listOf(
                "entry.nameSnapshot",
                "text = category?.label",
                "category?.label ?: stringResource(R.string.category_other)",
            ),
        )

        val violations = forbiddenSnippetsByFile.flatMap { (relativePath, forbiddenSnippets) ->
            val source = projectFile(relativePath).readUtf8Text()
            forbiddenSnippets.filter(source::contains).map { snippet -> "$relativePath contains `$snippet`" }
        }

        assertTrue(
            "Seed-backed UI fields must go through displayName/displayLabel/displayDescription helpers. Violations:\n" +
                violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }

    private fun stringKeys(relativePath: String): Set<String> {
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(projectFile(relativePath).toFile())
        val strings = document.getElementsByTagName("string")
        return buildSet {
            for (index in 0 until strings.length) {
                val item = strings.item(index)
                add(item.attributes.getNamedItem("name").nodeValue)
            }
        }
    }

    private fun projectFile(relativePath: String): Path = projectPath(relativePath).also { path ->
        if (!Files.isRegularFile(path)) {
            throw FileNotFoundException("Could not find regular file $relativePath at $path")
        }
    }

    private fun projectPath(relativePath: String): Path {
        val searchedPaths = generateSequence(testWorkingDirectory) { it.parent }
            .map { it.resolve(relativePath).normalize() }
            .toList()
        return searchedPaths.firstOrNull { Files.exists(it) }
            ?: throw FileNotFoundException(
                "Could not find $relativePath from Gradle/JUnit working directory " +
                    "$testWorkingDirectory. Searched: ${searchedPaths.joinToString()}",
            )
    }

    private fun Path.readUtf8Text(): String = toFile().readText(Charsets.UTF_8)
}
