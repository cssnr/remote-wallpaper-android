# TODO

- Replace Toast with Snackbar

## Requires Migration

### Store LogLevel as String

Log levels are stored as `LogLevel.ordinal` (Int) in the Room `LogDatabase`.
If the enum is reordered or a new level is inserted, previously stored entries
will silently map to the wrong level.

FIX: Store `LogLevel.name` (String) instead. Requires a `LogDatabase` v1→v2
migration to convert existing ordinal values to names on installed devices.
See [AppLogs.kt](app/src/main/java/org/cssnr/remotewallpaper/log/AppLogs.kt).

Note: This "should" be a destructive migration.

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
