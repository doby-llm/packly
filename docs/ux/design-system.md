# Packly Design System

## Design system goals

Packly’s design system should make the app feel modern, friendly, colorful, and easy to retheme. It should be built on Material 3 semantic roles, not one-off colors, so future themes can change without rewriting screens.

Core qualities:

- Calm: soft surfaces and readable hierarchy.
- Cheerful: category accents feel like luggage tags and travel stickers.
- Trustworthy: destructive actions are clear, progress is accurate, and save states are visible.
- Lightweight: animations are short, lists are performant, and components remain standard Compose Material 3 wherever possible.

## Visual inspiration

Use a blend of:

- Material 3 expressive but accessible component defaults.
- Airtable/Figma-like friendly color coding for categories.
- Notion-like whitespace and low-noise content hierarchy.
- Airbnb-like warmth for travel-adjacent friendliness.

The result should not imitate a brand directly; it should feel native to Android and purpose-built for packing.

## Color system

Use semantic Material 3 tokens as the app contract:

- primary: main actions and progress.
- secondary: supporting actions, selected chips.
- tertiary: delight/accent moments.
- surface: screen/card backgrounds.
- surfaceVariant: grouped sections and secondary containers.
- error: destructive actions and validation.
- outline: dividers, input borders, inactive icons.

Suggested light palette:

| Role | Token | Value | Usage |
| --- | --- | --- | --- |
| Primary | packlyOcean | #2F6FED | Main CTA, active progress, selected nav |
| On primary | onPacklyOcean | #FFFFFF | Text/icons on primary |
| Primary container | packlyOceanSoft | #DCE8FF | Low-emphasis primary containers |
| On primary container | onPacklyOceanSoft | #0B2F6B | Text on primary container |
| Secondary | packlyMint | #2D9C7A | Positive accents, packed status |
| Secondary container | packlyMintSoft | #DDF6EC | Packed chips, success containers |
| Tertiary | packlyCoral | #F27B6D | Warm highlights, onboarding moments |
| Tertiary container | packlyCoralSoft | #FFE2DE | Soft warm cards |
| Background | packlyCanvas | #FCFCFF | App background |
| Surface | packlySurface | #FFFFFF | Cards, sheets, dialogs |
| Surface variant | packlySurfaceTint | #F1F4FA | Group backgrounds |
| Outline | packlyOutline | #C5CAD6 | Dividers and borders |
| Error | packlyError | #BA1A1A | Errors/destructive actions |

Suggested dark palette:

| Role | Token | Value | Usage |
| --- | --- | --- | --- |
| Primary | packlyOceanDark | #AFC6FF | CTA and progress on dark |
| Primary container | packlyOceanDeep | #17468F | Selected containers |
| Secondary | packlyMintDark | #9BE2C5 | Packed/success accents |
| Tertiary | packlyCoralDark | #FFB4A8 | Warm accent |
| Background | packlyNight | #101318 | App background |
| Surface | packlyNightSurface | #181C22 | Cards and sheets |
| Surface variant | packlyNightVariant | #252A33 | Category groups |
| Outline | packlyNightOutline | #8B919C | Borders and dividers |
| Error | packlyErrorDark | #FFB4AB | Errors |

Accessibility notes:

- Verify all token combinations with contrast tests before release.
- Do not put body text on category colors unless contrast is guaranteed.
- Prefer tint containers plus dark text over saturated fills.
- Support Material You dynamic color as an optional mode only if category identity remains understandable.

## Category color and icon tokens

Each category should have an icon, accent color, soft container, and stable semantic key for persistence.

| Category | Key | Icon suggestion | Accent | Soft container |
| --- | --- | --- | --- | --- |
| Clothing | clothing | checkroom / laundry | #6C63FF | #ECEAFF |
| Toiletries | toiletries | soap / shower | #00A7A7 | #DDF7F6 |
| Electronics | electronics | cable / devices | #2F6FED | #DCE8FF |
| Documents | documents | badge / article | #B7791F | #FFF0D2 |
| Health | health | medical_services | #D94E67 | #FFE1E8 |
| Travel Comfort | comfort | flight_takeoff / chair | #7C4DFF | #EEE6FF |
| Weather | weather | umbrella / wb_sunny | #F59E0B | #FEF3C7 |
| Kids / Family | family | child_friendly / family_restroom | #EC4899 | #FCE7F3 |
| Food / Snacks | food | lunch_dining | #43A047 | #E5F5E8 |
| Miscellaneous | misc | category | #64748B | #EEF2F7 |

Implementation rules:

- Persist category keys, not localized labels.
- Pair every color with a text label and icon.
- Keep icons from one family, ideally Material Symbols Rounded.
- Category chips use soft container backgrounds; selected chips add a stronger border or filled tonal state.

## Typography

Use Android system defaults through Material 3 typography unless the product later chooses a custom font. Recommended style mapping:

| Purpose | Material style | Notes |
| --- | --- | --- |
| App title / Home hero | headlineMedium | Friendly, not oversized |
| Screen title | titleLarge | Top app bar/title area |
| Section headers | titleMedium | Category groups, form sections |
| Card titles | titleMedium / titleSmall | List/trip cards |
| Item names | bodyLarge | Checklist readability |
| Metadata | bodyMedium | Quantity, notes, dates |
| Helper/error text | bodySmall | Forms and validation |
| Buttons | labelLarge | Material default |
| Chips | labelLarge / labelMedium | Based on density |

Typography requirements:

- Support font scale up to at least 200% without truncating core actions.
- Avoid all-caps labels except where Material defaults apply.
- Use max line widths on tablet so content does not become hard to scan.

## Spacing and shape

Spacing scale:

- 4 dp: tiny gaps, icon/text internal spacing.
- 8 dp: chip gaps, compact row internals.
- 12 dp: card internal clusters.
- 16 dp: standard screen margin and form spacing.
- 24 dp: section separation.
- 32 dp: major layout separation / empty states.

Shape scale:

- 8 dp: small chips, compact controls.
- 12 dp: text fields, list rows.
- 16 dp: cards, bottom sheets.
- 24 dp: hero cards and prominent containers.
- Full: FAB, circular progress, icon badges.

Elevation:

- Prefer tonal elevation over heavy shadows.
- Use elevation sparingly: FAB, modal sheets, dialogs, active dragged rows.
- Most cards can be flat with a subtle border or surfaceVariant background.

## Component specifications

### Home destination card

Purpose: lead users to Items, Lists, Trips.

Content:

- Leading illustrative icon.
- Title.
- One-line explanation.
- Optional count/progress.
- Entire card tappable.

States:

- Default: white/surface, soft icon container.
- Pressed: tonal overlay.
- Focused: visible outline.
- Empty data: show starter action text.

### Category header

Purpose: make grouped lists scannable.

Content:

- Icon in soft category container.
- Category name.
- Count or progress.
- Expand/collapse affordance.

Behavior:

- Header tap toggles collapse.
- In trips, show packed/total count.
- Maintain a minimum 48 dp touch target.

### Item row

Purpose: present one reusable or trip item.

Library variant:

- Leading category marker.
- Item name.
- Metadata line when present.
- Trailing edit or overflow.

Packing variant:

- Leading checkbox with large hit area.
- Item name and quantity.
- Optional notes icon.
- Packed state uses checked icon, secondary text, and reduced emphasis.

Accessibility:

- Row role should communicate button/checkbox behavior correctly.
- Content description should include item state in packing mode.

### List card

Content:

- List name.
- Item count.
- Category icon stack or chips.
- Last updated.
- Primary action “Use for trip”.

States:

- Empty list: show “No items yet” and “Add items”.
- Template/seed: optional “Sample” badge.

### Trip card

Content:

- Trip name/destination.
- Date range or “No dates”.
- Progress bar/ring.
- Packed count.
- CTA: Start packing / Continue / Review.

Visual priority:

- Active trips appear above completed trips.
- Nearly complete trips can show a positive mint accent.

### Forms

Use full-screen forms for complex list/trip editors; use bottom sheets for quick item creation.

Required field treatment:

- Label required fields with text, not only asterisks.
- Show errors inline below the field.
- Disable Save only when impossible, but prefer explaining validation after interaction.

### Dialogs

Use AlertDialog for destructive confirmations.

Dialog content rules:

- Title names the object.
- Body explains consequences in one sentence.
- Destructive action uses error color.
- Cancel remains the safe default.

### Snackbar

Use for:

- Saved item/list/trip.
- Deleted with Undo.
- Packed/unpacked toggles when accidental changes are likely.

Avoid long messages; keep to one line when possible.

## Motion system

Motion should clarify state, not entertain at the user’s expense.

Recommended durations:

- Checkbox toggle: 100–150 ms.
- Chip selection: 100–150 ms.
- Category expand/collapse: 150–200 ms.
- Screen transition: Material default.
- Progress change: 200–300 ms.
- Success check: 300–500 ms.

Reduced motion:

- Keep layout changes immediate or use fade only.
- Disable decorative success animation.
- Preserve essential state changes with static feedback.

## Accessibility checklist

Every screen:

- Has one clear title.
- Has logical TalkBack traversal.
- Supports landscape and large text.
- Uses visible focus states.
- Keeps touch targets at least 48 dp.
- Does not rely on color alone.
- Uses semantic buttons, checkboxes, text fields, and headings where possible.

Specific checks:

- Packing progress must be exposed as text, not only a visual bar.
- Category icons marked decorative if the category label is adjacent; otherwise include label in content description.
- Delete dialogs should focus the title first and keep Cancel reachable before Delete.
- Snackbars with Undo must remain visible long enough for accessibility users; use Material defaults or extended duration for undo.

## Responsive layout rules

Compact width:

- Single-column layout.
- Bottom navigation or Home destination cards.
- FAB for primary creation action.
- Full-screen editors for complex flows.

Medium width:

- Two-column layouts for overview/detail where helpful.
- NavigationRail if persistent top-level navigation is introduced.
- Keep forms max-width around 560–640 dp.

Expanded width/tablet:

- NavigationRail plus content.
- Trip/list master-detail layout.
- Checklist max-width around 720 dp, centered or paired with details panel.
- Avoid stretching category rows edge-to-edge on very wide screens.

## Retheming contract

Engineering should expose tokens in one theme layer:

- Semantic app colors.
- Category color map.
- Shape scale.
- Spacing constants.
- Motion duration constants.

Do not hardcode category colors inside individual screens. Components should receive category key or design token references.

## Iconography

Recommended family: Material Symbols Rounded.

Style rules:

- Rounded, friendly icons.
- Filled/outlined state can indicate selected vs default.
- Keep stroke/weight consistent.
- Use category icons as wayfinding, not decoration.
- Avoid emoji as primary icons because rendering differs by platform and accessibility labels become inconsistent.

## Content and naming conventions

Prefer plain, human words:

- “Items” not “Inventory entities”.
- “Lists” not “Templates” in primary navigation, though helper copy can say reusable list.
- “Trips” not “Sessions”.
- “Packed” / “Unpacked” not “Complete” / “Incomplete”.

Button labels:

- Add item
- Create list
- Start trip
- Start packing
- Continue packing
- Finish packing
- Reset packed items

## State matrix

| Component/screen | Loading | Empty | Error | Success |
| --- | --- | --- | --- | --- |
| Home | Skeleton destination cards | Starter hero with sample actions | Inline retry card | Snackbar after quick actions |
| Items | Skeleton rows grouped by category | “No items yet” + Add item | Retry list load, preserve filters | “Item saved” / Undo delete |
| Lists | Skeleton list cards | “No packing lists yet” + Create list | Retry and keep local edits | “List saved” / “List duplicated” |
| Trips | Skeleton trip cards | “No trips planned” + Start trip | Retry load, keep draft | “Trip created” |
| Packing mode | Skeleton checklist | “No unpacked items” for filter | Inline retry if state fails to save | “Everything is packed” success panel |
| Forms | Button spinner while saving | N/A | Inline validation and save failure | Close sheet and show Snackbar |

## Engineering handoff notes

- Build components with Compose Material 3 first; customize through tokens before writing custom UI primitives.
- Keep category and theme data serializable in JSON-friendly structures.
- Snapshot list items into trips at creation so UI copy and persistence match.
- Seed data should be visually complete: category key, icon key, name, default quantity.
- Prioritize accessibility semantics during component creation, not after visual polish.
