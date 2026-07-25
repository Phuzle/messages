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
| `accent` / `accentSoft` / `accentText` | Brand color, its 16%-alpha tint (icon badges, selected chips), and the color to put *on* `accent` (usually white/near-black depending on theme) |
| `danger` | Destructive actions and debit amounts. Never hardcode red. |
| `success` | Positive/credit amounts and success states. Never hardcode green. |
| `inputBg` | Search bars and other inline text-input fills |
| `switchTrackOff` | The off-state track of switches (on-state uses `accent`) |

If you need a color this table doesn't cover, that's a sign the token set is missing something —
add it to `ThemeTokens` (and both branches of `buildTheme`) rather than hardcoding.

## Shape — always `ShapeSmall` / `ShapeMedium` / `ShapeLarge` / `ShapePill` from `ui/theme/Shapes.kt`

```kotlin
val ShapeSmall = RoundedCornerShape(14.dp)   // input fields, small chips, inline pills
val ShapeMedium = RoundedCornerShape(20.dp)  // cards, primary buttons, sheets
val ShapeLarge = RoundedCornerShape(28.dp)   // rarely used — large modal surfaces
val ShapePill = RoundedCornerShape(50)       // true stadium — selector chips, tag pills
```

Never write `RoundedCornerShape(9.dp)` or any other one-off radius — it's the single biggest
source of screens visibly not matching each other. If none of the four fit, that's a sign to
reconsider the design, not to invent a fifth radius.

Zero elevation everywhere. No `Card`, no `Modifier.shadow()` on content surfaces (the top/bottom
`GlassBar` is the one deliberate exception — see below). Flat fills + a 1dp `border` stroke is how
this app shows "this is a distinct surface," not a shadow.

## The two list/content patterns — pick the right one, don't invent a third

**Flat row** — every repeating list: `ThreadRow`, Passbook account rows, transaction rows, drafts,
archived, recycle bin. No card background, no border, no rounding. Full-width, `tokens.bg`
background (or `tokens.accentSoft`/similar flat tint if the row needs to stand out — still no
border/rounding), `padding(horizontal = 16.dp, vertical = 12.dp)`, leading icon/avatar + a
`Column` of title/subtitle, trailing content right-aligned. Rows are visually separated by nothing
but their own padding — no dividers between them, no gaps. This *is* the message list's look; if
a screen looks like the message list, it's using this pattern correctly. (Passbook was rebuilt to
match this exact pattern after it originally used cards — see git history if you need the
before/after.)

**Bordered card** (`SettingsCard` in `ui/components/SettingsRow.kt`) — grouped settings/info, never
a repeating list: `background(tokens.surface, ShapeMedium).border(1.dp, tokens.border,
ShapeMedium)`, wrapping a `Column` of `SettingsToggleRow`/`SettingsNavRow` (or custom content),
separated internally by `SettingsRowDivider()`. Used for Settings screens, the Backup & Restore
sections, the About screen's info panels — anywhere you're grouping a handful of *related but
distinct* controls, not iterating a data list.

If what you're building is a list of things → flat row. If it's a labeled group of controls or a
single info panel → bordered card. Don't reach for `accentSoft` background + border + rounding on
a single standalone element (a banner, a notice) just because it "needs to stand out" — that's a
third pattern this app doesn't otherwise have; use a flat tinted row instead (see
`DashboardScreen.FeedbackBanner` for the current example).

## Buttons

- **Primary CTA** (`PrimaryButton`, `ui/components/PrimaryButton.kt`): full-width, `tokens.accent`
  background, `ShapeMedium`, `tokens.accentText` label, bold ~14.5sp. This is a flat
  `Text().background().clickable().padding()` composable, not Material3's `Button` — Material3
  `Button`/`TextButton` bring their own elevation/ripple/internal-padding defaults that don't match
  this system and must not be used for primary actions (`DriveSignInPromptScreen` used to use
  `Button`/`TextButton`; don't reintroduce that).
- **Secondary/text action**: `Text(...)` in `tokens.accent` or `tokens.textTertiary`, no
  background, just `.clickable().padding()`. For a "Skip"/"Cancel"-style action under a primary
  button.
- **Selector pill** (`PillButton`, `ui/components/PillButton.kt`): the soft-accent-tinted pill used
  for theme/accent/swipe/schedule choices — `ShapePill`, border, active/inactive tint pair from
  `pillOptionColors`.
- **Icon-only tap target** (`roundClickable`, used throughout bars/rows): a 36–40dp square with a
  centered icon, ripple clipped to a circle.

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
| Screen/dialog title | 16–20sp | Bold |
| Row/card title | 14–15.5sp | SemiBold |
| Body / row subtitle | 13–13.5sp | Normal/Medium |
| Caption / tertiary meta | 11–12.5sp | Normal, `textTertiary` |
| Section label (all-caps) | 12–13sp | Bold, `letterSpacing = 0.4–0.5sp`, `textTertiary` |

## Spacing

Not currently on a strict grid (existing screens use everything from 4dp to 32dp), but new work
should stick to a 4dp-based scale: **4, 8, 12, 16, 20, 24, 32**. Prefer `Arrangement.spacedBy(n)`
over manual per-child `padding(top = n)` when laying out a `Column`/`Row` of siblings — it keeps
the rhythm visible in one place instead of scattered across every child.

Standard screen content padding: `horizontal = 16.dp`. Standard card internal padding: `14.dp`.
Standard row padding: `horizontal = 16.dp, vertical = 12.dp` (flat rows) or `14.dp` all sides
(card rows).

## Bars — `GlassBar` only

Every top or bottom bar is `GlassBar` (`ui/components/GlassBar.kt`): solid `tokens.surface`
background (not translucent — an earlier frosted-glass look read as messy over scrolling content),
soft `6.dp` shadow, `BarInset.Top`/`.Bottom` to pad content clear of the system status/nav bar
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

## Known debt

Not everything in the app matches this doc yet — some Settings/Backup screens still have one-off
`RoundedCornerShape(Ndp)` literals or hand-rolled buttons from before this file existed. Don't take
that as license to add another one; when you're touching a screen anyway, pull it onto the tokens
above.
