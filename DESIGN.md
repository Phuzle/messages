# Design system

Read this before writing or changing any UI code in this app. Every screen should look like it
was built by the same person on the same day — if you're about to type a raw hex color, a
`RoundedCornerShape(Ndp)` literal, or a Material3 `Button`/`Card`, stop and check whether one of
the primitives below already does it.

This file documents what's *actually* built (`ui/theme/`, `ui/components/`), not an aspiration.
When you add a new reusable pattern, add it here in the same edit — this file drifting out of
date is exactly how the inconsistency this file exists to prevent creeps back in.

## Color — always via `MessagesTheme.tokens`, never a literal hex

`val tokens = MessagesTheme.tokens` at the top of every screen. Tokens are computed per theme
mode (light/dark/amoled/sepia) and accent choice by `ui/theme/ThemeTokens.kt` — a literal
`Color(0xFF...)` in a screen is a value the theme system can't adjust and a sepia/AMOLED user
will see the wrong color on.

| Token | Use for |
|---|---|
| `bg` | Screen background |
| `surface` | Card/row backgrounds that need to sit *above* `bg` |
| `surfaceAlt` | A second, slightly-different-from-surface fill (icon badge circles, input backgrounds) |
| `border` | The 1dp hairline on every bordered card |
| `barBorder` | The hairline specifically under top bars / above bottom bars |
| `textPrimary` / `textSecondary` / `textTertiary` | Title / body / caption text, in that order of emphasis |
| `accent` / `accentSoft` / `accentText` | The raw chosen accent, its 16%-alpha tint (selected chips, tonal icon badges), and the color to put *on* raw `accent`. Raw `accent` is now only for the Appearance swatches and cursors — everything else uses the derived tokens below |
| `accentDeep` / `onAccentDeep` | The calm accent **fill** derived from `accent` (light/Sepia: mixed 40% toward `textPrimary`; dark/Midnight: mixed 60% toward `bg`) and the white/near-black that contrasts best on it. Use for sent bubbles, FAB, unread badges, switches, send/primary buttons |
| `accentLabel` | Accent-coloured **text and icons** (and content on `accentSoft` tonal pills): `accentDeep` in light themes, `accent` in dark ones. Passes 4.5:1 on `surface` for all 6 accents × 4 themes |
| `modalAccent` / `onModalAccent` | Primary button on the inverse-surface OTP modal (it uses the opposite theme's accent) |
| `danger` | Destructive actions and debit amounts. Never hardcode red. |
| `success` | Positive/credit amounts and success states. Never hardcode green. |
| `inputBg` | Search bars and other inline text-input fills |
| `switchTrackOff` | The off-state track of switches (on-state uses `accent`) |

If you need a color this table doesn't cover, that's a sign the token set is missing something —
add it to `ThemeTokens` (and both branches of `buildTheme`) rather than hardcoding.

## Shape — always `ShapeSmall` / `ShapeMedium` / `ShapeLarge` / `ShapePill` from `ui/theme/Shapes.kt` (plus `bubbleShape` for chat bubbles)

```kotlin
val ShapeSmall = RoundedCornerShape(14.dp)   // input fields, small chips, inline pills
val ShapeMedium = RoundedCornerShape(20.dp)  // cards, primary buttons, sheets
val ShapeLarge = RoundedCornerShape(28.dp)   // sheets, modals, filled text inputs
val ShapePill = RoundedCornerShape(50)       // true stadium — selector chips, tag pills
```

Chat bubbles use `bubbleShape(isMine, hasTail)` (22dp corners, 6dp tail on the sender's side of the last bubble of a run) — the only shape function outside the four above.

Never write `RoundedCornerShape(9.dp)` or any other one-off radius — it's the single biggest
source of screens visibly not matching each other. If none of the four fit, that's a sign to
reconsider the design, not to invent a fifth radius.

## Elevation — one soft level, via `softElevation` / `barElevation` only

There is exactly one elevation level. `Modifier.softElevation(shape)` (`ui/theme/Elevation.kt`) draws
a soft shadow on Light/Sepia and a 1dp `border` hairline on Dark/Midnight, where a shadow would be
invisible. It is used for: unread inbox rows, received chat bubbles, `SettingsCard` and other
grouped info cards, contact-info tiles. `Modifier.barElevation()` is the same idea for full-width
bars (`GlassBar`, the category chip strip, the reply bar, the Backup bottom bar). Never call
`Modifier.shadow()` with ad-hoc values on content, and no Material `Card`. Read (non-elevated) rows
and everything else stay flat.

## The two list/content patterns — pick the right one, don't invent a third

**Flat row** — every repeating list: `ThreadRow`, Passbook account rows, transaction rows, drafts,
archived, recycle bin. Read rows have no card background and no border: `tokens.bg` background (or
`tokens.accentSoft` when selected), `ShapeMedium` clip so the selected tint and ripple are rounded,
`padding(horizontal = 16.dp, vertical = 12–14.dp)`, leading avatar (48–52dp) + a `Column` of
title/subtitle, trailing content right-aligned, 4dp of outer padding between rows. **Unread**
inbox rows are the one exception: they lift into an elevated `tokens.surface` card
(`softElevation(ShapeMedium)`) with the time in `accentLabel`; that is the entire unread treatment
besides the bold name and the count badge. Inbox previews are always **one line**.

**Soft card** (`SettingsCard` in `ui/components/SettingsRow.kt`) — grouped settings/info, never
a repeating list: `softElevation(ShapeMedium)` + `background(tokens.surface, ShapeMedium)`, wrapping a
`Column` of `SettingsToggleRow`/`SettingsNavRow` (or custom content), separated internally by
`SettingsRowDivider()`. No `border` on top of it — `softElevation` already supplies the dark-theme
hairline. Used for Settings screens, the Backup & Restore sections, the About screen's info panels —
anywhere you're grouping a handful of *related but distinct* controls, not iterating a data list.

If what you're building is a list of things → flat row. If it's a labeled group of controls or a
single info panel → soft card. Don't reach for `accentSoft` background + border + rounding on
a single standalone element (a banner, a notice) just because it "needs to stand out" — that's a
third pattern this app doesn't otherwise have; use a flat tinted row instead (see
`DashboardScreen.FeedbackBanner` for the current example).

## Buttons

- **Primary CTA** (`PrimaryButton`, `ui/components/PrimaryButton.kt`): full-width, `tokens.accentDeep`
  background, `ShapeMedium`, `tokens.onAccentDeep` label, bold 16sp, 52dp minimum height. This is a flat
  `Text().background().clickable().padding()` composable, not Material3's `Button` — Material3
  `Button`/`TextButton` bring their own elevation/ripple/internal-padding defaults that don't match
  this system and must not be used for primary actions (`DriveSignInPromptScreen` used to use
  `Button`/`TextButton`; don't reintroduce that).
- **Secondary/text action**: `Text(...)` in `tokens.accentLabel` or `tokens.textTertiary`, no
  background, just `.clickable().padding()` with a 48dp minimum height. For a "Skip"/"Cancel"-style action under a primary
  button.
- **Selector pill** (`PillButton`, `ui/components/PillButton.kt`): the soft-accent-tinted pill used
  for theme/accent/swipe/schedule choices — `ShapePill`, 40dp visual inside a 48dp tap target,
  border, active/inactive tint pair from `pillOptionColors`. Category chips (`CategoryChip`) follow
  the same 40/48 rule with `categoryChipColors` (tonal active fill, no border).
- **Toggle rows** (`SettingsToggleRow`): the whole 64dp-minimum row is tappable and calls the same
  callback as the switch.
- **Nav drawer** (`NavDrawer`): 52dp pill rows; the destination you are on (`DrawerItem.selected`)
  gets an `accentSoft` fill and `accentLabel` content.
- **OTP modal** uses a flat 52dp button on `modalAccent`, not Material `Button`, like every other CTA.
- **Inline action pill** (`TonalActionPill`, `ui/components/TonalActionPill.kt`): the small
  Restore / Unarchive / Unblock / Export action — 40dp `accentSoft` pill inside a 48dp tap target.
  Replaces outlined mini-buttons with hand-rolled radii.
- **Icon-only tap target** (`roundClickable`, used throughout bars/rows): a 48dp square with a
  centered icon, ripple clipped to a circle. **Every tap target is at least 48dp** in each
  dimension; if the visual is smaller (chips, pills, switches) the clickable area still pads to 48.

## Icon badges

A leading icon inside a tinted circle — used on the SMS-disclosure screen's rows, Passbook account
avatars, transaction credit/debit icons. Canonical form: `Box(Modifier.size(40.dp).background(tint,
CircleShape), contentAlignment = Alignment.Center) { Icon(..., tint = iconColor, modifier =
Modifier.size(iconSize)) }`, typically `tint = tokens.accentSoft` / `iconColor = tokens.accent`
for a neutral badge, or `success`/`danger` at low alpha for credit/debit. Use `IconBadge` in
`ui/components/IconBadge.kt` rather than hand-rolling this Box each time.

## Typography scale

No `Typography`/`MaterialTheme.typography` usage — every `Text` sets its own `fontSize`/
`fontWeight` directly. Keep new text within this scale rather than picking an arbitrary size:

| Role | Size | Weight |
|---|---|---|
| Screen/dialog title | 18–22sp | Bold |
| Row/card title | 15.5–17sp | SemiBold |
| Body / row subtitle | 14–15sp | Normal/Medium |
| Chat bubble text | 15sp / 22sp line | Normal |
| Caption / tertiary meta | 11–13sp | Normal, `textTertiary` |
| Section label (all-caps) | 12.5–13sp | Bold, `letterSpacing = 0.4–0.5sp`, `textTertiary` |

## Spacing

Not currently on a strict grid (existing screens use everything from 4dp to 32dp), but new work
should stick to a 4dp-based scale: **4, 8, 12, 16, 20, 24, 32**. Prefer `Arrangement.spacedBy(n)`
over manual per-child `padding(top = n)` when laying out a `Column`/`Row` of siblings — it keeps
the rhythm visible in one place instead of scattered across every child.

Standard screen content padding: `horizontal = 16.dp`. Standard card internal padding: `16.dp`.
Standard row padding: `horizontal = 16.dp, vertical = 12–14.dp` (flat rows) or `16.dp` all sides
(card rows). Chat bubbles sit 3dp apart inside a run and 12dp apart between runs.

## Chat bubbles

`MessageBubble` (ThreadScreen.kt): sent = `accentDeep` / `onAccentDeep`; received = `surface` with
`softElevation` (Light/Sepia) or `surfaceAlt` + hairline (Dark/Midnight). Max width 78% of the
screen. Runs (same sender, same day, ≤5 minutes apart) are derived in `ThreadScreen` from the
messages already on screen: only the last bubble of a run gets the 6dp tail corner and the
timestamp. A message still waiting to send is a transparent bubble with a dashed `accentLabel`
outline, a clock icon and its send time.

## Bars — `GlassBar` only

Every top or bottom bar is `GlassBar` (`ui/components/GlassBar.kt`): solid `tokens.surface`
background (not translucent — an earlier frosted-glass look read as messy over scrolling content),
`barElevation()` (soft shadow, or a hairline in dark themes), 64dp default height, `BarInset.Top`/`.Bottom` to pad content clear of the system status/nav bar
without shrinking the painted background. Don't build a one-off top bar `Row` inside a plain `Box`.

## Startup/status screens

`SyncingScreen`, `DriveSignInPromptScreen`, `DriveRestoreDialog`, and `SmsDisclosureScreen`
together make up the single startup flow (`StartupFlowScreen`) a fresh install walks through
before reaching the dashboard. They share one job — communicate one thing (a permission ask, a
progress state, a yes/no decision) — and must share one look:

- Content is **not** dead-centered in an otherwise-empty screen (that reads as a tiny cluster
  floating in a sea of blank space, not "cramped" in the way padding failures are, but just as
  wrong). Anchor content starting a fixed distance below the status bar (`padding(top = 64.dp)`
  or similar), so the composition has a clear top-down reading order even when it's short.
  Long-form content (`SmsDisclosureScreen`'s list of permission explanations) still scrolls.
- The header block — logo/icon badge, title, subtitle — is **horizontally centered**
  (`horizontalAlignment = Alignment.CenterHorizontally`, `TextAlign.Center` on the text). This is
  what makes it read as a standard onboarding/permission screen instead of a settings page; don't
  drop it for the sake of matching a list's left alignment below it.
- **Any screen with action buttons pins them to the bottom of the screen**, not inline right after
  the last line of text (`SmsDisclosureScreen`, `DriveSignInPromptScreen`). Structure as an outer
  `Box(fillMaxSize)` with a scrollable content `Column` filling it, and a second `Column` holding
  the buttons `Modifier.align(Alignment.BottomCenter)` + `navigationBarsPadding()` +
  `background(tokens.bg)` so it reads as a fixed region, not part of the scroll. `PrimaryButton`
  for the forward action, `SecondaryTextButton` below it for skip/cancel — never Material3
  `Button`/`TextButton` (see Buttons above). This is what makes the screen feel airy: content and
  action are two distinct regions instead of the button crowding whatever text precedes it.
  Screens with **no actions** (`SyncingScreen` is pure status, nothing to tap) skip this — just the
  centered header block, top-anchored per above.
- Multi-item lists on these screens (`SmsDisclosureScreen`'s three permission rows) use a
  **horizontal** layout — icon badge on the left, title+detail stacked to its right — never icon
  stacked *above* title stacked above detail. Vertical stacking per-row is what makes a 3-item list
  take the whole screen; horizontal keeps each row to roughly one line of vertical space per detail
  line. Unlike the header, these rows stay left-aligned — only the header block is centered.

## Search mode

- Entering search replaces the dashboard top bar (back arrow · expanded `inputBg` field with an X
  clear button · Filters button); every other icon is hidden. The Filters button shows a dot when
  any non-default filter is active.
- Filter choices live in one bottom sheet (`SearchFiltersSheet`) made of pill-shaped
  `FilterChip`-style choices — Sort, Category, Date, Direction. Active non-default filters are echoed
  as removable chips under the bar (`SearchFilterSummary`); tapping one clears it.
- Results are one row per **message** (`SearchResultRow`), not per thread, with the match bolded in a
  snippet centered on the hit. Contacts are a separate section above Messages. Tapping a message
  opens its thread scrolled to that message, which flashes a primary-colored outline briefly.
- Ranking is a domain concern (`domain/search`), never a UI one: strong matches (whole word / word
  start) first, newest-first within each band.

## Known debt

Not everything in the app matches this doc yet — some Settings/Backup screens still have one-off
`RoundedCornerShape(Ndp)` literals or hand-rolled buttons from before this file existed. Don't take
that as license to add another one; when you're touching a screen anyway, pull it onto the tokens
above.
