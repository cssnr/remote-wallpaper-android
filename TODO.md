# TODO

- Replace Toast with Snackbar

## Requires Migration

### Store LogLevel as String

Log levels are stored as `LogLevel.ordinal` (Int) in the Room `LogDatabase`.
If the enum is reordered or a new level is inserted, previously stored entries
will silently map to the wrong level.

FIX: Store `LogLevel.name` (String) instead. The column changes type (INTEGER →
TEXT), so pick one:

- Destructive: bump `LogDatabase` to v2, do NOT add a `MIGRATION_1_2`, and set
  `.fallbackToDestructiveMigration(true)`. Room drops and recreates the table, so
  existing logs are lost - acceptable since logs purge after 7 days.

- Preserve logs: requires a "very manual" table-rebuild migration. A plain
  `UPDATE` leaves the column with INTEGER affinity and fails Room's post-migration
  schema validation, so the table must be recreated:

See [AppLogs.kt](app/src/main/java/org/cssnr/remotewallpaper/log/AppLogs.kt).

**Note: This "should" be a destructive migration.**

## Required Fixes

### Intent.ACTION_VIEW

All `startActivity` calls to `Intent.ACTION_VIEW` will crash if a browser is not installed.
See call sites in [SettingsFragment.kt](app/src/main/java/org/cssnr/remotewallpaper/ui/settings/SettingsFragment.kt).

Example:

```kotlin
startActivity(Intent(Intent.ACTION_VIEW, getString(R.string.acra_info_link).toUri()))
```

FIX:

```kotlin
try {
    startActivity(Intent(Intent.ACTION_VIEW, getString(R.string.acra_info_link).toUri()))
} catch (e: Exception) {
    Log.w("SettingsFragment", "openAcraDocs failed: $e")
}
```

## Known Issues

### HomeFragment.kt:310

DownloadResult.Downloaded wraps a closed OkHttp Response (body consumed in use{} block).
Safe today: callers only read .code/.request.url.

FIX IF: any new caller needs response body.
FIX: store code+url strings instead of Response.
