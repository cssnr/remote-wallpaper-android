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

### WidgetRefreshActivity.kt - relaunch guard is unverified

`startRefresh(intent, relaunched = savedInstanceState != null)` assumes the restart that
Android 12+ schedules after a wallpaper change passes a non-null `savedInstanceState`. The
restart is documented as being scheduled "via the regular life-cycle", which is the
config-change path, but that has not been confirmed on a device.

CONFIRM: log inside the `relaunched && REFRESH_COMPLETED` branch, tap refresh once on a lit
screen, and check whether "relaunch after a completed refresh" ever appears.

If it never appears the guard is a no-op and a duplicate download can slip through when the
restart lands after the update already finished. Safe to leave as-is: the ETag 304-skips the
second run. Unsafe only for a remote that sends no ETag or Last-Modified, where it could loop.
Fix in that case: suppress re-entry with a short post-completion time window instead of
relying on the bundle.

See [WidgetRefreshActivity.kt](app/src/main/java/org/cssnr/remotewallpaper/widget/WidgetRefreshActivity.kt).

### WidgetRefreshActivity.kt - only the tapped widget is redrawn

`flushWidgets()` redraws the widget ids that asked, so with more than one widget on the home
screen the others keep showing a stale "last updated" time until something else triggers
`WidgetProvider.onReceive(ACTION_APPWIDGET_UPDATE)`, which redraws every widget.

FIX: redraw all of them in `flushWidgets()` - [WidgetProvider.kt](app/src/main/java/org/cssnr/remotewallpaper/widget/WidgetProvider.kt)
already has a helper that collects every `appWidgetId`.

### HomeFragment.kt:310

DownloadResult.Downloaded wraps a closed OkHttp Response (body consumed in use{} block).
Safe today: callers only read .code/.request.url.

FIX IF: any new caller needs response body.
FIX: store code+url strings instead of Response.
