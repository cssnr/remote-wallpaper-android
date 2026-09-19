# TODO

- Replace Toast with Snackbar

## Requires Migration

### Store LogLevel by Name

`LogEntry.level` uses `LogLevel.ordinal` (Int) - breaks if the enum is reordered.

FIX:

```kotlin
@Entity
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val level: LogLevel,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
)
```

- remove the `levelEnum` getter, update the insert and `levelEnum.name` call sites
- destructive migration: `LogDatabase` v2 + `.fallbackToDestructiveMigration(true)`

See [AppLogs.kt](app/src/main/java/org/cssnr/remotewallpaper/log/AppLogs.kt).

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
