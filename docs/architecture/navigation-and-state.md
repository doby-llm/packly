# Packly Navigation and State Blueprint

## Navigation architecture

Use one Activity with Compose navigation.

Top-level destinations:

- Home
- Items
- Lists
- Trips

Nested/detail destinations:

- Item edit sheet or route
- List detail/editor
- Create trip flow
- Trip detail
- Packing mode inside a trip route

Packing mode is not a separate product area; it is a focused state inside a trip.

## Route contract

Define typed route objects in navigation/ and keep raw route strings private to the navigation layer.

```kotlin
sealed interface PacklyRoute {
    data object Home : PacklyRoute
    data object Items : PacklyRoute
    data object Lists : PacklyRoute
    data object Trips : PacklyRoute
    data class ListDetail(val listId: ListId) : PacklyRoute
    data class TripDetail(val tripId: TripId) : PacklyRoute
    data class PackingMode(val tripId: TripId) : PacklyRoute
}
```

Rules:

- Pass IDs through routes, not whole objects.
- ViewModels load state from repositories by ID.
- Missing IDs show a recoverable error state with a path back to the overview.
- Destructive actions stay inside the relevant screen.

## Screen state pattern

Each feature owns:

- A screen composable that renders state.
- A ViewModel that exposes StateFlow<ScreenState>.
- An event contract for user actions.
- Optional one-shot effects for snackbars/navigation.

```kotlin
data class ItemsScreenState(
    val isLoading: Boolean = true,
    val query: String = "",
    val selectedCategoryKey: String? = null,
    val sections: List<CategorySectionUiState> = emptyList(),
    val emptyState: EmptyState? = null,
    val error: PacklyUiError? = null
)

sealed interface ItemsEvent {
    data class SearchChanged(val query: String) : ItemsEvent
    data class CategorySelected(val categoryKey: String?) : ItemsEvent
    data object AddItemClicked : ItemsEvent
    data class EditItemClicked(val itemId: ItemId) : ItemsEvent
    data class DeleteItemRequested(val itemId: ItemId) : ItemsEvent
}
```

## Feature boundaries

### Home

Responsibilities:

- Show product hierarchy and starter actions.
- Surface counts/progress summary from repositories.
- Navigate to Items, Lists, Trips, or Create Trip.

Home should not edit domain entities directly.

### Items

Responsibilities:

- Search/filter item library.
- Group by category.
- Add/edit/archive items.
- Warn on duplicates.

Items ViewModel calls item use cases and exposes grouped UI state.

### Lists

Responsibilities:

- Show list cards.
- Create/rename/duplicate/archive lists.
- Add/remove list entries from the item library.
- Keep list entries as templates with snapshots.

Lists must not modify active trip entries after trip creation.

### Trips

Responsibilities:

- Show active/upcoming first and completed/past collapsed.
- Create a trip from a list snapshot or blank start.
- Edit trip metadata.
- Reset packed state with confirmation.

Trip creation is an explicit use case that snapshots list entries.

### Packing

Responsibilities:

- Show focused checklist with progress.
- Toggle packed state quickly and safely.
- Filter All/Unpacked/Packed/category.
- Provide Snackbar undo for accidental toggles.
- Show completion panel when all entries are packed.

Packing mode must optimize for repeated taps: no disruptive auto-scroll, stable row keys, and quick state updates.

## StateFlow and concurrency

Recommended flow:

1. Repository exposes Flow<PacklyAppDocument> or feature-specific flows.
2. Use cases transform domain data into results.
3. ViewModels combine flows with local UI filters, collapsed groups, and form drafts.
4. Compose collects ViewModel state with lifecycle awareness.

Concurrency rules:

- Repository writes should be serialized with a Mutex or DataStore transactional updates.
- Optimistic UI is acceptable for toggling packed state if failure restores state and shows a snackbar/error.
- Form saves disable only the save action while writing, not the entire screen.
- Use viewModelScope for UI-triggered work.
- Move expensive filtering/grouping to Dispatchers.Default if lists grow large.

## One-shot effects

Use one-shot effects for:

- Snackbars: saved, deleted with undo, packed/unpacked undo.
- Navigation after create/save.
- Opening confirmation dialogs if not represented as regular state.

Do not encode one-shot effects as permanent boolean fields in screen state; that causes duplicate snackbars after recomposition or process recreation.

## Responsive layout

Compact width:

- Home destination cards.
- Single-column lists.
- FAB for primary creation.
- Full-screen editors for complex list/trip flows.

Medium/expanded width:

- Add NavigationRail only when persistent top-level navigation becomes useful.
- Use overview/detail layouts for Trips and Lists.
- Keep forms max-width around 560-640 dp.
- Keep checklists around 720 dp max width for readability.

## Performance for grouped lists/checklists

Use Compose list best practices immediately:

- LazyColumn with stable keys: category key for headers and entry IDs for rows.
- Derive grouped UI state outside row composables.
- Use immutable lists/state objects so row recomposition is predictable.
- Keep row composables small and stateless.
- Use remember/derivedStateOf only for local UI calculations, not domain ownership.
- Avoid full-screen success animations and heavy particle effects.
- Collapse/expand category sections with 150-200 ms animation.
- Do not auto-scroll packed items while the user is tapping quickly.

## Accessibility state requirements

- Each screen has one title.
- Checklist rows expose item name, category, quantity, and packed/unpacked state.
- Category icons are decorative when a label is adjacent; otherwise include a content description.
- Progress is exposed as text, not only a visual bar.
- Touch targets are at least 48 dp.
- Color is never the only category signal.
