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
            .filterNot { it.toString().endsWith("ui/i18n/SeedDisplayNames.kt") }
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
