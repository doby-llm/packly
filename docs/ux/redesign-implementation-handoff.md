# Packly redesign implementation handoff

Task: t_4b81767b
Role: UX / visual specification owner
Project root: /home/mflova/packly

Authoritative sources audited:

- docs/ux/design.md
- docs/ux/stitch_packly_mobile_app_design/vibrant_minimalism/DESIGN.md
- docs/ux/logo.png
- docs/ux/stitch_packly_mobile_app_design/packly_home_vibrant/screen.png
- docs/ux/stitch_packly_mobile_app_design/packly_items_library_vibrant/screen.png
- docs/ux/stitch_packly_mobile_app_design/packly_items_library_catalog/screen.png
- docs/ux/stitch_packly_mobile_app_design/packly_list_templates_vibrant/screen.png
- docs/ux/stitch_packly_mobile_app_design/packly_packing_mode_vibrant/screen.png
- docs/ux/stitch_packly_mobile_app_design/packly_packing_mode_quantities/screen.png
- HTML references available for Items, Lists, Packing mode, Packing quantities, and Items catalog.

Validation note: the DESIGN.md lint workflow could not be run locally because `npx` is not installed on this host. Do not treat that as design invalidity; the token values below are copied from the source files and HTML references.

## 1. Product direction

Packly should feel like a focused, optimistic packing companion: clean white space, high-saturation mint/lavender/sky accents, rounded cards, quick checklist interactions, and minimal cognitive overhead. The redesign is not a decorative skin. It changes the app's information hierarchy around four primary destinations:

1. Home: orientation and active trip summary.
2. Items: reusable item library grouped by category.
3. Lists: reusable packing templates.
4. Trips: trip management and entry point into focused packing mode.

Packing mode is a focused task route inside a trip, not a top-level tab.

Recommendation: fresh UI-layer rewrite over incremental patching.

Reasoning:

- Existing theme currently uses older Ocean/Mint/Coral colors and default Material typography, while the redesign requires a fully different Material-like token set, Plus Jakarta Sans, new category accents, new card structures, bottom navigation, FAB behavior, sticky packing headers, and snackbars.
- Existing components can preserve domain behavior, routes, ViewModels, and repository state, but visual components should be rebuilt around the new token system to avoid mixing old and new styles.
- Keep model, repository, navigation contract, and business flows. Rewrite `ui/theme`, `ui/token`, shared components, and feature screen composables coherently in Compose.

## 2. Exact visual tokens to implement

Use these tokens exactly. Do not invent replacement colors.

### Colors

Core surfaces:

- surface: #f8f9fa
- background: #f8f9fa
- surface-bright: #f8f9fa
- surface-dim: #d9dadb
- surface-container-lowest: #ffffff
- surface-container-low: #f3f4f5
- surface-container: #edeeef
- surface-container-high: #e7e8e9
- surface-container-highest: #e1e3e4
- surface-variant: #e1e3e4

Text and outlines:

- on-surface: #191c1d
- on-background: #191c1d
- on-surface-variant: #3b4a44
- outline: #6b7b74
- outline-variant: #b9cac3
- inverse-surface: #2e3132
- inverse-on-surface: #f0f1f2

Primary / mint:

- primary: #006b57
- surface-tint: #006b57
- on-primary: #ffffff
- primary-container: #00e5bc
- on-primary-container: #00614f
- primary-fixed: #42fdd3
- primary-fixed-dim: #00e0b8
- on-primary-fixed: #002019
- on-primary-fixed-variant: #005141
- inverse-primary: #00e0b8

Secondary / lavender:

- secondary: #6844c7
- on-secondary: #ffffff
- secondary-container: #9c7afe
- on-secondary-container: #310082
- secondary-fixed: #e8ddff
- secondary-fixed-dim: #cebdff
- on-secondary-fixed: #21005e
- on-secondary-fixed-variant: #5028ae

Tertiary / sky:

- tertiary: #006591
- on-tertiary: #ffffff
- tertiary-container: #94d2ff
- on-tertiary-container: #005b84
- tertiary-fixed: #c9e6ff
- tertiary-fixed-dim: #89ceff
- on-tertiary-fixed: #001e2f
- on-tertiary-fixed-variant: #004c6e

Error:

- error: #ba1a1a
- on-error: #ffffff
- error-container: #ffdad6
- on-error-container: #93000a

Important usage:

- Main brand title and primary actions use primary #006b57.
- Large saturated checklist progress in packing mode uses secondary-container #9c7afe.
- Packed check circles in packing mode use secondary-container #9c7afe.
- FAB background uses primary #006b57 and white icon/text.
- Duplicate/conflict warning uses error #ba1a1a.
- Do not use color as the only signal: pair accents with icons, labels, progress text, or count text.

### Typography

Font family for all app text: Plus Jakarta Sans.

Load as app font resources or Google Fonts Compose provider if project already supports it. Use these roles:

- display-lg: 48sp, weight 800, line height 56sp, letter spacing -0.02em.
- headline-lg: 32sp, weight 700, line height 40sp, letter spacing -0.01em.
- headline-lg-mobile: 28sp, weight 700, line height 36sp.
- title-md: 20sp, weight 600, line height 28sp.
- body-lg: 16sp, weight 400, line height 24sp.
- body-sm: 14sp, weight 400, line height 20sp.
- label-md: 12sp, weight 600, line height 16sp, letter spacing 0.05em.

Observed usage:

- App brand `Packly`: headline-lg-mobile, weight 700, primary color.
- Home hero `Good Morning.`: display-like weight 800, approximately 40-48sp on compact, on-surface.
- Screen titles like `My Lists`, `Paris Trip`: headline-lg-mobile, on-surface.
- Card titles/category titles: title-md.
- Row labels/search text/body copy: body-lg.
- Metadata, chips, action labels, progress label: label-md or body-sm depending density.
- Progress label `PACKING PROGRESS` is uppercase label-md.

### Spacing rhythm

Base rhythm: 8dp.

Token mapping:

- xs: 4dp
- base: 8dp
- sm: 12dp
- md: 24dp
- lg: 48dp
- xl: 80dp
- gutter: 24dp
- margin-mobile: 16dp
- margin-desktop: 40dp

Mobile layout rules:

- Horizontal screen padding: 16dp.
- Top app bar height: 64dp.
- Bottom navigation height: 80dp including bottom padding.
- Main content often starts with 24dp top padding below top bar.
- Section/card vertical gaps: 24dp to 48dp depending hierarchy.
- Item rows: minimum 48dp touch target; packing rows use 16dp padding and 48dp+ height.
- Checklist/category large gaps: 48-80dp between category groups in focused packing mode.

### Shapes

From DESIGN.md:

- Small components: 8dp radius.
- Cards/containers: 16dp radius.
- Large sections/modals: 24dp radius.
- Full/pill chips: 9999dp.

Observed HTML/screen details:

- Home trip cards: 12-16dp radius, white surface.
- Items category cards: 24dp radius.
- Lists cards: 12dp radius in HTML, visually around 12-16dp; prefer 16dp for system consistency unless exact parity requires 12dp.
- FAB: 20dp radius for large 64dp items FAB; 12dp radius for 56dp lists FAB. Prefer 20dp on the item-library screen and 16dp/20dp elsewhere if no screenshot contradicts.
- Bottom nav active tab: 12dp radius.
- Snackbar: 8dp radius.

### Elevation and shadows

Use soft ambient shadows, never heavy grey Material elevation.

Approximate Compose shadows:

- Level 0: no shadow, background #f8f9fa.
- Level 1 cards: white #ffffff with border surface-container-high or surface-container-low and very soft shadow equivalent to `0 4dp 20dp rgba(0,0,0,0.04)`.
- List/checklist rows card: shadow equivalent to `0 2dp 12dp rgba(0,0,0,0.02)`.
- FAB: primary tinted shadow equivalent to `0 8dp 24dp rgba(0,107,87,0.30)`.
- Snackbar: `0 8dp 24dp rgba(0,0,0,0.12)`.
- Sticky packing header: subtle shadow `0 4dp 20dp rgba(0,0,0,0.02)`.

Compose implementation can approximate with `shadow(elevation, shape, ambientColor, spotColor)` plus a 1dp border. If tinted shadows are hard on ARM validation, prioritize correct color/shape/border over perfect shadow.

### Components

Top app bar:

- Height: 64dp.
- Background: surface #f8f9fa.
- Content horizontally padded 16dp.
- Brand text: centered on Home/Items/Packing or left-aligned with avatar on Lists reference; choose centered brand for Home, Items, Packing, Trips for consistency unless an existing screen pattern needs left brand.
- Leading: avatar on top-level screens; back arrow on nested/focused screens.
- Trailing: settings icon.
- Icons: on-surface-variant #3b4a44, 48dp touch target.
- Avatar: 32dp circle, outline-variant border when available.

Bottom navigation:

- Destinations: Home, Items, Lists, Trips.
- Height: 80dp, fixed bottom on compact screens.
- Background: surface-container #edeeef.
- Top corners: 12-16dp radius.
- Shadow: `0 -4dp 20dp rgba(0,0,0,0.04)`.
- Each item min 64dp wide and 48dp high.
- Inactive color: on-surface-variant #3b4a44.
- Active container: secondary-container #9c7afe.
- Active content: on-secondary-container #310082.
- Active icon filled where possible; inactive outlined.
- Labels: label-md. Active label may be weight 700.

FAB:

- Use for create/add on Items and Lists/Trips where applicable.
- Size: 64dp on Items screen, 56dp acceptable on Lists screen; choose 64dp if standardizing.
- Background: primary #006b57.
- Content: on-primary #ffffff.
- Shape: rounded 20dp.
- Position: bottom end, 16dp from edge, above bottom nav (about 96-100dp from bottom).
- Pressed: scale 0.95. Hover/desktop: scale 1.05.
- Content description must state action, e.g. `Add item`, `Create list`, `Create trip`.

Search/filter:

- Search field: 48dp height, rounded full, surface-container-low #f3f4f5 background.
- Padding: 16dp horizontal; icon gap 12dp.
- Placeholder: `Search items...`, body-lg, on-surface-variant.
- Focus state: surface background with 2dp primary-container focus ring/glow.
- Filter button: 48dp circle, surface-container background, filter_list icon.

Cards:

- Surface: surface-container-lowest #ffffff.
- Border: 1dp surface-container-high or surface-container-low.
- Shape: 16-24dp depending card type.
- Accent bar: 4dp vertical bar on left for list cards/category cards; color maps to category/list theme.
- Padding: 24dp for list cards; category cards split into colored header and row list.

Chips:

- Shape: pill.
- Padding: approximately 12dp horizontal, 4-8dp vertical.
- Label: label-md.
- Active filter chip: secondary-container background, on-secondary-container text.
- Inactive filter chip: surface-container background, on-surface-variant text, light outline.
- Category preview chips: fixed accent background with corresponding on-fixed-variant text and a 14dp icon.

Inputs:

- White/surface background, 1dp outline-variant or surface-container-high border.
- Focus: 2dp primary border and soft mint outer glow.
- Error: error border/text, helper text in error.
- Do not disable the full screen during save; disable only the saving action.

Checkboxes/check rows:

- Touch target: row >= 48dp; checkbox visual 24-28dp.
- Unchecked: outline-variant circle / radio_button_unchecked, hover tint primary.
- Checked in packing mode: filled check_circle in secondary-container #9c7afe.
- Checked in item library categories: filled circle may use category accent (primary-container, secondary-container, tertiary-container) with on-container check.
- Packed row text: line-through, on-surface-variant, opacity around 60%.
- Quantity and metadata remain visible under packed item at 60% opacity.

Progress bars:

- Track: surface-container-highest #e1e3e4.
- Home trip cards: thin bar around 8dp height; fill can be primary for 65% packed and secondary-container for lavender card.
- Packing mode: 16dp height rounded full; fill secondary-container #9c7afe; progress value displayed separately in primary #006b57.
- Accessibility: expose numeric progress text and progress semantics.

Snackbars:

- Background: inverse-surface #2e3132.
- Text: inverse-on-surface #f0f1f2.
- Action: inverse-primary #00e0b8, uppercase label-md.
- Copy pattern: `Packed "Light Jacket"` with action `UNDO`.
- Position: bottom center above nav/FAB safe area.

## 3. Logo handoff

Logo uses a suitcase/checkmark metaphor:

- Icon: rounded suitcase with handle, two vertical straps, buckle cutouts, and a central checkmark.
- Brand color family: deep teal/mint close to primary #006b57, with subtle teal gradient and soft shadow.
- Suitcase body: translucent very light mint/white panel, rounded corners, slight glassy effect.
- Wordmark: `Packly`, bold rounded sans, dark teal, visually aligned with Plus Jakarta Sans ExtraBold/800.
- Spacing: icon centered above wordmark with generous white space; preserve clear space around logo equal to at least one strap width.

Implementation guidance:

- Use `docs/ux/logo.png` as the source image for splash/about/brand assets unless vectorizing.
- For app chrome, use text `Packly` rather than scaling the full logo into the top app bar; the screenshot top bars use text brand only.
- If making adaptive launcher icons, crop to the suitcase/checkmark icon without the wordmark for legibility at small sizes.
- Do not rely on the logo image as the only branding cue; app bars should use text for screen-reader clarity.

## 4. Screen-by-screen UX behavior and hierarchy

### Home screen

Reference: `packly_home_vibrant/screen.png`.

Visible copy:

- App brand: `Packly`
- Hero title: `Good Morning.`
- Hero body: `You have 2 upcoming trips. Let's get you prepared and ready to go.`
- Section title: `Active Trips`
- Trip card 1: `Trip to Japan`, `Oct 12 - Oct 26 • 14 Days`, `65% Packed`, chips `Carry-on`, `Cool Weather`.
- Trip card 2: `Weekend Getaway`, `Nov 3 - Nov 5 • 3 Days`, `10% Packed`, chips `Backpack`, `Beach`.

Hierarchy:

1. App bar with avatar/settings, brand centered.
2. Large greeting creates emotional entry point.
3. Supporting summary tells user why they are here.
4. Active Trips cards are the primary action area.
5. Bottom nav anchors top-level navigation.

Behavior:

- Tapping a trip card opens trip detail or packing mode depending existing flow; primary UX should expose packing progression quickly.
- Progress badge should summarize percent packed; progress bar supports fast scan.
- Empty active trips should show a friendly empty state with create trip CTA.
- If more than two active trips, cards scroll vertically.

Visual mapping:

- Background: surface #f8f9fa.
- Hero title: display-lg on compact if fits; otherwise 40sp/800 approximation.
- Trip card: white, rounded 12-16dp, 1dp light border, soft shadow, 4dp vertical accent bar.
- Trip card padding: 24dp.
- Progress badge: pill, primary-container or secondary-container background; on-container text.
- Progress track: surface-container-high; fill matches accent.
- Chips: lavender/peach-style accent at 15% opacity or secondary-fixed/tertiary-fixed equivalents.

### Items screen / item library

References: `packly_items_library_catalog/screen.png` and `packly_items_library_vibrant/screen.png`.

Visible copy:

- Search placeholder: `Search items...`
- Categories: `Electronics`, `Clothing`, `Toiletries`
- Count pills: `3 Items`, `5 Items`, `2 Items`, or `All Packed`
- Electronics rows: `MacBook Pro & Charger`, `Universal Travel Adapter`, `Noise Cancelling Headphones`
- Clothing rows: `Lightweight Jacket`, `Cold weather`, `T-Shirts (x4)`, `Jeans`
- Toiletries rows: `Toothbrush & Paste`, `Shampoo Travel Size`

Hierarchy:

1. App brand and global controls.
2. Search/filter as primary narrowing tools.
3. Category cards with strong colored headers and item counts.
4. Row-level item state: available/packed/duplicate warning.
5. FAB for adding item.

Behavior:

- Search filters items across categories while preserving category grouping for visible results.
- Filter button opens category/status filters as a bottom sheet or dropdown.
- Tapping an item row opens edit/details or toggles packed only if this screen is being used inside a packing context. For library-only mode, row should edit item; avoid accidental packing toggles outside trip context.
- Duplicate/conflict state shows red warning icon at row trailing edge and accessible text like `Duplicate item detected`.
- Drag handle shown in catalog reference suggests reorder; if reorder is not implemented yet, hide handle or make it non-interactive with no misleading affordance.

Visual mapping:

- Category card: white, 24dp radius, 1dp border, soft shadow.
- Accent bar: 4dp left edge spanning card height.
- Header: category accent tinted background.
- Electronics: primary-container accent (#00e5bc) with 10-15% header tint.
- Clothing: secondary-container accent (#9c7afe) with 20% header tint.
- Toiletries: tertiary-container accent (#94d2ff) with 10-15% header tint.
- Item rows: min 48dp; bottom dividers surface-container-low.
- Completed rows: checked circle, line-through, 60% opacity.

### Lists screen / templates

Reference: `packly_list_templates_vibrant/screen.png`.

Visible copy:

- Title: `My Lists`
- Subtitle: `Manage your packing templates and upcoming trips.`
- Cards: `Beach Weekend` / `45 Items`; `Business Trip` / `22 Items`; `Ski Trip` / `58 Items`
- Chips: `Clothes`, `Toiletries`, `Formal`, `Tech`, `Gear`, `Warm Clothes`
- Actions: `Rename`, `Duplicate`, `Archive`

Hierarchy:

1. Top app bar with avatar/brand/settings.
2. Screen title and explanatory subtitle.
3. List cards as primary content.
4. Card actions in a separated action bar.
5. FAB creates a new template/list.

Behavior:

- Card tap opens list detail/editor.
- Rename opens inline dialog/sheet with focused text field.
- Duplicate creates a copy and shows success snackbar.
- Archive must confirm or offer undo because it removes a reusable template.
- FAB opens create list flow.
- On medium width, cards may use two columns; on compact, one column.

Visual mapping:

- Card: white, rounded 16dp, padding 24dp, border surface-container-high, shadow.
- Accent bar: 4dp full height. Beach uses tertiary-fixed #c9e6ff; Business uses primary-fixed #42fdd3; Ski uses secondary-fixed #e8ddff.
- Chips use same fixed accent background with on-fixed-variant text.
- Action bar has top border surface-container and small icon+label buttons.
- Archive hover/pressed/error state uses error text and error-container background.

### Trips screen

No dedicated screenshot was present in docs/ux. Infer from Home active-trip cards and existing navigation requirements.

Required hierarchy:

1. Top app bar with Packly brand.
2. Screen title `Trips` or `My Trips`.
3. Active/upcoming trips first.
4. Completed/past trips collapsed or visually lower priority.
5. FAB/create CTA.

Behavior:

- Trip card should reuse Home active-trip visual language.
- Primary action should be `Start packing` or `Continue packing` based on packed count.
- Secondary actions: details/edit, delete/archive with confirmation.
- Trip creation can start blank or from a template/list snapshot.
- Empty state must include `Create trip` CTA.

Ambiguity: no exact Trips screenshot, so coder should infer card style from Home and list management behavior from existing app. Manu input is only needed if the Trips screen should diverge substantially from Home active trip cards.

### Packing mode

References: `packly_packing_mode_vibrant/screen.png` and `packly_packing_mode_quantities/screen.png`.

Visible copy:

- Brand: `Packly`
- Trip title: `Paris Trip`
- Label: `PACKING PROGRESS`
- Progress value: `80%`
- Filter chips: `All Items`, `Unpacked (4)`, `Packed (16)`
- Category `Essentials`: `Passport`, `Flight Tickets`, `Digital`, `Euros (Cash)`, `x2`
- Category `Clothing`: `Socks`, `x5`, `Evening Dress`, `Dinner at Le Jules Verne`, `Comfortable Walking Shoes`, `Light Jacket`, `x1`, `Weather forecast: 15°C`
- Snackbar: `Packed "Light Jacket"`, action `UNDO`
- Completion preview in HTML: `All Packed`

Hierarchy:

1. Focused app bar with back and settings.
2. Sticky trip progress header with trip name, progress label/value/bar, and filters.
3. Grouped category sections.
4. Checklist rows optimized for repeated taps.
5. Snackbar undo for recent pack/unpack action.

Behavior:

- Tapping a row toggles packed state immediately with optimistic UI.
- Do not reorder or auto-scroll packed items while the user is tapping. Stable row keys are required.
- Filters should not destroy scroll position unnecessarily.
- Progress bar animates to new percent around 500ms.
- Category headers can be sticky; if sticky causes complexity, preserve visual grouping first.
- Quantity display: show under item name as `xN` when quantity > 1, and for packed rows keep the quantity visible at 60% opacity. When quantity combines with another note, use `x1 • Weather forecast: 15°C` style.
- Metadata/tag chips such as `Digital` use a small grey surface-container badge.
- When progress reaches 100%, show an `All Packed` success affordance/panel; avoid heavy particle animations.
- Snackbar must include Undo and restore prior packed state.

Visual mapping:

- Header section sticky below top app bar: surface background, shadow `0 4dp 20dp rgba(0,0,0,0.02)`.
- Progress bar: 16dp high, rounded full, track surface-container-highest, fill secondary-container.
- Progress text: title-md, primary.
- Filter chips: active secondary-container, inactive surface-container with light outline.
- Category headers: primary text/icons, title-md; bottom divider surface-container-high.
- Row card: white, rounded 8dp, border surface-container-high, `0 2dp 12dp rgba(0,0,0,0.02)`.
- Row padding: 16dp; leading checkbox 24-28dp; label gap 24dp.

## 5. Visual and interaction states

Loading states:

- Home: skeleton cards for active trips plus skeleton text for greeting summary.
- Items: search field visible, category-card skeletons with shimmer or neutral placeholders.
- Lists: title visible, card skeletons.
- Trips: trip-card skeletons.
- Packing: sticky header skeleton with progress placeholder, row skeletons by category.
- Use subtle surface-container-low/high placeholders; avoid saturated loading colors.

Empty states:

- Home with no trips: friendly icon/illustration, copy `No trips yet`, body `Create your first trip and Packly will keep you ready.`, CTA `Create trip`.
- Items search empty: `No items found`, body `Try a different search or add a new item.`, CTA `Add item`.
- Items library empty: `Your item library is empty`, CTA `Add item`.
- Lists empty: `No packing lists yet`, body `Start with a reusable template for trips you take often.`, CTA `Create list`.
- Trips empty: `No trips planned`, CTA `Create trip`.
- Packing empty: `Nothing to pack yet`, body `Add items to this trip before entering packing mode.`, CTA `Add items` or `Back to trip`.

Error states:

- Missing route ID: show recoverable error card with `This trip/list could not be found` and `Back to Trips` or `Back to Lists`.
- Persistence failure: snackbar using inverse-surface with an error message and `Retry` action if applicable.
- Validation errors in create/edit forms: error color on field border/helper text, not a blocking dialog.
- Duplicate item: inline red warning icon plus accessible label; if tapped, explain duplicate source.
- Destructive actions: confirmation or undo for archive/delete/reset packed state.

Success states:

- Pack/unpack: snackbar `Packed "Item"` / `Unpacked "Item"` with `UNDO`.
- Save/create/duplicate/archive: snackbar concise success, e.g. `List duplicated` with optional `VIEW` or `UNDO`.
- Packing completion: `All Packed` pill/panel with celebration icon, primary-container background and on-primary-container text. Keep animation light.
- Form success should navigate back only after state is persisted.

Pressed/focused/disabled states:

- Buttons/FAB: active scale 0.95; hover/desktop scale 1.02-1.05; maintain 48dp touch target.
- Focus ring: primary-container or primary 2dp around inputs/search; visible on keyboard focus.
- Disabled: reduce opacity, but ensure labels remain readable; do not disable whole screen during saving.

## 6. Accessibility and usability requirements

WCAG/accessibility:

- Maintain 48dp minimum touch targets for nav items, icons, row toggles, chips where actionable, and FAB.
- Every screen must have a single semantic title.
- Progress bars must expose percent text, not only visual fill.
- Checklist rows must announce item name, category, quantity, note, and packed/unpacked state.
- Category icons are decorative when adjacent to category text; set null/empty content description. If standalone, provide descriptions.
- Icon-only buttons require content descriptions: Settings, Back, Add item/list/trip, Filter.
- Color cannot be the only status cue. Packed also uses check icon and strikethrough; duplicate also uses warning icon and accessible label; active nav uses container plus filled icon/label.
- Text contrast should be checked in CI or by a reviewer because local `npx @google/design.md lint` was unavailable.
- Support dynamic font scaling. Avoid hard clipping in bottom nav and chips; allow labels to wrap/truncate only where safe.

Responsiveness:

- Compact: single column, bottom navigation, FAB above nav.
- Medium: Lists may become two-column grid; Items/packing content should have max width around 600-720dp for readability.
- Expanded: consider NavigationRail only if existing architecture supports it; do not invent a desktop app layout for this task.
- Forms and detail content should max at 560-640dp.
- Respect safe areas/insets for bottom nav, FAB, and snackbar.

Usability guardrails:

- Packing mode must optimize for rapid repeated taps. No row reordering, no surprise scroll jumps, no blocking dialogs after a checkbox tap.
- Archive/delete/reset actions should be reversible or confirmed.
- Search should preserve text after navigation back if ViewModel state survives.
- Filter chips must make current filter obvious and counts must update with packed state.
- Empty and error states must offer a next action.

## 7. Ambiguities and inferred decisions

Inferred from screenshots/HTML:

- Top-level navigation order is Home, Items, Lists, Trips.
- Active top-level nav uses lavender secondary-container regardless of screen.
- Packing mode uses back arrow and no bottom nav in the screenshot; keep it focused and avoid bottom nav there unless existing navigation requires otherwise.
- Items category drag handles in catalog HTML appear as visual handles, but implementation should not expose dragging unless reorder actually works.
- Category accent mapping is: Electronics/mint, Clothing/lavender, Toiletries/sky. Other categories should derive from primary-fixed, secondary-fixed, tertiary-fixed and must include an icon/label.
- Quantity display should be shown as secondary metadata under the item, not appended to the main name, when space allows.

Needs Manu input only if these choices are product decisions:

- Whether Home greeting should be time-aware (`Good Morning.`, `Good Afternoon.`, etc.) or static.
- Whether item library rows toggle packed state or only edit item library entries. UX recommendation: library rows edit; packing-mode rows toggle.
- Whether Trips screen should have a separate unique visual treatment; no dedicated Trips screenshot exists.
- Whether app should support dark theme now. DESIGN.md includes light/dark-ish token roles in HTML, but screenshots are all light. Recommendation: implement light theme first and avoid half-finished dark mode.

## 8. Compose implementation map

Suggested UI-layer files to rebuild/extend:

- `ui/theme/Color.kt`: replace old Ocean/Mint/Coral palette with DESIGN.md color roles.
- `ui/theme/Type.kt`: define Plus Jakarta Sans typography roles.
- `ui/theme/Theme.kt`: wire Material 3 color scheme and shapes.
- `ui/token/Spacing.kt`: centralize 4/8/12/16/24/48/80dp values.
- `ui/token/CategoryTokens.kt`: map category key to accent color, container tint, on-color, and Material Symbol/icon.
- Shared components:
  - PacklyTopBar
  - PacklyBottomNav
  - PacklyFab
  - PacklySearchBar
  - PacklyCard
  - PacklyChip / FilterChip variants
  - PacklyProgressBar
  - PacklyChecklistRow
  - PacklyCategoryCard
  - PacklySnackbar host styling
  - EmptyState and ErrorState variants

Screen mapping:

- `HomeScreen`: rebuild hero + active trip cards from screenshot.
- `ItemsScreen`: rebuild search/filter + category cards + FAB.
- `ListsScreen`: rebuild list cards + actions + FAB.
- `TripsScreen`: reuse Home trip card pattern and active/completed grouping.
- `PackingModeScreen`: rebuild sticky progress header, filters, category checklist rows, snackbar undo, quantity metadata.
- Existing `TripCard`, `ListCard`, `ItemRow`, `CategoryHeader`, `PacklyProgress`, `QuantityBadge`, and `EmptyState` can be replaced rather than patched if token mismatch is high.

## 9. Acceptance criteria for coder/reviewer

Visual parity:

- The app uses Plus Jakarta Sans for all text.
- Core colors match the token list exactly; no old Ocean/Coral palette remains in user-facing UI.
- Top app bar, bottom nav, FAB, cards, chips, progress bars, and checklist rows match the screenshots in hierarchy, spacing, and shape.
- Home shows the large greeting and active trip cards with progress badges/chips.
- Items shows search/filter and grouped category cards with count pills, accent headers/bars, and duplicate warning state.
- Lists shows template cards with action bars and category chips.
- Packing mode shows sticky trip progress, filter chips, grouped checklist rows, quantities/notes, packed strikethrough states, and undo snackbar.
- Logo asset is available for launcher/splash/about use, while top app bars use text `Packly`.

Behavior parity:

- Bottom nav switches Home/Items/Lists/Trips and reflects active destination.
- FAB actions are accessible and route to the correct create/add flow.
- Search and filter states update content without losing grouping clarity.
- Packing toggles are optimistic, undoable, and do not reorder rows during rapid packing.
- Progress percentage and counts update after toggles.
- Empty, loading, error, and success states exist for every screen.

Accessibility parity:

- All action targets are at least 48dp.
- Icon-only buttons have content descriptions.
- Progress has semantic/text percent.
- Checklist rows expose packed state and item metadata.
- Color-only distinctions have icon/text alternatives.
- Keyboard/focus states are visible.

Validation constraints:

- Because this host is ARM64, do not rely on local Android/Gradle compile/lint/test. Commit code locally and block for review/push per board instructions.
- Prefer GitHub Actions validation after orchestrator push.
- If a coder can run non-Android static checks safely, they may do so, but lack of local Android build is expected.
