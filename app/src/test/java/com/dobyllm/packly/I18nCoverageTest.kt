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
    fun tripAndPackingComposeFlowsDoNotBypassLocalizationForKnownUiCopy() {
        val targetFiles = listOf(
            "app/src/main/java/com/dobyllm/packly/feature/trips/TripDetailScreen.kt",
            "app/src/main/java/com/dobyllm/packly/feature/packing/PackingModeScreen.kt",
            "app/src/main/java/com/dobyllm/packly/ui/component/TripCard.kt",
            "app/src/main/java/com/dobyllm/packly/ui/component/PacklyProgress.kt",
            "app/src/main/java/com/dobyllm/packly/ui/component/PacklySearchBar.kt",
        )
        val localizedUiSnippets = listOf(
            "Packing progress",
            "Start packing",
            "Continue packing",
            "Reset packed items",
            "Trip settings",
            "Items in this trip",
            "Search templates and items",
            "No matches found",
            "All Packed",
            "Filter items",
            "Pack-by reminder due soon with unpacked items",
        )

        targetFiles.forEach { relativePath ->
            val source = projectFile(relativePath).readUtf8Text()
            localizedUiSnippets.forEach { snippet ->
                assertTrue(
                    "$snippet should come from string resources in $relativePath",
                    !source.contains(snippet),
                )
            }
        }
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

    private fun projectFile(relativePath: String): Path {
        val searchedPaths = generateSequence(testWorkingDirectory) { it.parent }
            .map { it.resolve(relativePath).normalize() }
            .toList()
        return searchedPaths.firstOrNull { Files.isRegularFile(it) }
            ?: throw FileNotFoundException(
                "Could not find $relativePath from Gradle/JUnit working directory " +
                    "$testWorkingDirectory. Searched: ${searchedPaths.joinToString()}",
            )
    }

    private fun Path.readUtf8Text(): String = toFile().readText(Charsets.UTF_8)
}
