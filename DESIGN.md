---
name: "AppDist — Lovable Utility"
description: "A calm, continuous release workspace that makes dense Android build operations feel immediate and humane."
colors:
  graphite: "#2B2A28"
  body-ink: "#44423F"
  milk: "#FCFBF8"
  white: "#FFFFFF"
  muted-ink: "#716E6A"
  badge-ink: "#65635F"
  border: "#D8D5CF"
  quiet-border: "#EBEAE7"
  soft-surface: "#F3F0F2"
  lilac: "#C9C7DB"
  lilac-strong: "#A9A5C8"
  gradient-blue: "#EEF1FF"
  gradient-rose: "#FFF0FC"
  success-ink: "#2F6E48"
  warning-ink: "#7A5311"
  danger-ink: "#9B2C3A"
  success-surface: "#E2F1E8"
  warning-surface: "#F6E8C9"
  danger-surface: "#F8E1E3"
typography:
  display:
    fontFamily: "Funnel Sans, Geist, sans-serif"
    fontSize: "clamp(2.5rem, 4vw, 3.25rem)"
    fontWeight: 700
    lineHeight: 1.02
    letterSpacing: "-0.035em"
  headline:
    fontFamily: "Funnel Sans, Geist, sans-serif"
    fontSize: "clamp(2rem, 3vw, 2.5rem)"
    fontWeight: 700
    lineHeight: 1
    letterSpacing: "-0.025em"
  title:
    fontFamily: "Funnel Sans, Geist, sans-serif"
    fontSize: "1.375rem"
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: "-0.012em"
  body:
    fontFamily: "Geist, Arial, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: "normal"
  label:
    fontFamily: "Geist, Arial, sans-serif"
    fontSize: "0.75rem"
    fontWeight: 600
    lineHeight: 1.35
    letterSpacing: "normal"
  data:
    fontFamily: "Geist Mono, ui-monospace, monospace"
    fontSize: "0.6875rem"
    fontWeight: 500
    lineHeight: 1.4
    letterSpacing: "0.045em"
rounded:
  xs: "6px"
  sm: "8px"
  md: "10px"
  lg: "14px"
spacing:
  xxs: "4px"
  xs: "8px"
  sm: "12px"
  md: "16px"
  lg: "24px"
  xl: "32px"
  xxl: "40px"
components:
  button-primary:
    backgroundColor: "{colors.graphite}"
    textColor: "{colors.white}"
    typography: "{typography.label}"
    rounded: "{rounded.sm}"
    padding: "10px 16px"
    height: "44px"
  button-secondary:
    backgroundColor: "{colors.white}"
    textColor: "{colors.graphite}"
    typography: "{typography.label}"
    rounded: "{rounded.sm}"
    padding: "10px 16px"
    height: "44px"
  field:
    backgroundColor: "{colors.milk}"
    textColor: "{colors.graphite}"
    typography: "{typography.body}"
    rounded: "{rounded.sm}"
    padding: "12px 14px"
    height: "44px"
  card:
    backgroundColor: "{colors.white}"
    textColor: "{colors.graphite}"
    rounded: "{rounded.lg}"
    padding: "24px"
  status-active:
    backgroundColor: "{colors.success-surface}"
    textColor: "{colors.success-ink}"
    typography: "{typography.data}"
    rounded: "{rounded.xs}"
    padding: "7px 10px"
---

# Design System: AppDist — Lovable Utility

## Overview

**Creative North Star: "The Lovable Utility Workspace"**

AppDist is one continuous release workspace: calm enough to reduce anxiety around publishing, precise enough to support rapid comparison of versions, channels, environments, and status. Its personality comes from an editorial Funnel Sans voice, warm milk-and-graphite fundamentals, and pale lilac/rose/blue atmosphere—not from decorative dashboard chrome.

The web admin expression wraps a dense operational core in soft structural framing. A fixed sidebar and compact top bar establish place; the dominant gradient header establishes the current task; the table or form immediately beneath it carries the work. On compact screens the shell becomes a drawer, the build view reduces to its critical scan set, and authentication moves ahead of editorial storytelling. Surfaces are quiet, controls are familiar, and each screen has one unmistakable dark primary action.

**Key Characteristics:**

- One continuous desktop workspace with a fixed 244px sidebar.
- An adaptive mobile shell that protects the first task and a 44px touch-target floor.
- A dominant pale blue-to-rose gradient header for orientation and primary action.
- Funnel Sans for decisive display moments; Geist for interface reading; Geist Mono for build data.
- Dense, aligned operational content on warm-neutral and white surfaces.
- Restrained 6–14px corners, one-pixel borders, and ambient shadows used sparingly.

## Colors

Warm graphite and milk form the working canvas; lilac and pale spectral gradients soften structure while status tints carry meaning without shouting.

### Primary

- **Graphite:** The singular action color and strongest text color. Use it for primary buttons, the product mark tile, active pagination, and major headings.

### Secondary

- **Workspace Lilac:** A quiet selection and identity accent for avatars, active indicators, and small points of continuity across the workspace.

### Tertiary

- **Structural Blue and Rose:** The two endpoints of the atmospheric header gradient. Their job is to group and orient, never to compete with operational content.
- **Success, Warning, and Danger Surfaces:** Low-chroma status fills for healthy/active, production-like/caution, and deprecated/error states. Pair every tint with a text label; color is never the only signal.

### Neutral

- **Milk:** The warm application ground and field fill; it keeps the interface softer than clinical pure gray.
- **White:** The working surface for cards, tables, navigation, and the login form area.
- **Body Ink:** General interface copy that does not require Graphite's headline emphasis.
- **Muted Ink:** Secondary copy, timestamps, helper text, and noncritical navigation labels.
- **Badge Ink:** Neutral chip and compact metadata text where Muted Ink would be too soft.
- **Standard Border:** The visible one-pixel outline for controls and focus-adjacent structure.
- **Quiet Border:** The softer one-pixel separator for panels, table rows, and low-emphasis divisions.
- **Soft Surface:** Selected navigation, table headers, metadata blocks, and low-emphasis grouping.

### Named Rules

**The Accent Is Structure Rule.** Lilac, pale blue, and pale rose organize identity, selection, and page hierarchy; they do not fill routine buttons or turn tables into a rainbow.

**The Semantic Tint Rule.** Status color always appears as a compact tinted badge with explicit text and is never used as a full-row background.

## Typography

**Display Font:** Funnel Sans (with Geist and sans-serif fallbacks)  
**Body Font:** Geist (with Arial and sans-serif fallbacks)  
**Label/Mono Font:** Geist Mono (with ui-monospace and monospace fallbacks)

**Character:** Funnel Sans gives page titles and identity copy an open, friendly authority. Geist stays neutral and highly legible through controls and dense rows, while Geist Mono turns versions, build codes, package names, channels, roles, and audit metadata into a consistent data layer.

### Hierarchy

- **Display** (700, responsive 40–52px, 1.02): Login proposition and rare high-emphasis statements.
- **Headline** (700, responsive 32–40px, 1): Page titles such as available builds and new build.
- **Title** (700, 22px, 1.2): Card headings and major form groups.
- **Body** (400, 14px, 1.5): Descriptions, navigation, field values, and explanatory copy; keep long prose near 70 characters per line.
- **Label** (600, 12px, 1.35): Field labels, buttons, and compact interface emphasis.
- **Data** (500, 11px, 1.4, 0.045em): Version metadata, package IDs, environments, channels, roles, table headers, and counts. Uppercase only terse categorical values.

### Named Rules

**The Display Stops at Orientation Rule.** Funnel Sans establishes product and page hierarchy; it does not enter table cells, helper text, or dense metadata.

**The Mono Means Data Rule.** Use Geist Mono only when a value benefits from machine-like alignment or categorization. Never use it for sentences or navigation labels.

## Layout

The desktop admin is a continuous app shell. At large widths, a fixed 244px sidebar holds product identity, workspace switcher, primary navigation, public access, and the signed-in user. The main region begins with a 68px utility bar, then uses a 32px page gutter. Content aligns to one shared left and right edge so the gradient header, filters, table, and upload panels read as one system.

Below the large breakpoint, the same 244px navigation becomes an off-canvas drawer with a dismissing scrim and an explicit menu control in the 68px utility bar. Mobile page gutters reduce to 16px. All interactive controls retain a minimum 44px touch target on mobile, even when their desktop presentation compacts.

The login page deliberately changes priority by viewport. Desktop shows the editorial gradient panel beside authentication; mobile hides that panel and places authentication in the first viewport, retaining only the compact brand mark and theme control around it.

The page header is the dominant spatial landmark: a wide, shallow panel with 28–32px internal padding, primary copy on the left, and action/status context on the right. Keep the upload action visible in this header on the builds page. Below it, use a compact 24–32px vertical rhythm: filter row, then dense table; or a two-column upload composition with a fluid form and an approximately 360px publication summary.

Tables prioritize horizontal comparison. Headers stay compact, rows are approximately 72px high, and related primary/secondary values stack within a cell rather than expanding the column count. On mobile, Builds exposes Version, Environment, and Status as its critical scan set and hides lower-priority columns. Generic tables that cannot reduce columns live in a labeled, focusable horizontal-scroll region and show a compact mobile scroll hint. Forms use a two-column field grid, with full-width release notes and upload file blocks; they stack on compact widths, with the publication summary following the form until the wide two-column layout is available.

**The Shared Edge Rule.** Header, controls, data surface, and form panels use the same content bounds. Do not introduce arbitrary nested containers or competing max-widths.

**The Work Starts Above the Fold Rule.** Orientation and the primary action fit alongside the first meaningful rows or fields; oversized hero treatment must never push the task away.

**The Authentication First Rule.** Mobile login hides the editorial panel so the authentication task owns the first viewport; desktop may carry the fuller brand story beside it.

**The Critical Scan Set Rule.** Mobile Builds preserves Version, Environment, and Status before any other table columns. Generic tables must remain navigable as labeled, focusable horizontal-scroll regions.

## Elevation & Depth

AppDist is flat by default. Depth comes first from white-on-milk tonal layering, soft-surface grouping, quiet one-pixel borders, and the pale structural gradient. Use a restrained ambient shadow only on large floating-feeling surfaces such as the table, login release card, and principal form panels; controls and ordinary navigation remain unshadowed.

### Shadow Vocabulary

- **Ambient Panel** (`0 12px 32px rgba(43, 42, 40, 0.06)`): Large table, form, summary, or login preview surfaces that need gentle separation from the page ground.

### Named Rules

**The Flat-by-Default Rule.** Borders and tonal changes establish structure at rest; shadows are reserved for a small number of high-level surfaces and transient layers.

## Shapes

The form language is gently rectangular rather than pill-shaped. Compact labels and badges use 6px corners; buttons and fields use 8px; grouped controls use 10px; major headers, cards, tables, and form panels use 14px. Borders are one pixel and quiet. Circular geometry is reserved for avatars, status dots, step markers, and icon-only buttons.

**The 6–14 Rule.** Keep ordinary rectangular corners within the established 6–14px range. Do not drift into sharp enterprise boxes or inflated capsule cards.

## Components

### Buttons

- **Shape:** Compact, confident rectangles with gently curved corners (8px). Base actions are at least 44px high on mobile and may compact to 40px from the small desktop breakpoint; focused tasks such as login and final publish use 50–52px controls.
- **Primary:** Graphite background, white label, and a trailing icon when it clarifies direction. Use once per decision area for actions such as upload, receive code, or publish.
- **Hover / Focus:** Hover shifts the background or border without moving the control; focus uses a visible two-pixel Strong Workspace Lilac outline with two-pixel offset.
- **Secondary / Ghost:** White or transparent surfaces with a quiet one-pixel border. Use for replace, pagination, menus, and utilities; never let them rival the primary action.

### Chips

- **Style:** Compact 6px badges with Geist Mono labels. Neutral metadata uses Soft Surface; healthy status uses Success Surface; caution and deprecated states use their semantic tints.
- **State:** Selected filters may gain a lilac indicator or stronger border. Always preserve a readable text label.

### Cards / Containers

- **Corner Style:** Gently rounded major panels (14px) and nested blocks (8–10px).
- **Background:** White working surfaces over Milk; Soft Surface for nested summaries; the spectral gradient only for orientation or file/publish destination emphasis.
- **Shadow Strategy:** Flat by default, with Ambient Panel reserved for principal work surfaces.
- **Border:** One-pixel Quiet Border; gradient panels may use a lilac-biased quiet border.
- **Internal Padding:** 24px for major work panels, 16px for nested blocks, and 12px for compact rows.

### Inputs / Fields

- **Style:** Milk or white fill, graphite value, 8px corners, one-pixel Standard Border, and 12–14px horizontal padding. Every visible label is explicitly associated with its input, select, textarea, or file control.
- **Focus:** Border shifts toward Workspace Lilac with a subtle two-pixel outline; the field does not glow.
- **Error / Disabled:** Error adds a low-chroma danger border and explicit helper copy; invalid controls set `aria-invalid` and reference that copy with `aria-describedby`. Disabled fields retain readable values and lower contrast without disappearing.

### Navigation

The sidebar uses icon-and-label rows with 12–16px horizontal padding and 8px corners. Default items are transparent with Muted Ink; hover introduces Milk; active introduces Soft Surface, Graphite text, and a narrow lilac marker on the right. Section labels use compact uppercase Geist Mono. The top utility bar keeps breadcrumbs left, search and utilities right, and no ornamental decoration. On mobile, navigation is a 244px off-canvas drawer with explicit open, close, scrim-dismiss, and close-after-navigation behavior.

### Gradient Page Header

The signature admin component is a broad pale blue-to-rose header with 14px corners and a one-pixel lilac-biased border. It combines page title, short operational guidance, count or step metadata, and the most relevant action/status cluster. Keep the gradient low-contrast enough that graphite text remains effortless to read.

### Builds Table

The table is a dense white surface with a Soft Surface header, quiet row dividers, compact mono column labels, and approximately 72px rows. Stack version/build code and project/package name to preserve scanning. Desktop keeps the full comparison set; mobile Builds shows Version, Environment, and Status. Generic tables use a focusable region labeled “Таблица данных” for horizontal scrolling. Interactive rows are real keyboard targets with a descriptive accessible label, a visible focus state, and equivalent activation on click, Enter, and Space.

### Upload Workspace

The upload screen pairs a fluid form card with a narrower publication summary. The uploaded APK appears in a softly gradient file block that supports both file selection and actual drag/drop. The file input accepts `.apk`, and both picked and dropped files are validated by extension with explicit error feedback. Metadata fields follow in a two-column grid, and the summary keeps destination, extracted metadata, completion state, and the publish action visible together. The summary is a decision aid, not a duplicate form.

### Feedback & Progress

Authentication and upload success messages use polite `status` live regions; failures use assertive `alert` semantics. Upload progress exposes a named `progressbar` with current, minimum, and maximum values while retaining visible stage text and percentage. Visual animation supplements these semantics and never replaces them.

## Do's and Don'ts

### Do:

- **Do** keep the 244px sidebar, 68px utility bar, and shared 32px page gutter aligned as one workspace.
- **Do** reserve Graphite for high-emphasis text and the single primary action in each decision area.
- **Do** keep upload visible from the builds screen and publish visible beside the final upload summary.
- **Do** use compact Geist Mono metadata to make versions, build codes, channels, environments, and roles easy to compare.
- **Do** pair every semantic tint with explicit status text and sufficient contrast.
- **Do** keep tables dense and forms structured without shrinking controls below accessible pointer targets.
- **Do** make mobile login authentication-first, then restore the editorial panel only when the desktop split layout has room.
- **Do** preserve Version, Environment, and Status as the mobile Builds scan set; label and focus generic horizontal table regions.
- **Do** make interactive rows operable by click, Enter, and Space with a visible focus state and descriptive accessible label.
- **Do** explicitly associate every field label and connect invalid fields to their error copy with `aria-invalid` and `aria-describedby`.
- **Do** expose authentication and upload feedback through `status` or `alert` live regions and upload progress through `progressbar` semantics.
- **Do** support real APK drag/drop and validate `.apk` files regardless of whether they are picked or dropped.
- **Do** maintain a 44px minimum touch target for mobile controls.

### Don't:

- **Don't** turn the gradient into decoration on every card, control, or table row; it is a structural orientation device.
- **Don't** use pill-shaped controls as the default form language or exceed 14px corners on ordinary panels.
- **Don't** add heavy shadows, glossy effects, glassmorphism, or high-saturation dashboard color.
- **Don't** hide core release metadata behind drawers when it can be scanned in the primary table or summary.
- **Don't** use Funnel Sans for dense data or Geist Mono for prose.
- **Don't** scatter multiple dark primary buttons across the same decision area.
- **Don't** make a table row pointer-only or hide focus when the row opens build details.
- **Don't** rely on a visual error tint, progress fill, or loading label without the corresponding accessible relationship or live semantics.
- **Don't** style a file block as a drop zone unless dropping a file actually works and runs the same APK validation as the picker.
