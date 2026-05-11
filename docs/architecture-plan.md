# Packly — Architecture & Implementation Plan

> **App:** Packly — Smart Packing List Manager
> **Platform:** Android (Jetpack Compose + Kotlin)
> **Architect:** Hermes Software Architect Agent
> **Date:** 2026-05-11

---

## 1. Architecture Overview

### 1.1 Design Pattern: **Clean Architecture (3-layer) + Unidirectional Data Flow (UDF)**

```
┌────────────────────────────────────────────┐
│              UI LAYER                       │
│  Composable Screens  ←→  ViewModels        │
│  (Jetpack Compose)       (StateFlow)       │
└──────────────────┬─────────────────────────┘
                   │  ViewModel calls repo
┌──────────────────▼─────────────────────────┐
│           DOMAIN LAYER                      │
│  Use Cases / Models / Repository Interface │
└──────────────────┬─────────────────────────┘
                   │  Repo impl
┌──────────────────▼─────────────────────────┐
│            DATA LAYER                       │
│  Repository Impl  →  JSON File I/O         │
│  (Kotlinx Serialization / Gson)            │
└────────────────────────────────────────────┘
```

**Decision Rationale:**
- **Why Clean Architecture?** The app has clear separation between UI state, business logic, and JSON persistence. Three layers prevent coupling the Compose UI to file I/O.
- **Why UDF?** ViewModels expose `StateFlow<UiState>`, screens collect and render. Events flow up via lambda callbacks. Predictable, testable, Compose-native.
- **Why not Hexagonal?** Overkill. The only I/O is JSON files and user input. No external APIs, no databases, no network. Ports/adapters add ceremony without benefit here.
- **Why not MVI?** MVI introduces Intents and Reducers that add boilerplate for a CRUD-heavy local app. UDF with ViewModel + StateFlow is lighter and equally testable.

### 1.2 Scalability Forecast

| Concern | Risk | Mitigation |
|---------|------|------------|
| Large JSON file (1000+ items) | UI thread blocked on read | Coroutine dispatchers (`Dispatchers.IO`), read once into memory at app start, incremental writes |
| Many trips/lists | File size grows linearly | JSON remains human-readable for reasonable use (< 200 trips). If needed, migrate to Room/SQLite later — repository interface makes this a one-file swap |
| Swipe performance (Tinder mode) | Recompositions on fast swipes | `remember` + `derivedStateOf`, keyed LazyColumn items, avoid allocating new lists on each swipe |
| Concurrent writes | Race condition on JSON file | Single `Mutex` in repository. All writes serialize through one coroutine |

**Verdict:** JSON files are the right choice for v1. Room/SQLite adds schema migration complexity and SQL knowledge requirement. JSON files are debuggable, portable, and sufficient for single-user offline use. If the user ever needs cloud sync or 10,000+ items, swap the repository implementation.

---

## 2. Data Model (JSON Schema)

### 2.1 Root Document (`packly_data.json`)

```json
{
  "version": 1,
  "categories": [],
  "items": [],
  "lists": [],
  "trips": []
}
```

All data lives in a single JSON file at `context.filesDir/packly_data.json`.

### 2.2 Categories

```kotlin
@Serializable
data class Category(
    val id: String,           // UUID
    val name: String,         // "Clothes", "Technology", etc.
    val iconName: String,     // Material Icons name: "checkroom", "devices", etc.
    val sortOrder: Int = 0    // Display ordering
)
```

**Default categories** (seeded on first launch):

| name | iconName | sortOrder |
|------|----------|-----------|
| Clothes | checkroom | 0 |
| Shoes | hiking | 1 |
| Accessories | watch | 2 |
| Technology | devices | 3 |
| Toiletries | sanitizer | 4 |
| Documents | description | 5 |
| Extra Layers | umbrella | 6 |
| Miscellaneous | more_horiz | 7 |

### 2.3 Items (Backlog)

```kotlin
@Serializable
data class Item(
    val id: String,           // UUID
    val name: String,         // "Passport", "Toothbrush", etc.
    val categoryId: String,   // FK to Category.id
    val iconName: String,     // Material Icons name
    val createdAt: Long       // epoch millis
)
```

**Default backlog items** (seeded on first launch):

| name | categoryId | iconName |
|------|-----------|----------|
| T-Shirt | clothes | checkroom |
| Jeans | clothes | checkroom |
| Socks | clothes | socks |
| Underwear | clothes | accessibility |
| Sweater | extra_layers | umbrella |
| Jacket | extra_layers | umbrella |
| Sneakers | shoes | hiking |
| Sandals | shoes | beach_access |
| Watch | accessories | watch |
| Sunglasses | accessories | dark_mode |
| Belt | accessories | more_horiz |
| Phone | technology | smartphone |
| Charger | technology | cable |
| Power Bank | technology | battery_charging_full |
| Headphones | technology | headphones |
| Laptop | technology | laptop |
| Toothbrush | toiletries | more_horiz |
| Toothpaste | toiletries | more_horiz |
| Shampoo | toiletries | water_drop |
| Deodorant | toiletries | air |
| Sunscreen | toiletries | wb_sunny |
| Passport | documents | description |
| Boarding Pass | documents | flight |
| Insurance Card | documents | health_and_safety |
| Scarf | extra_layers | more_horiz |
| Gloves | extra_layers | more_horiz |

### 2.4 ItemLists (Reusable Packing Lists)

```kotlin
@Serializable
data class ItemList(
    val id: String,           // UUID
    val name: String,         // "Beach Weekend", "Business Trip"
    val items: List<ListEntry> = emptyList(),
    val createdAt: Long       // epoch millis
)

@Serializable
data class ListEntry(
    val itemId: String,       // FK to Item.id
    val quantity: Int = 1,
    val checked: Boolean = false
)
```

### 2.5 Trips

```kotlin
@Serializable
data class Trip(
    val id: String,           // UUID
    val name: String,         // "Summer Greece 2026"
    val date: String,         // "2026-07-15" (ISO date)
    val listId: String?,      // nullable FK to ItemList.id (source)
    val items: List<TripEntry> = emptyList(),
    val notes: String = "",
    val createdAt: Long       // epoch millis
)

@Serializable
data class TripEntry(
    val itemId: String,       // FK to Item.id
    val quantity: Int = 1,
    val packed: Boolean = false
)
```

**Relationship diagram:**

```
Category 1──N Item
Item     M──N ItemList (via ListEntry)
ItemList 1──1 Trip      (materialized from)
Trip      1──N TripEntry
Item      M──N Trip      (via TripEntry)
```

---

## 3. Package Structure

```
com.packly.app/
│
├── PacklyApplication.kt          // Application class
├── MainActivity.kt                // Single Activity, hosts NavHost
│
├── data/                          // DATA LAYER
│   ├── model/
│   │   ├── Category.kt
│   │   ├── Item.kt
│   │   ├── ListEntry.kt
│   │   ├── ItemList.kt
│   │   ├── TripEntry.kt
│   │   ├── Trip.kt
│   │   └── PacklyData.kt         // Root container
│   │
│   ├── repository/
│   │   ├── PacklyRepository.kt   // Interface
│   │   └── JsonPacklyRepository.kt // Implementation
│   │
│   └── seed/
│       └── DefaultData.kt        // Default categories + items
│
├── domain/                        // DOMAIN LAYER
│   ├── usecase/
│   │   ├── GetCategoriesUseCase.kt
│   │   ├── ManageItemsUseCase.kt
│   │   ├── ManageListsUseCase.kt
│   │   └── ManageTripsUseCase.kt
│   └── model/                     // Domain models (if different from data)
│
├── ui/                            // UI LAYER
│   ├── navigation/
│   │   ├── NavRoutes.kt           // Route constants
│   │   └── NavGraph.kt            // NavHost definition
│   │
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Type.kt
│   │   ├── Theme.kt
│   │   └── Shape.kt
│   │
│   ├── components/                // Shared composables
│   │   ├── PacklyTopBar.kt
│   │   ├── ItemCard.kt
│   │   ├── CategoryChip.kt
│   │   ├── EmptyState.kt
│   │   └── ConfirmDialog.kt
│   │
│   ├── screen/
│   │   ├── home/
│   │   │   ├── HomeScreen.kt
│   │   │   └── HomeViewModel.kt
│   │   │
│   │   ├── items/
│   │   │   ├── ItemsScreen.kt
│   │   │   ├── ItemsViewModel.kt
│   │   │   └── AddItemDialog.kt
│   │   │
│   │   ├── lists/
│   │   │   ├── ListsScreen.kt
│   │   │   ├── ListsViewModel.kt
│   │   │   ├── ListDetailScreen.kt
│   │   │   ├── ListDetailViewModel.kt
│   │   │   ├── ListModeScreen.kt        // Checkbox mode
│   │   │   └── TinderModeScreen.kt      // Swipe mode
│   │   │
│   │   └── trips/
│   │       ├── TripsScreen.kt
│   │       ├── TripsViewModel.kt
│   │       ├── TripDetailScreen.kt
│   │       └── TripDetailViewModel.kt
│   │
│   └── util/
│       ├── DateFormatter.kt
│       └── IconMapper.kt
│
└── di/                            // Dependency Injection
    └── AppModule.kt               // Manual DI (or Hilt module)
```

---

## 4. Navigation

### 4.1 Route Definitions

```kotlin
// com.packly.app.ui.navigation.NavRoutes.kt
object NavRoutes {
    const val HOME = "home"
    const val ITEMS = "items"
    const val LISTS = "lists"
    const val LIST_DETAIL = "list_detail/{listId}"
    const val LIST_MODE = "list_mode/{listId}"           // Checkbox view
    const val TINDER_MODE = "tinder_mode/{listId}"        // Swipe view
    const val TRIPS = "trips"
    const val TRIP_DETAIL = "trip_detail/{tripId}"

    // Helper functions for parameterized routes
    fun listDetail(listId: String) = "list_detail/$listId"
    fun listMode(listId: String) = "list_mode/$listId"
    fun tinderMode(listId: String) = "tinder_mode/$listId"
    fun tripDetail(tripId: String) = "trip_detail/$tripId"
}
```

### 4.2 Navigation Graph

```
Home ─────────────────────────────────────────────┐
  │                                                │
  ├── "Edit Items" ──────► Items (backlog CRUD)    │
  │                          └── AddItemDialog      │
  │                                                │
  ├── "Items Lists" ──────► Lists (overview)       │
  │                          │                      │
  │                          ├── Create/Edit List   │
  │                          ├── ListDetail         │
  │                          │   ├── "List Mode" ──► ListModeScreen
  │                          │   └── "Tinder Mode" ─► TinderModeScreen
  │                          └── Delete List        │
  │                                                │
  └── "Trips" ─────────────► Trips (overview)      │
                               │                    │
                               ├── Create Trip       │
                               │   └── Select List   │
                               │   └── Fine-tune     │
                               ├── TripDetail        │
                               └── Delete Trip       │
```

### 4.3 Navigation Graph Code Structure

```kotlin
// NavGraph.kt
@Composable
fun PacklyNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = NavRoutes.HOME) {

        composable(NavRoutes.HOME) {
            HomeScreen(
                onNavigateToItems = { navController.navigate(NavRoutes.ITEMS) },
                onNavigateToLists = { navController.navigate(NavRoutes.LISTS) },
                onNavigateToTrips = { navController.navigate(NavRoutes.TRIPS) }
            )
        }

        composable(NavRoutes.ITEMS) {
            ItemsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(NavRoutes.LISTS) {
            ListsScreen(
                onNavigateToListDetail = { id -> navController.navigate(NavRoutes.listDetail(id)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.LIST_DETAIL,
            arguments = listOf(navArgument("listId") { type = NavType.StringType })
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getString("listId") ?: return@composable
            ListDetailScreen(
                listId = listId,
                onNavigateToListMode = { navController.navigate(NavRoutes.listMode(listId)) },
                onNavigateToTinderMode = { navController.navigate(NavRoutes.tinderMode(listId)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.LIST_MODE,
            arguments = listOf(navArgument("listId") { type = NavType.StringType })
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getString("listId") ?: return@composable
            ListModeScreen(listId = listId, onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = NavRoutes.TINDER_MODE,
            arguments = listOf(navArgument("listId") { type = NavType.StringType })
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getString("listId") ?: return@composable
            TinderModeScreen(listId = listId, onNavigateBack = { navController.popBackStack() })
        }

        composable(NavRoutes.TRIPS) {
            TripsScreen(
                onNavigateToTripDetail = { id -> navController.navigate(NavRoutes.tripDetail(id)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.TRIP_DETAIL,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            TripDetailScreen(tripId = tripId, onNavigateBack = { navController.popBackStack() })
        }
    }
}
```

---

## 5. Component Tree per Screen

### 5.1 HomeScreen

```
HomeScreen
├── PacklyTopBar(title = "Packly")
├── Column
│   ├── AppLogo / Hero section
│   ├── LargeButton("Edit Items", onClick → ITEMS)
│   ├── LargeButton("Item Lists", onClick → LISTS)
│   └── LargeButton("Trips", onClick → TRIPS)
```

**States:** Idle (always loaded, no loading state needed — data loaded in MainActivity).

### 5.2 ItemsScreen (Backlog)

```
ItemsScreen
├── PacklyTopBar(title = "Items", onBack)
├── CategoryFilterRow (horizontal scrollable chips)
│   └── CategoryChip (× N categories, one selected)
├── LazyVerticalGrid / LazyColumn
│   └── ItemCard (× N filtered items)
│       ├── Icon (Material Icons)
│       ├── Text (item name)
│       └── DeleteIconButton
└── FloatingActionButton("+", onClick → show AddItemDialog)
    └── AddItemDialog
        ├── TextField (name)
        ├── CategoryDropdown
        ├── IconPicker (optional grid of common icons)
        ├── Button("Cancel")
        └── Button("Add")
```

**States:**
- Loaded (normal grid)
- Empty ("No items yet. Tap + to add one.")
- Filtered empty ("No items in this category.")

### 5.3 ListsScreen (Lists Overview)

```
ListsScreen
├── PacklyTopBar(title = "Lists", onBack)
├── LazyColumn
│   └── ListCard (× N lists)
│       ├── Text (list name)
│       ├── Text (item count, e.g. "12 items")
│       ├── IconButton(edit)
│       └── IconButton(delete → ConfirmDialog)
├── FloatingActionButton("+", onClick → CreateListDialog)
│   └── CreateListDialog
│       ├── TextField (name)
│       ├── LazyColumn (backlog items with checkboxes to include)
│       ├── Button("Cancel")
│       └── Button("Create")
└── EditListDialog (reuse CreateListDialog with pre-filled data)
```

**States:** Loaded, Empty ("No lists yet. Create your first packing list.").

### 5.4 ListDetailScreen (after selecting a list)

```
ListDetailScreen
├── PacklyTopBar(title = list.name, onBack)
├── Column
│   ├── Text(list.name, large)
│   ├── Text("${list.items.size} items")
│   ├── Button("List Mode (Checkboxes)", onClick → LIST_MODE)
│   └── Button("Tinder Mode (Swipe)", onClick → TINDER_MODE)
```

**States:** Loaded, Empty ("This list has no items. Edit the list to add some.").

### 5.5 ListModeScreen (Checkbox Packing)

```
ListModeScreen
├── PacklyTopBar(title = "Pack: ${list.name}", onBack)
├── LazyColumn
│   └── PackingCheckItem (× N entries)
│       ├── Checkbox(checked = entry.checked)
│       ├── Icon (item icon)
│       ├── Text(item.name)
│       └── Text("×${entry.quantity}") if quantity > 1
└── BottomBar
    └── Text("${checkedCount} / ${totalCount} packed")
```

**States:** Loaded, Empty, AllPacked (confetti / celebration state).

### 5.6 TinderModeScreen (Swipe Packing)

```
TinderModeScreen
├── PacklyTopBar(title = "Pack: ${list.name}", onBack)
├── Box (card stack container)
│   ├── SwipeCard (current item, draggable)
│   │   ├── Icon (large, centered)
│   │   ├── Text(item.name, large)
│   │   ├── Text("×${quantity}") if quantity > 1
│   │   └── Swipe overlay ("✓ Packed" / "✗ Skip")
│   └── Text("${pending} remaining")
├── Row (action buttons, alternative to swipe)
│   ├── IconButton("✗ Skip", onClick = swipeLeft)
│   └── IconButton("✓ Pack", onClick = swipeRight)
└── Text("All done! 🎉") when empty
```

**States:** Loaded, Empty, AllDone (celebration).

### 5.7 TripsScreen

```
TripsScreen
├── PacklyTopBar(title = "Trips", onBack)
├── LazyColumn
│   └── TripCard (× N trips)
│       ├── Text(trip.name)
│       ├── Text(trip.date)
│       ├── Text("${packedCount}/${totalCount} packed")
│       ├── LinearProgressIndicator(packedRatio)
│       ├── IconButton(delete → ConfirmDialog)
│       └── onClick → TRIP_DETAIL
└── FloatingActionButton("+", onClick → CreateTripDialog)
    └── CreateTripDialog
        ├── TextField (trip name)
        ├── DatePicker (trip date)
        ├── ListDropdown (select from existing lists)
        ├── TextField (notes, optional)
        ├── Button("Cancel")
        └── Button("Create")
```

**States:** Loaded, Empty ("No trips yet. Create one from a packing list.").

### 5.8 TripDetailScreen

```
TripDetailScreen
├── PacklyTopBar(title = trip.name, onBack)
├── Column
│   ├── Text(trip.date, subtitle)
│   ├── Text(trip.notes) if not empty
│   ├── Divider
│   ├── Text("Packing Progress", section header)
│   ├── LinearProgressIndicator(packedRatio)
│   ├── Text("${packedCount}/${totalCount} packed")
│   ├── LazyColumn
│   │   └── TripPackingItem (× N entries)
│   │       ├── Checkbox(packed = entry.packed)
│   │       ├── Icon (item icon)
│   │       ├── Text(item.name)
│   │       └── Text("×${entry.quantity}") if quantity > 1
│   └── Row
│       ├── Button("Add Items") → add from backlog
│       └── Button("Fine-tune") → adjust quantities, remove items
```

**States:** Loaded, Empty ("No items in this trip. Tap Add Items."), AllPacked.

---

## 6. State Management

### 6.1 UiState Pattern

Each ViewModel exposes a single `StateFlow<UiState>`:

```kotlin
// Example: ItemsViewModel
data class ItemsUiState(
    val items: List<Item> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null, // null = "All"
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false
)

class ItemsViewModel(
    private val manageItemsUseCase: ManageItemsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemsUiState())
    val uiState: StateFlow<ItemsUiState> = _uiState.asStateFlow()

    init { loadData() }

    fun onCategorySelected(categoryId: String?) { ... }
    fun onAddItem(name: String, categoryId: String, iconName: String) { ... }
    fun onDeleteItem(itemId: String) { ... }
    fun onShowAddDialog() { _uiState.update { it.copy(showAddDialog = true) } }
    fun onDismissAddDialog() { _uiState.update { it.copy(showAddDialog = false) } }
}
```

### 6.2 ViewModel Instantiation

Manual DI (no Hilt for v1 — keep it simple):

```kotlin
// AppModule.kt
object AppModule {
    private val repository: PacklyRepository by lazy {
        JsonPacklyRepository(context) // context injected from Application
    }

    fun provideItemsViewModel(): ItemsViewModel {
        return ItemsViewModel(ManageItemsUseCase(repository))
    }

    fun provideListsViewModel(): ListsViewModel {
        return ListsViewModel(ManageListsUseCase(repository))
    }

    fun provideTripsViewModel(): TripsViewModel {
        return TripsViewModel(ManageTripsUseCase(repository))
    }
}
```

If the project grows, swap to Hilt later. `AppModule` is the single seam.

---

## 7. Repository Pattern

### 7.1 Interface

```kotlin
// com.packly.app.data.repository.PacklyRepository.kt
interface PacklyRepository {
    // Categories
    fun getCategories(): Flow<List<Category>>

    // Items
    fun getItems(): Flow<List<Item>>
    suspend fun addItem(item: Item)
    suspend fun deleteItem(itemId: String)

    // Lists
    fun getLists(): Flow<List<ItemList>>
    fun getListById(listId: String): Flow<ItemList?>
    suspend fun createList(list: ItemList)
    suspend fun updateList(list: ItemList)
    suspend fun deleteList(listId: String)

    // Trips
    fun getTrips(): Flow<List<Trip>>
    fun getTripById(tripId: String): Flow<Trip?>
    suspend fun createTrip(trip: Trip)
    suspend fun updateTrip(trip: Trip)
    suspend fun deleteTrip(tripId: String)

    // Bulk
    suspend fun seedDefaultData()
}
```

### 7.2 Implementation

```kotlin
// com.packly.app.data.repository.JsonPacklyRepository.kt
class JsonPacklyRepository(
    private val context: Context
) : PacklyRepository {

    private val dataFile: File
        get() = File(context.filesDir, "packly_data.json")

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val writeMutex = Mutex()

    private fun readData(): PacklyData {
        if (!dataFile.exists()) return PacklyData()
        return json.decodeFromString(dataFile.readText())
    }

    private suspend fun writeData(data: PacklyData) {
        writeMutex.withLock {
            withContext(Dispatchers.IO) {
                dataFile.writeText(json.encodeToString(data))
            }
        }
    }

    // ... Flow-based reads return MutableStateFlow updated on each mutation
}
```

**Why `Flow` return types?** ViewModels collect flows reactively. When the user adds a list, the repository updates its internal `MutableStateFlow`, and all observing ViewModels recompose. No manual "reload" calls needed.

**Why `Mutex`?** All writes to the single JSON file must serialize. `Mutex.withLock` + `Dispatchers.IO` is the idiomatic Kotlin coroutine approach.

---

## 8. Dependencies

### 8.1 build.gradle.kts (app level)

```kotlin
dependencies {
    // Core AndroidX
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    // Jetpack Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    // Image loading (for icon resources if needed)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

### 8.2 build.gradle.kts (project level)

```kotlin
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.10" apply false
}
```

### 8.3 Dependency Rationale

| Dependency | Why | Alternative Considered |
|-----------|-----|----------------------|
| Material Icons Extended | Full icon set for categories and items | Material Icons (default) — too limited |
| Kotlinx Serialization | Compile-time safe, no reflection | Gson — runtime reflection, slower, less Kotlin-idiomatic |
| Coil | Icon loading if custom images are added later | Glide — heavier, less Compose-native |
| Manual DI (no Hilt) | Reduces build complexity for v1 | Hilt — adds annotation processing, Dagger learning curve |
| No Room/SQLite | JSON files sufficient for single-user, offline | Room — overkill for <1000 records, adds migration complexity |

---

## 9. Implementation Plan (Ordered Phases)

### Phase 1: Skeleton & Data Layer
1. Create Android project with Compose
2. Add all dependencies to `build.gradle.kts`
3. Implement data models (`Category`, `Item`, `ListEntry`, `ItemList`, `TripEntry`, `Trip`, `PacklyData`)
4. Implement `PacklyRepository` interface
5. Implement `JsonPacklyRepository` with JSON read/write
6. Implement `DefaultData.kt` (seed categories + default items)
7. Implement `AppModule.kt` (Manual DI)

### Phase 2: Navigation & Theme
8. Set up Material3 theme (`Color`, `Type`, `Theme`)
9. Define `NavRoutes` constants
10. Implement `NavGraph` with all routes
11. Wire `NavGraph` into `MainActivity`

### Phase 3: Home + Items Screens
12. `HomeScreen` composable + `HomeViewModel`
13. `ItemsScreen` composable + `ItemsViewModel`
14. `AddItemDialog` with name, category picker, icon picker
15. Shared components: `PacklyTopBar`, `ItemCard`, `CategoryChip`

### Phase 4: Lists Screens
16. `ListsScreen` (overview) + `ListsViewModel`
17. `CreateListDialog` / `EditListDialog`
18. `ListDetailScreen` with mode selection
19. `ListModeScreen` (checkbox packing)
20. `TinderModeScreen` (swipe packing)

### Phase 5: Trips Screens
21. `TripsScreen` (overview) + `TripsViewModel`
22. `CreateTripDialog` (materialize from list selector)
23. `TripDetailScreen` with packing progress + fine-tuning

### Phase 6: Polish & Edge Cases
24. `EmptyState` component for all screens
25. `ConfirmDialog` for delete actions
26. Swipe animation polish in `TinderModeScreen`
27. All-packed celebration states

---

## 10. ADRs (Architecture Decision Records)

### ADR-001: JSON File Storage over Room/SQLite
**Decision:** Use a single JSON file (`packly_data.json`) in app-private storage.
**Rationale:** Packly is single-user, offline, low-volume (<1000 records). JSON is human-readable, debuggable, requires no migrations, and the repository interface allows future swap to Room without changing UI code.
**Trade-off:** Slower reads at scale. Mitigated by reading once at startup into memory and using `Mutex` for serialized writes.

### ADR-002: Manual DI over Hilt
**Decision:** Use a manual `AppModule` singleton for dependency injection in v1.
**Rationale:** Hilt adds annotation processing, Dagger learning curve, and build time overhead. With ~5 ViewModels and one repository, manual DI is trivial (an object with lazy factory functions). If the app grows beyond ~20 injectable classes, migrate to Hilt — the seam is `AppModule`.
**Trade-off:** No compile-time dependency graph validation. Acceptable for v1 scope.

### ADR-003: Kotlinx Serialization over Gson
**Decision:** Use Kotlinx Serialization.
**Rationale:** Compile-time safe (no reflection), Kotlin-idiomatic (`@Serializable` data classes), faster than Gson, first-party JetBrains support. Requires the Kotlin serialization compiler plugin.
**Trade-off:** Requires `kotlin("plugin.serialization")` in build config. Marginal setup cost, zero runtime cost.

### ADR-004: UDF with StateFlow over MVI
**Decision:** Use ViewModel + `StateFlow<UiState>` pattern.
**Rationale:** MVI (Model-View-Intent) introduces Intents, Actions, Results, and Reducers — useful for complex async state machines. Packly is CRUD-heavy with simple state transitions. UDF with `StateFlow` + `update {}` is lighter, equally testable, and more familiar to Android developers.
**Trade-off:** Less formal state transition modeling. Acceptable given Packly's state complexity (low).

### ADR-005: Flow return types in Repository
**Decision:** Repository methods return `Flow<T>` backed by internal `MutableStateFlow`.
**Rationale:** Multiple screens may observe the same data (e.g., editing a list while viewing the trip that materialized from it). Flow-based reactive reads ensure all observers stay in sync without manual refresh calls.
**Trade-off:** Slightly more complex repository implementation vs. suspend fun returning snapshots. Worth it for consistency.

---

*Generated by Hermes Software Architect Agent for Packly project.*
