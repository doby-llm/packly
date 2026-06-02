# Packly Persistence and Migration Blueprint

## Persistence goal

Packly should save user data locally in a JSON-style format that survives app restarts, preserves packing session progress, and evolves safely across app updates.

## Recommended persistence adapter

Use Jetpack DataStore with a custom Kotlinx Serialization JSON serializer.

Why this is the right MVP fit:

- Better corruption and transactional update behavior than manual file writes.
- More JSON-aligned and migration-friendly than flat SharedPreferences keys.
- Less architectural weight than Room for a small local document model.
- Easy to replace behind repository interfaces if later data volume requires Room or cloud sync.

## Repository boundary

Domain code depends on repository interfaces only:

```kotlin
interface PacklyRepository {
    val appState: Flow<PacklyAppDocument>
    suspend fun updateItems(transform: (List<PacklyItem>) -> List<PacklyItem>)
    suspend fun updateLists(transform: (List<PacklyList>) -> List<PacklyList>)
    suspend fun updateTrips(transform: (List<PacklyTrip>) -> List<PacklyTrip>)
    suspend fun updateSettings(transform: (PacklySettings) -> PacklySettings)
}
```

Data layer owns the DataStore instance, serializers, migrations, corruption handling, and seed bootstrapping.

## JSON serializer contract

Recommended behavior:

- Ignore unknown keys so future app versions can roll back without immediate crashes.
- Encode defaults consistently.
- Avoid explicit nulls where defaults are clearer.
- Fail closed on invalid required fields, then use a recoverable error path rather than silently deleting data.

Kotlinx Serialization configuration:

```kotlin
Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
    prettyPrint = false
}
```

## Schema versioning

Every persisted document has schemaVersion.

Migration pipeline:

1. Read raw JSON.
2. Detect version.
3. Apply ordered migrations until current version.
4. Decode into current PacklyAppDocument.
5. Persist upgraded document on next successful write or explicitly after migration.

```kotlin
interface PacklyJsonMigration {
    val fromVersion: Int
    val toVersion: Int
    fun migrate(rawJson: JsonObject): JsonObject
}
```

Migration rules:

- Migrations are deterministic.
- Never copy code/data from packly-deprecated.
- Never drop unknown user-created items/lists/trips without an explicit backup/error path.
- Add new fields with defaults.
- Convert labels to stable keys when needed.
- Keep migration tests in the future Android project and run them only through GitHub Actions.

## Corruption handling

If JSON is unreadable:

- Preserve the bad file if possible before replacing it.
- Start with a safe empty/seeded document.
- Show a human-readable error such as “Couldn’t load saved packing data. A safe starter workspace was opened.”
- Do not silently erase user data without a backup attempt.

## Seeding strategy

Seed in data layer through SeedDataProvider.

Rules:

- On first launch with no user data, create default categories, 24+ seed items, and starter lists.
- Mark seed entities with isSeed = true.
- Allow users to duplicate or edit seed lists according to UX copy.
- On later app versions, add missing seed categories/items without overwriting user edits.

## Session persistence

Persist lightweight UI session state only when it improves user confidence:

- Collapsed category sections by screen.
- Last used category for add-item forms.
- Last opened trip.
- Theme settings.

Do not persist transient snackbar/dialog states.

## Write operations

Use transactional update APIs.

Examples:

- Add item: append item, update timestamp, optionally update last used category.
- Archive item: set isArchived, do not remove from historical references.
- Create trip from list: snapshot list entries into trip entries in one transaction.
- Toggle packed: update one trip entry by ID and set/clear packedAt.
- Undo toggle: restore prior packed value and timestamp.

## Scalability and data volume

Expected MVP data volume is small. Likely bottlenecks are unnecessary recomposition and frequent full-document writes during rapid packing toggles, not storage size.

Mitigations:

- Batch or serialize rapid toggle writes through DataStore update transactions.
- Keep UI responsive with optimistic state while persistence completes.
- Use stable row IDs and feature-specific derived state.
- Consider splitting trips into a separate JSON document if active packing writes become too frequent.
- Consider Room only if query complexity, sync, or thousands of items/trips make JSON updates measurably slow.

## Validation without local builds

Local workers must not run Android builds, lint, unit tests, connected tests, or APK assembly. Persistence implementation and migration tests must be validated through GitHub Actions once the Android skeleton exists.

Allowed local work for this task class:

- Read/write docs.
- Inspect files.
- Use git status/diff/log.
- Prepare code/docs commits.

Forbidden local commands include:

- ./gradlew build
- ./gradlew assembleDebug
- ./gradlew lintDebug
- ./gradlew testDebugUnitTest
- ./gradlew connectedAndroidTest
