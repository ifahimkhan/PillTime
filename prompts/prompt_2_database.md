# Prompt 2 — Local Database (SQLDelight)

> **Before starting**: Read `CLAUDE.md` for full project context. Do not proceed without it.

---

## Role

You are a senior Kotlin Multiplatform engineer working with SQLDelight for local-only persistence.

---

## Goal

A working `reminder` table exists via SQLDelight, with generated typed queries for creating, listing, updating, deactivating, and deleting reminders, ready for `ReminderRepository` to consume.

---

## Context Snapshot

- **Stack**: SQLDelight (multiplatform), no remote DB, no RLS (single local user)
- **Relevant existing files**: `sqldelight/com/pilltime/db/` (created empty in prompt 1)
- **Upstream dependencies**: prompt 1 (project scaffold) must be complete
- **Downstream consumers**: `prompt_3_notifications.md` (reads reminder data to reschedule alarms), `prompt_4_state.md` (`ReminderRepository` wraps these queries)

---

## Task Breakdown

1. **Inspect first**
   - Confirm `sqldelight/com/pilltime/db/` exists and is empty.
2. **Create `Reminder.sq`** with the table definition below.
3. **Add typed queries** for all operations listed in Detailed Requirements.
4. **Validate**
   - SQLDelight code generation succeeds with no errors.
   - Every query needed by the repository layer (prompt 4) exists.

---

## Detailed Requirements

### Table: `reminder`

```sql
CREATE TABLE reminder (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    medicine_name   TEXT NOT NULL,
    dosage          TEXT,
    hour            INTEGER NOT NULL,   -- 0-23, local device time
    minute          INTEGER NOT NULL,   -- 0-59
    is_active       INTEGER NOT NULL DEFAULT 1,  -- boolean: 1 = active, 0 = paused
    created_at      INTEGER NOT NULL    -- epoch millis
);
```

Keep this v1 schema to **one reminder = one time per day, every day** (no per-weekday selection). This matches the "very basic" scope. Note in a code comment that a `days_mask INTEGER` column could be added later for per-weekday repeats without breaking existing rows (default it to "every day").

### Queries needed (`Reminder.sq`)

- `insertReminder` — insert a new row, return nothing (use `lastInsertRowId` from the driver to get the new id back in the repository)
- `selectAll` — all reminders ordered by `hour, minute`
- `selectActive` — only `is_active = 1` reminders (used to reschedule alarms on boot)
- `selectById` — single reminder by id
- `updateReminder` — update medicine_name, dosage, hour, minute by id
- `setActive` — toggle `is_active` by id
- `deleteReminder` — delete by id

---

## Output Specification

| Action | File path | Contents |
|--------|-----------|----------|
| CREATE | `sqldelight/com/pilltime/db/Reminder.sq` | Table definition + all queries listed above |

Show only this file.

---

## Constraints

- Do NOT add any other tables — this app needs exactly one.
- Do NOT add a `user_id` or any ownership column — this is a single-user local database.
- Do NOT modify files outside `sqldelight/com/pilltime/db/`.
- Keep explanations to 2–3 sentences maximum.

---

## Success Criteria

- [ ] `reminder` table exists with exactly the columns specified
- [ ] All 7 queries listed exist and compile via SQLDelight codegen
- [ ] No extra tables or unused columns were added
- [ ] `is_active` defaults to `1` (active) on insert
