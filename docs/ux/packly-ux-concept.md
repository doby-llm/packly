# Packly UX Concept

## Product promise

Packly helps people leave home confident: open the app, choose a trip or list, see what still needs to be packed, and check items off without thinking too hard. The experience should feel calm, optimistic, and practical — like a friendly packing companion rather than a spreadsheet.

Design principles:

1. Clarity first: every screen answers one question: What can I pack, edit, or prepare next?
2. Soft color, strong meaning: use cheerful category colors as wayfinding, but keep surfaces quiet and readable.
3. Fast capture: adding an item, list, or trip should take seconds and never interrupt the broader flow.
4. Confidence loops: show progress, confirmations, undo, and clear empty/error states so users trust their changes.
5. Retheme-ready: use semantic tokens and Material 3 components so the app can change style without rewriting UX.

## Framework recommendation

Use Jetpack Compose with Material 3.

Rationale:

- The app is interaction-heavy but simple in information architecture; Compose makes state-driven screens, list filtering, chips, dialogs, and animated progress easy to maintain.
- Material 3 gives accessible defaults for typography, color roles, touch targets, motion, dynamic color, and component states.
- Compose supports a clean separation between domain state and UI state, which pairs well with JSON-style persistence for trips, lists, categories, and session progress.

Avoid custom views unless a later engineering spike proves a Compose component cannot meet performance or accessibility goals.

## App personality

Tone: upbeat, calm, and lightly playful.

Voice examples:

- Empty trip: “This trip is ready for a packing plan.”
- Completed packing mode: “Everything is packed. Safe travels!”
- Delete confirmation: “Remove ‘Phone charger’ from your items? Lists using it will keep their own copy until edited.”
- Offline/session persistence hint: “Saved on this device.”

Visual personality:

- Rounded cards and chips.
- Colorful category accents inspired by travel stickers and luggage tags.
- Plenty of whitespace and clear section headings.
- Small celebratory moments only when they confirm progress, never as blocking animations.

## Primary user flows

### 1. First launch and main-menu navigation

Goal: help a new user understand the model immediately: Items are reusable, Lists are templates, Trips are real packing sessions.

Flow:

1. App opens to Home / Main Menu.
2. Hero card says “Ready to pack?” with primary action “Start a trip”.
3. Three main destinations are visible as large cards:
   - Edit Items: reusable packing inventory.
   - Item Lists: reusable templates like “Weekend”, “Beach”, “Work trip”.
   - Trips: specific travel plans and packing progress.
4. Secondary action: “Browse sample list” if no user data exists.

Friction to avoid:

- Do not make users configure categories before seeing value.
- Do not hide core areas behind a hamburger menu on mobile-sized screens.
- Do not use technical labels like “database”, “schema”, or “entities”.

Recommended navigation:

- Use a simple Home screen with destination cards for the first release.
- If the app grows, add a Material 3 NavigationBar with Home, Lists, Trips, Items.
- Keep destructive and administrative actions inside each section, not in global navigation.

### 2. Edit Items

Goal: manage the reusable item library.

Screen layout:

- Top app bar: “Items” with search icon and overflow for import/export later.
- Category filter row: horizontally scrollable FilterChips with icons.
- Grouped list: category headers with icon, color stripe, item count, and collapsible content.
- Floating action button: “Add item”.

Item row content:

- Leading category icon or small colored dot.
- Item name.
- Optional metadata: “Usually 2”, “Carry-on”, “Toiletries”.
- Trailing edit affordance.

Add/Edit item form:

Fields:

- Name (required).
- Category (required, default to last used or suggested).
- Default quantity (optional, default 1).
- Notes (optional).
- Tags or contexts (optional later: beach, work, baby, winter).

Validation:

- Name cannot be empty.
- Duplicate warning: “You already have ‘Socks’. Add another anyway?” with Add anyway / Edit existing.
- Quantity must be positive.

Delete behavior:

- Use a confirmation dialog for permanent library deletion.
- Explain impact clearly: deleting from the item library should not surprise users if lists or trips reference it.
- Prefer soft deletion or detached copies in persisted data so historical trips remain intact.
- Provide Snackbar undo when safe.

### 3. Item Lists

Goal: create reusable packing templates.

Screen layout:

- Top app bar: “Lists”.
- Search/filter by name or category coverage.
- List cards showing:
  - List name.
  - Number of items.
  - Top 3 category icons.
  - Last updated.
  - Primary action: “Use for trip”.

List detail:

- Header card with name, description, item count, and actions: Rename, Duplicate, Delete.
- Grouped category sections with selectable item rows.
- Add items from library via bottom sheet with search and category filters.
- Inline quantity adjustment using stepper or compact text field.

Creation flow:

1. Tap Add list.
2. Choose “Start from sample”, “Start from existing”, or “Blank list”.
3. Name list.
4. Add items from library.
5. Save shows success Snackbar: “Weekend list saved.”

Useful defaults:

- Provide seed lists after first launch: Weekend, Beach day, Business trip, Camping, Family visit.
- Let users duplicate a seed list before editing if seeds are intended as reusable examples.

Delete confirmation:

- Dialog: “Delete ‘Beach day’ list?”
- Body: “Trips already created from this list won’t change.”
- Actions: Cancel / Delete.

### 4. Trips

Goal: turn a list into an active packing session with progress.

Trips screen layout:

- Upcoming/active trips first.
- Completed trips collapsed under “Past trips”.
- Trip cards show destination/name, date range if available, progress ring/bar, packed count, and next action.

Create trip flow:

1. Tap Start trip.
2. Enter trip name/destination.
3. Optional date range.
4. Pick a list template or start blank.
5. Review generated packing list grouped by category.
6. Save trip and enter trip detail.

Trip detail:

- Header: trip name, date, progress, “Start packing” button.
- Checklist preview grouped by category.
- Quick actions: Add one-off item, Add from library, Edit quantities, Reset packed state.

Persistence expectation:

- A trip should snapshot list items at creation time so later list edits do not unexpectedly change an active packing session.
- JSON-style persistence should keep:
  - Item library.
  - List templates.
  - Trip snapshots.
  - Per-trip packed/unpacked state.
  - App settings/theme.

### 5. Packing-mode UX

Goal: support focused, low-friction checking while physically packing.

Entry points:

- Trip detail primary button: “Start packing”.
- Trip card action: “Continue packing”.

Packing mode layout:

- Prominent progress at top: “12 of 31 packed”.
- Search/filter chips: All, Unpacked, Packed, category chips.
- Large touch-friendly checklist rows grouped by category.
- One-handed interaction: checkbox/tap target on the leading side, item text center, quantity/notes secondary.
- Sticky bottom action: “Finish packing” when all required items are checked.

Row behavior:

- Tap row toggles packed state.
- Long press or overflow opens item notes/actions.
- Packed item moves to lower opacity or to a “Packed” collapsed group depending on the active filter.
- Snackbar undo after accidental toggle: “Marked packed. Undo”.

Completion:

- When all required items are packed, show a gentle success state:
  - “Everything is packed.”
  - Actions: Review packed items, Finish trip, Back to home.
- Avoid confetti every time; reserve a subtle animated check or progress glow.

## Information architecture

Recommended top-level model:

- Home: overview and primary shortcuts.
- Items: reusable library.
- Lists: reusable templates.
- Trips: active and historical packing sessions.

Hierarchy rules:

- Items are the source vocabulary.
- Lists are reusable combinations of items.
- Trips are snapshots with status.
- Packing mode is not a separate destination; it is a focused state inside a trip.

This hierarchy should be visible in copy and screen structure so users do not wonder whether editing a list changes a trip.

## Grouped category presentation

Categories should create visual memory and reduce scanning effort.

Default categories:

1. Clothing
2. Toiletries
3. Electronics
4. Documents
5. Health
6. Travel Comfort
7. Weather
8. Kids / Family
9. Food / Snacks
10. Miscellaneous

Presentation pattern:

- Category header has icon, label, count, and optional progress within trips.
- Use a soft-tinted container or left accent rail rather than fully saturated backgrounds.
- In packing mode, show category progress: “Clothing 5/8”.
- Allow collapsed sections, remembering state per screen session.

## Default seed items

Seed 24 reusable items so the first run feels useful but not overwhelming:

Clothing:

- T-shirts
- Underwear
- Socks
- Pants / shorts
- Sweater or hoodie
- Sleepwear
- Comfortable shoes

Toiletries:

- Toothbrush
- Toothpaste
- Shampoo
- Deodorant
- Hairbrush / comb

Electronics:

- Phone charger
- Power bank
- Headphones
- Laptop charger
- Travel adapter

Documents:

- Passport / ID
- Tickets / boarding pass
- Wallet
- Travel insurance card

Health:

- Medication
- Sunscreen
- First-aid basics

Travel Comfort:

- Water bottle
- Book / e-reader
- Neck pillow

Each seed item should have a category icon, default quantity, and plain-language name.

## Forms and confirmations

General form rules:

- Use Material 3 TextField, ExposedDropdownMenuBox for categories, AssistChips for tags/contexts, and clear primary/secondary actions.
- Put Save/Create as the positive action; Cancel/Back should warn only when there are unsaved changes.
- Keep add forms in a modal bottom sheet for quick creation; use full screen for complex list/trip editing.
- Autofocus the first meaningful field, but do not pop the keyboard unexpectedly after navigation.

Confirmation rules:

- Confirm destructive actions: delete item, delete list, delete trip, reset packing state.
- Do not confirm reversible low-risk actions; use Snackbar undo instead.
- Copy must name the affected object and explain consequences.

## Empty states

Home with no data:

- Title: “Your packing workspace is ready.”
- Body: “Start with sample items or create your first trip.”
- Actions: Start a trip, Browse sample lists.

Items empty:

- Title: “No items yet.”
- Body: “Add reusable items like chargers, socks, and toiletries.”
- Action: Add item.

Lists empty:

- Title: “No packing lists yet.”
- Body: “Create a reusable list for weekends, work trips, or holidays.”
- Actions: Create list, Use sample list.

Trips empty:

- Title: “No trips planned.”
- Body: “Turn a packing list into a trip checklist when you know where you’re going.”
- Action: Start trip.

Search empty:

- Title: “No matches for ‘query’.”
- Body: “Try another word or clear filters.”
- Action: Clear filters.

Packing complete empty/unpacked filter:

- Title: “No unpacked items.”
- Body: “Everything in this view is packed.”
- Action: Show packed items.

## Loading, error, success states

Loading:

- Prefer skeleton rows for lists and trips.
- Use small inline CircularProgressIndicator for save actions.
- Disable only the action currently saving, not the whole screen unless required.

Error:

- Show human-readable errors: “Couldn’t save the trip. Try again.”
- Preserve user-entered form data after failure.
- Offer retry for load errors.
- Use AlertDialog only for blocking errors; otherwise use inline messages or Snackbar.

Success:

- Use Snackbar for create/update/delete with Undo where possible.
- Use a success illustration or check state only for complete trip/packing milestones.

## Micro-interactions

Polish that should remain performant:

- Category chips animate selected state with Material default motion.
- Progress ring/bar smoothly updates when checking items.
- Checkbox state uses a quick scale/fade, not a long animation.
- Drag/reorder can be deferred; if implemented, use haptic feedback and clear handles.
- FAB morphs or expands only on screens where it improves recognition.
- Collapse/expand category sections with short 150–200 ms animation.

Avoid:

- Heavy particle effects.
- Auto-scrolling packed items while the user is tapping rapidly.
- Full-screen success animation for every checked item.

## Accessibility and inclusivity

WCAG and Android accessibility goals:

- Minimum contrast: 4.5:1 for normal text, 3:1 for large text and meaningful icons.
- Touch targets: at least 48 x 48 dp.
- Support Dynamic Type / font scaling without clipped text.
- All icons need content descriptions unless decorative.
- Checklist rows should announce state, quantity, and category: “Phone charger, Electronics, quantity 1, not packed.”
- Color cannot be the only category indicator; pair with icon and label.
- Maintain logical TalkBack order: screen title, progress, filters, list content, primary action.
- Provide visible focus states for keyboard/D-pad users.
- Do not rely on swipe-only gestures for core actions.
- Respect reduced motion by shortening or disabling decorative transitions.

Responsive behavior:

- Phone portrait: single-column cards and full-width list rows.
- Phone landscape / foldables: use two panes where useful: list of trips on left, detail on right.
- Tablets: NavigationRail plus content pane; avoid stretching checklist rows beyond readable widths.

## Key UX risks

1. Users may not understand whether changing a list affects existing trips.
   - Mitigation: use snapshot language in copy and clear confirmation text.
2. Category color overload could reduce readability.
   - Mitigation: keep color as accent, not background, and test contrast.
3. Adding items from library could become slow with many items.
   - Mitigation: search, filters, recent items, and category grouping from the start.
4. Accidental packing toggles are likely during real-world packing.
   - Mitigation: large touch targets, undo Snackbar, clear packed/unpacked filters.

## Recommended MVP screen set

1. Home / Main Menu
2. Items list
3. Add/Edit item bottom sheet
4. Lists overview
5. List detail editor
6. Create trip flow
7. Trips overview
8. Trip detail
9. Packing mode
10. Shared empty/error/success components

This set gives engineering a concrete Compose surface map while keeping the first release focused.
