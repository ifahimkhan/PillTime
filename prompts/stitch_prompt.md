# Stitch Prompt — PillTime

> Paste these into [Google Stitch](https://stitch.withgoogle.com/) in order. Stitch works best with
> the "zoom-out-zoom-in" approach: one broad prompt to set the app's concept and style, then one
> focused prompt per screen so it doesn't have to guess at scope. Each prompt below is kept under
> ~1,500 characters so Stitch doesn't start dropping components.

---

## Prompt 1 — App concept (paste first)

```
Design a mobile app called PillTime — a very simple medication reminder app. The user adds a reminder with a medicine name, optional dosage, and a time of day, and the app notifies them at that exact time every day. There is no login, no social features, no dashboard — just two screens: a reminder list and an add/edit form.

Audience: people managing their own or a family member's medication, including older users, so prioritize legibility and large touch targets over density.

Visual style: clinical-calm, not playful or clinical-cold. Warm off-white background (#F7F5F0), white capsule-shaped cards (#FFFFFF) with a thin warm border (#E5E1D8), deep ink-green text (#1F2A24) for primary copy and a muted sage-grey (#5C6B62) for secondary text. Primary actions use a sage green (#3C6E5C), with a soft sage tint (#DCE8E2) for selected states.

The signature visual idea: everything interactive — buttons, switches, chips, and reminder rows — uses a true pill/capsule shape (fully rounded ends), echoing the medicine the app reminds you about. Reserve a warm terracotta-orange (#E0683C) and its soft tint (#FBE3D8) exclusively for "due now" or overdue states — it should never appear as decoration, only as a functional alert color.

Typography: a rounded geometric sans-serif (like Manrope) for headings, a humanist sans (like Inter) for body text, and a monospace font with tabular figures (like Roboto Mono) specifically for displaying the reminder time, so the time reads with deliberate, clock-like precision.

Platform: Android + iOS mobile, Material 3 influenced but with the capsule-shaped signature style throughout. 8pt spacing grid, fully rounded corners on small components, 16px corners on larger sheets/dialogs.
```

---

## Prompt 2 — Reminder List screen

```
Now design the main screen: "Reminder List", the app's start destination.

Layout: a top app bar titled "PillTime". Below it, a vertical list of capsule-shaped reminder cards. Each card shows the reminder time on the left in tabular monospace digits (e.g. "8:00 AM"), the medicine name and optional dosage stacked in the center (e.g. "Lisinopril" / "10mg, 1 tablet"), and an active/inactive toggle switch on the right. Cards have a white background with a thin warm border and full pill-shaped corners.

Show one card in its normal state, one card in its "due now" state (background shifts to the soft terracotta tint #FBE3D8 with a small terracotta dot indicator near the time), and one card that's toggled off/inactive (switch off, text slightly muted).

Add a floating action button in the bottom right, sage green (#3C6E5C), pill-shaped, with a "+" icon, for adding a new reminder.

Above the list, include a dismissible capsule-shaped banner in the soft terracotta tint warning "Without this permission, reminders may arrive late." with a text button "Open settings" — this only appears when the Android exact-alarm permission is missing, but show it in this mockup so I can see the state.

Also generate the empty state version of this same screen: centered illustration of an empty pill capsule outline, muted text "No reminders yet. Add the first one to get started.", and the same primary FAB.
```

---

## Prompt 3 — Add/Edit Reminder screen

```
Now design the second screen: "Add Reminder" (a form, reached from the FAB on the Reminder List screen).

Layout: top app bar with a back arrow and title "Add Reminder". Below it, a vertical form on the warm off-white background:
1. A text field labeled "Medicine name" (required) — rounded rectangle input, sage-green focus outline.
2. A text field labeled "Dosage" with placeholder text "e.g. 1 tablet, 10mg" (optional).
3. A large time picker section labeled "Remind me at" showing a big tabular-monospace time display (e.g. "8:00 AM") with a scrollable hour/minute wheel picker below it, sage green selection indicator.
4. A full-width pill-shaped primary button at the bottom, sage green fill, white text, labeled "Save reminder". Show it in its enabled state and, as a smaller second version, its disabled/greyed-out state (used when the medicine name field is empty).

Keep the form to exactly these elements — no repeat-day selector, no extra settings, this is intentionally a one-screen-one-job form. Same capsule/pill visual language and typography as the Reminder List screen (Manrope headings, Inter body, Roboto Mono for the time digits).
```

---

## Tips for using these in Stitch

- Paste Prompt 1 first and let it generate before moving to Prompt 2 — Stitch carries the established style forward into later screens in the same project.
- If a generated screen drifts from the palette, reply with the specific hex codes again rather than re-describing the whole concept — Stitch responds better to "use #3C6E5C for the button" than to a restated paragraph.
- Use Stitch's multi-select (Shift+Click) across both screens once generated, then send a short theme-consistency prompt ("make the time typography identical on both screens") to lock them together.
