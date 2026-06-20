# Design.md — PillTime

> Visual design specification for the PillTime app. This file is the source of truth for
> the design tokens and screen specs used to generate UI in Google Stitch (see `stitch_prompt.md`).

---

## Concept

The entire job of this app is one promise: tell you the right pill at the right exact minute. Most reminder apps bury that promise under generic to-do-list chrome. PillTime's signature is the **capsule** — the literal shape of the thing the app exists to remind you about. Every actionable surface (reminder rows, the time itself, the primary button) is built on a true pill/capsule silhouette, so the shape of the UI echoes the shape of the medicine.

The palette stays clinical-calm rather than playful: this is read by people managing their own or a family member's health, often under stress, sometimes older or visually fatigued. Contrast and legibility outrank decoration everywhere except one place — the moment a dose is actually due, which gets the only saturated color in the system. Color carries meaning here, not mood.

---

## Color Tokens

| Token | Hex | Usage |
|-------|-----|-------|
| `color-background` | `#F7F5F0` | App background — warm off-white, not clinical white |
| `color-surface` | `#FFFFFF` | Cards, reminder rows, sheets |
| `color-ink` | `#1F2A24` | Primary text — deep ink-green-black, high contrast |
| `color-ink-muted` | `#5C6B62` | Secondary text, dosage subtext, helper copy |
| `color-primary` | `#3C6E5C` | Sage green — primary actions, active switches, FAB |
| `color-primary-soft` | `#DCE8E2` | Selected/active row backgrounds, chip fills |
| `color-due` | `#E0683C` | Reserved exclusively for "due now" / overdue states — never decorative |
| `color-due-soft` | `#FBE3D8` | Background for due-now banners and badges |
| `color-border` | `#E5E1D8` | Hairline dividers, card outlines |

Rule: `color-due` and `color-due-soft` may only appear when a reminder is actively due or overdue, or on the permission-warning banner. If it shows up anywhere else, that's a bug in the design, not a style choice.

---

## Typography

| Role | Typeface | Weight/notes |
|------|---------|---------------|
| Display (screen titles) | Manrope | SemiBold, slightly rounded geometric sans — echoes the capsule shape |
| Body (labels, copy) | Inter | Regular/Medium — humanist, highly legible at small sizes |
| Time display (the reminder clock) | Roboto Mono or IBM Plex Mono | Medium, **tabular figures** — the one place precision should *look* precise |

Type scale: Display 28/34, Section heading 18/24, Body 16/22, Caption 13/18, Time display 32/36 (tabular).

---

## Layout & Spacing

- 8pt spacing grid (4 / 8 / 16 / 24 / 32).
- Corner radius: **full pill radius** (999px) on buttons, chips, switches, and reminder row containers. 16px radius on larger sheets/dialogs (the only place a true capsule shape would look odd).
- Reminder rows: capsule-shaped card, time on the left in mono tabular figures, medicine name + dosage stacked center, active/inactive `Switch` on the right.
- Minimum touch target 48dp everywhere — this audience cannot be assumed to have precise motor control.

---

## Components

- **Reminder row (capsule card)**: pill-shaped container, `color-surface` background, `color-border` 1px outline, time block in mono font on the left, switch on the right. When a reminder is currently due, the entire row's background shifts to `color-due-soft` with a small dot in `color-due`.
- **Primary button**: full-pill radius, `color-primary` fill, white label, used for "Save reminder" and the FAB.
- **Time picker**: large tabular-mono digit wheels/dial, `color-primary` selection indicator.
- **Permission banner** (Android exact-alarm warning): capsule-shaped banner, `color-due-soft` background, `color-due` icon, single text button "Open settings."
- **Empty state**: centered capsule-outline illustration (an empty pill capsule), `color-ink-muted` supporting text, single primary button "Add your first reminder."

---

## Voice & Copy

- Active voice, plain verbs: "Add reminder," not "Submit." "Save changes," not "Confirm."
- The button label and the resulting state use the same word: "Add reminder" → confirmation reads "Reminder added," never "Successfully submitted."
- Empty states are an invitation, not an apology: "No reminders yet. Add the first one to get started." — not "You have no data."
- The exact-alarm permission banner states the consequence plainly: "Without this permission, reminders may arrive late." No jargon like "background execution" or "Doze mode."

---

## Screens (for Stitch generation)

1. **Reminder List** — primary screen, start destination. States: empty, populated, populated-with-one-due-now, populated-with-permission-banner.
2. **Add/Edit Reminder** — form sheet/screen: medicine name, optional dosage, time picker, Save button.

No other screens in v1 — no onboarding, no settings, no history log.
