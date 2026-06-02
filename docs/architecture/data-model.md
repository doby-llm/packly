# Packly Data Model Blueprint

## Modeling principles

Packly has three user-facing concepts:

1. Items are reusable library entries.
2. Lists are reusable packing templates.
3. Trips are real packing sessions and snapshot list contents at creation time.

Editing an item or list after a trip exists must not unexpectedly rewrite the active trip checklist.

## Identity and timestamps

Use stable string IDs for persisted entities.

Recommended ID format:

- Prefix plus random/UUID payload, for example item_..., list_..., trip_..., cat_....
- IDs are internal and must not encode display names.

Store timestamps as ISO-8601 UTC strings for JSON readability and migration stability.

## Top-level JSON document

Recommended MVP shape:

```json
{
  "schemaVersion": 1,
  "items": [],
  "lists": [],
  "trips": [],
  "categories": [],
  "settings": {},
  "session": {}
}
```

One versioned document is sufficient for MVP data volume. If write contention or size grows, split behind repository interfaces into items.json, lists.json, trips.json, and settings.json.

## Category

```kotlin
@Serializable
data class PacklyCategory(
    val id: CategoryId,
    val key: String,
    val label: String,
    val iconKey: String,
    val accentColorHex: String,
    val softColorHex: String,
    val sortOrder: Int,
    val isSeed: Boolean = true,
    val isArchived: Boolean = false
)
```

Rules:

- Persist category keys and icon keys, not localized labels.
- Use category ID/key for references; keep display colors in token maps so retheming can override them.
- Archive categories instead of deleting if existing items, lists, or trip snapshots reference them.

Default category keys:

- clothing
- toiletries
- electronics
- documents
- health
- comfort
- weather
- family
- food
- misc

## Item library entry

```kotlin
@Serializable
data class PacklyItem(
    val id: ItemId,
    val name: String,
    val categoryId: CategoryId,
    val defaultQuantity: Int = 1,
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val isSeed: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: InstantString,
    val updatedAt: InstantString
)
```

Validation:

- Name is required after trimming.
- Quantity must be positive.
- Duplicate names should warn but not hard-block.
- Deleting a library item should soft-archive it so lists and historical trips remain understandable.

## Packing list template

```kotlin
@Serializable
data class PacklyList(
    val id: ListId,
    val name: String,
    val description: String = "",
    val entries: List<PacklyListEntry>,
    val isSeed: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: InstantString,
    val updatedAt: InstantString
)

@Serializable
data class PacklyListEntry(
    val id: ListEntryId,
    val itemId: ItemId?,
    val itemNameSnapshot: String,
    val categoryIdSnapshot: CategoryId,
    val quantity: Int = 1,
    val notes: String = "",
    val sortOrder: Int
)
```

Rules:

- Keep snapshots on list entries so later item edits do not erase user meaning.
- Preserve itemId when the entry came from the library; allow null for future one-off entries.
- Lists are templates, not progress trackers.

## Trip

```kotlin
@Serializable
data class PacklyTrip(
    val id: TripId,
    val name: String,
    val destination: String = "",
    val startDate: LocalDateString? = null,
    val endDate: LocalDateString? = null,
    val sourceListId: ListId? = null,
    val status: TripStatus = TripStatus.Active,
    val entries: List<TripEntry>,
    val createdAt: InstantString,
    val updatedAt: InstantString,
    val completedAt: InstantString? = null
)

@Serializable
enum class TripStatus { Draft, Active, Completed, Archived }

@Serializable
data class TripEntry(
    val id: TripEntryId,
    val sourceItemId: ItemId?,
    val sourceListEntryId: ListEntryId?,
    val nameSnapshot: String,
    val categoryIdSnapshot: CategoryId,
    val quantity: Int = 1,
    val notes: String = "",
    val isPacked: Boolean = false,
    val packedAt: InstantString? = null,
    val sortOrder: Int
)
```

Rules:

- Trips always snapshot list entries at creation.
- Later library/list edits do not mutate existing trips.
- Packing progress is derived from entries.count { isPacked } and total entries; do not persist redundant progress unless needed later.
- Resetting packing state clears isPacked and packedAt after confirmation.

## Settings and session state

```kotlin
@Serializable
data class PacklySettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColorEnabled: Boolean = false,
    val selectedPaletteKey: String = "packly_default",
    val firstLaunchCompleted: Boolean = false
)

@Serializable
data class PacklySessionState(
    val collapsedCategoryKeysByScreen: Map<String, Set<String>> = emptyMap(),
    val lastUsedCategoryId: CategoryId? = null,
    val lastOpenedTripId: TripId? = null
)
```

Session state may persist across launches, but it must never be required for domain correctness.

## Seed data

Seed only when the repository has no user data or when a migration explicitly adds missing seed values without overwriting user edits.

Seed at least 24 reusable items from the UX concept:

- Clothing: T-shirts, Underwear, Socks, Pants / shorts, Sweater or hoodie, Sleepwear, Comfortable shoes
- Toiletries: Toothbrush, Toothpaste, Shampoo, Deodorant, Hairbrush / comb
- Electronics: Phone charger, Power bank, Headphones, Laptop charger, Travel adapter
- Documents: Passport / ID, Tickets / boarding pass, Wallet, Travel insurance card
- Health: Medication, Sunscreen, First-aid basics
- Travel Comfort: Water bottle, Book / e-reader, Neck pillow

Seed starter lists:

- Weekend
- Beach day
- Business trip
- Camping
- Family visit

## Derived UI models

Do not persist UI-only grouped structures. Build them from domain state:

- CategorySectionUiState(category, items, isCollapsed, packedCount, totalCount)
- TripProgressUiState(packedCount, totalCount, percent)
- ListCardUiState(name, itemCount, topCategoryIcons, lastUpdated)

## Scalability considerations

MVP JSON scale should handle hundreds to low thousands of entries. To prepare for growth:

- Keep repository operations off the main thread.
- Update data through immutable copy operations and atomic serializer writes.
- Use stable IDs as Compose LazyColumn keys.
- Avoid loading icons/images from persisted color data; map icon keys to vector assets.
- Split JSON documents if full-document rewrites become visible during rapid packing.
- Introduce indexed in-memory search before introducing Room; Packly does not need a database until data volume or query complexity justifies it.
