BruniO RP98 — Small initial Live TV category and last-channel priority
Version code 22202 / version name 2.0.0-rp98
Apply over RP86–RP97 preserving paths. Includes previous patches.

- Last remembered Live TV channel always has priority when it still exists in the active profile.
- Restores by stable ID first, then a unique normalized name after catalog migration/ID changes.
- When there is no remembered/matching channel, starts in Noticias (7 entries in the supplied file).
- If a later M3U update makes Noticias larger than 24 entries or removes it, selects the smallest group.
- An active/remembered channel keeps its category/context, rather than forcing News over it.
- All/Todos is available only when selected; initial category selection does not load that full grid.
- Metadata index is read/cached locally; lazy grids/lists create visible cards. No bulk opening of
  channel streams, no probing of all 2,662 URLs, and no upfront download of all channel logos.
- Removed/unmatched channel records fall back to the small category; a last URL not present in the current
  list is not reused from the old record. Provider errors during playback still use the existing UI.

Validation: 4 standalone checks passed for Noticias selection/count, smallest-group fallback and
empty input. Changed Kotlin syntax checked. No APK built, no full Android compile/TV validation.
No new SQL. Previous RP97 catalog/QR/LIDOM exceptions and all other features included.
