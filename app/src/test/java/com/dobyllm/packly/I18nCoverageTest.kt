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
    fun englishSpanishAndGermanStringAndPluralKeysMatch() {
        val defaultResources = resources("app/src/main/res/values/strings.xml")
        val spanishResources = resources("app/src/main/res/values-es/strings.xml")
        val germanResources = resources("app/src/main/res/values-de/strings.xml")

        assertEquals(defaultResources.stringKeys, spanishResources.stringKeys)
        assertEquals(defaultResources.stringKeys, germanResources.stringKeys)
        assertEquals(defaultResources.pluralQuantities, spanishResources.pluralQuantities)
        assertEquals(defaultResources.pluralQuantities, germanResources.pluralQuantities)
    }

    @Test
    fun homeSummaryUsesSingularAndPluralResourcesInEveryLocale() {
        val defaultResources = resources("app/src/main/res/values/strings.xml")
        val spanishResources = resources("app/src/main/res/values-es/strings.xml")
        val germanResources = resources("app/src/main/res/values-de/strings.xml")
        val localizedResources = mapOf(
            "values" to defaultResources,
            "values-es" to spanishResources,
            "values-de" to germanResources,
        )

        localizedResources.forEach { (locale, resources) ->
            val homeSummary = resources.plurals["home_summary"].orEmpty()
            assertEquals("$locale home_summary must support exactly-one copy", setOf("one", "other"), homeSummary.keys)
            assertTrue("$locale home_summary[one] must include the trip count", homeSummary["one"].orEmpty().contains("%1${'$'}d"))
            assertTrue("$locale home_summary[other] must include the trip count", homeSummary["other"].orEmpty().contains("%1${'$'}d"))
        }

        val homeScreen = projectFile("app/src/main/java/com/dobyllm/packly/feature/home/HomeScreen.kt").readUtf8Text()
        assertTrue(homeScreen.contains("pluralStringResource("))
        assertTrue(homeScreen.contains("R.plurals.home_summary"))
    }

    @Test
    fun plusJakartaSansIsTheOnlyComposeFontFamily() {
        val typeSource = projectFile("app/src/main/java/com/dobyllm/packly/ui/theme/Type.kt").readUtf8Text()
        listOf(
            "val PlusJakartaSans = FontFamily(",
            "R.font.plus_jakarta_sans_400",
            "R.font.plus_jakarta_sans_500",
            "R.font.plus_jakarta_sans_600",
            "R.font.plus_jakarta_sans_700",
            "R.font.plus_jakarta_sans_800",
            "fontFamily = PlusJakartaSans",
            "bodyLarge = packlyTextStyle(16, 24, FontWeight.Normal)",
            "bodyMedium = packlyTextStyle(16, 24, FontWeight.Normal)",
            "labelMedium = packlyTextStyle(12, 16, FontWeight.SemiBold, 0.6f)",
        ).forEach { snippet ->
            assertTrue("Type.kt must keep bundled Plus Jakarta typography snippet: $snippet", typeSource.contains(snippet))
        }

        val themeSource = projectFile("app/src/main/java/com/dobyllm/packly/ui/theme/Theme.kt").readUtf8Text()
        assertTrue(themeSource.contains("typography = PacklyTypography"))

        listOf(400, 500, 600, 700, 800).forEach { weight ->
            assertTrue(
                "Bundled Plus Jakarta Sans $weight resource must exist",
                Files.exists(projectPath("app/src/main/res/font/plus_jakarta_sans_$weight.ttf")),
            )
        }

        val sourceRoot = projectPath("app/src/main/java/com/dobyllm/packly")
        val fontBypasses = Files.walk(sourceRoot).use { paths ->
            paths.iterator().asSequence()
                .filter { Files.isRegularFile(it) }
                .filter { it.toString().endsWith(".kt") }
                .filterNot { sourceRoot.relativize(it).toString() == "ui/theme/Type.kt" }
                .toList()
        }.flatMap { file ->
                val relativePath = sourceRoot.relativize(file).toString()
                file.readUtf8Text().lineSequence().mapIndexedNotNull { index, line ->
                    if (line.contains("fontFamily") || line.contains("FontFamily")) "$relativePath:${index + 1}: ${line.trim()}" else null
                }.toList()
            }

        assertTrue(
            "All Compose text should inherit Plus Jakarta Sans from PacklyTypography; direct font overrides bypass it:\n" +
                fontBypasses.joinToString("\n"),
            fontBypasses.isEmpty(),
        )
    }

    @Test
    fun stringAndPluralFormatPlaceholdersMatchAcrossLocales() {
        val defaultResources = resources("app/src/main/res/values/strings.xml")
        val localizedResources = listOf(
            "values-es" to resources("app/src/main/res/values-es/strings.xml"),
            "values-de" to resources("app/src/main/res/values-de/strings.xml"),
        )

        val violations = localizedResources.flatMap { (locale, localized) ->
            buildList {
                defaultResources.strings.forEach { (key, defaultValue) ->
                    val localizedValue = localized.strings[key].orEmpty()
                    if (defaultValue.placeholders() != localizedValue.placeholders()) {
                        add("$locale:string/$key ${localizedValue.placeholders()} != ${defaultValue.placeholders()}")
                    }
                }
                defaultResources.plurals.forEach { (key, defaultQuantities) ->
                    defaultQuantities.forEach { (quantity, defaultValue) ->
                        val localizedValue = localized.plurals[key]?.get(quantity).orEmpty()
                        if (defaultValue.placeholders() != localizedValue.placeholders()) {
                            add("$locale:plurals/$key[$quantity] ${localizedValue.placeholders()} != ${defaultValue.placeholders()}")
                        }
                    }
                }
            }
        }

        assertTrue(
            "Localized resources must preserve format placeholders. Violations:\n" + violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }

    @Test
    fun localizedResourcesDoNotContainKnownEnglishUiCopy() {
        val localizedResources = listOf(
            "values-es" to resources("app/src/main/res/values-es/strings.xml"),
            "values-de" to resources("app/src/main/res/values-de/strings.xml"),
        )
        val forbiddenEnglishPhrases = listOf(
            Regex("\\bitem\\(s\\)|\\bitems?\\b", RegexOption.IGNORE_CASE),
            Regex("\\btrips?\\b", RegexOption.IGNORE_CASE),
            Regex("\\blists?\\b", RegexOption.IGNORE_CASE),
            Regex("\\bPack\\s*by\\b", RegexOption.IGNORE_CASE),
            Regex("\\bCreate\\b", RegexOption.IGNORE_CASE),
            Regex("\\bSearch\\b", RegexOption.IGNORE_CASE),
            Regex("\\bSelect\\b", RegexOption.IGNORE_CASE),
            Regex("\\bRename\\b", RegexOption.IGNORE_CASE),
            Regex("\\bDuplicate\\b", RegexOption.IGNORE_CASE),
            Regex("\\bArchive\\b", RegexOption.IGNORE_CASE),
            Regex("\\bUnpacked\\b|\\bPacked\\b", RegexOption.IGNORE_CASE),
            Regex("\\bBackup\\b|\\bRestore\\b", RegexOption.IGNORE_CASE),
        )
        val allowedTerms = listOf("Packly", "Google", "Google Drive", "Android", "URL", "Cloud")

        val violations = localizedResources.flatMap { (locale, resources) ->
            val localizedValues = resources.strings.map { (key, value) ->
                "string/$key" to value
            } + resources.plurals.flatMap { (key, quantities) ->
                quantities.map { (quantity, value) -> "plurals/$key[$quantity]" to value }
            }

            localizedValues.mapNotNull { (label, value) ->
                val textWithoutAllowedTerms = allowedTerms.fold(value) { text, allowedTerm ->
                    text.replace(allowedTerm, "")
                }
                val containsForbiddenPhrase = forbiddenEnglishPhrases.any { it.containsMatchIn(textWithoutAllowedTerms) }
                if (containsForbiddenPhrase) "$locale:$label: $value" else null
            }
        }

        assertTrue(
            "Localized string resources must translate UI copy; intentional product terms are allowlisted. Violations:\n" +
                violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }

    @Test
    fun defaultCountResourcesUsePluralsUnlessAllowlisted() {
        val resources = resources("app/src/main/res/values/strings.xml")
        val allowedCountStrings = setOf(
            "all_categories_count",
            "category_count_label",
            "filter_unpacked_count",
            "filter_packed_count",
            "percent_packed",
            "percent_packed_label",
            "a11y_packing_progress",
        )
        val countKeyPattern = Regex("count", RegexOption.IGNORE_CASE)
        val countValuePattern = Regex("%(?:\\d+\\$)?d.*\\b(items?|trips?|lists?)\\b|\\b(items?|trips?|lists?)\\b.*%(?:\\d+\\$)?d", RegexOption.IGNORE_CASE)
        val parentheticalPluralPattern = Regex("\\((s|es)\\)", RegexOption.IGNORE_CASE)

        val violations = resources.strings.mapNotNull { (key, value) ->
            when {
                parentheticalPluralPattern.containsMatchIn(value) -> "string/$key uses parenthetical plural copy: $value"
                key !in allowedCountStrings && key !in resources.pluralQuantities.keys &&
                    ((countKeyPattern.containsMatchIn(key) && "%" in value) || countValuePattern.containsMatchIn(value)) ->
                    "string/$key looks count-bearing; use plurals or add a deliberate allowlist entry"
                else -> null
            }
        }

        assertTrue(
            "Count-bearing UI copy must use plurals. Violations:\n" + violations.joinToString("\n"),
            violations.isEmpty(),
        )
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
            Regex("\\b(contentDescription|stateDescription)\\s*=\\s*\"[^\"]*[A-Za-z][^\"]*\""),
            Regex("\\b(title|body|actionLabel|label|placeholder|supportingText|percentLabel|metadata)\\s*=\\s*\"[^\"]*[A-Za-z][^\"]*\""),
            Regex("\\bshowSnackbar\\s*\\(\\s*\"[^\"]*[A-Za-z][^\"]*\""),
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

    private fun resources(relativePath: String): I18nResources {
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(projectFile(relativePath).toFile())
        val strings = document.getElementsByTagName("string")
        val plurals = document.getElementsByTagName("plurals")
        val stringValues = buildMap {
            for (index in 0 until strings.length) {
                val item = strings.item(index)
                put(item.attributes.getNamedItem("name").nodeValue, item.textContent.orEmpty())
            }
        }
        val pluralValues = buildMap {
            for (index in 0 until plurals.length) {
                val plural = plurals.item(index)
                val pluralName = plural.attributes.getNamedItem("name").nodeValue
                val items = plural.childNodes
                put(
                    pluralName,
                    buildMap {
                        for (itemIndex in 0 until items.length) {
                            val item = items.item(itemIndex)
                            val quantity = item.attributes?.getNamedItem("quantity")?.nodeValue ?: continue
                            put(quantity, item.textContent.orEmpty())
                        }
                    },
                )
            }
        }
        return I18nResources(strings = stringValues, plurals = pluralValues)
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

    private fun String.placeholders(): List<String> = placeholderPattern.findAll(this)
        .map { match -> "${match.groupValues[1]}$${match.groupValues[2]}" }
        .toList()

    private data class I18nResources(
        val strings: Map<String, String>,
        val plurals: Map<String, Map<String, String>>,
    ) {
        val stringKeys: Set<String> = strings.keys
        val pluralQuantities: Map<String, Set<String>> = plurals.mapValues { (_, quantities) -> quantities.keys }
    }

    private companion object {
        val placeholderPattern = Regex("%(?:(\\d+)\\$)?([dsf])")
    }
}
