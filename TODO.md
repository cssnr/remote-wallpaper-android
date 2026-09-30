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

### WidgetRefreshActivity.kt - wallpaper-change restart is best effort

`startRefresh(intent, relaunched = savedInstanceState != null)` is verified to work: the
relaunch Android schedules after a wallpaper change goes through `ActivityThread`
`handleRelaunchActivityInner()` -> `callActivityOnStop(saveState = true)` ->
`callActivityOnSaveInstanceState()` -> `r.state = new Bundle()`, so `savedInstanceState` is a
non-null (possibly empty) Bundle on the relaunch, and null on a genuine tap.

What is NOT guaranteed is that the restart happens at all. The restart is an asset-path
configuration change, and reporting on it found the wallpaper change leaving the activity alone
roughly one time in ten. A missed restart is harmless - nothing re-enters, the guard just sits
unused until the next recreation of any kind - but it means the guard cannot be relied on as
the only thing preventing a duplicate download.

Also note the flag is "was recreated", not "the wallpaper changed". Rotation, night mode, font
scale and locale all take the same branch and simply finish early, which is correct once the
refresh is done.

CONFIRM on device: log inside the `relaunched && REFRESH_COMPLETED` branch, tap refresh once on
a lit screen, and check whether "relaunch after a completed refresh" ever appears.

If it never appears the guard is a no-op and a duplicate download can slip through when the
restart lands after the update already finished. Safe to leave as-is: the ETag 304-skips the
second run. Unsafe only for a remote that sends no ETag or Last-Modified, where it could loop.
Fix in that case: suppress re-entry with a short post-completion time window instead of
relying on the bundle.

See [WidgetRefreshActivity.kt](app/src/main/java/org/cssnr/remotewallpaper/widget/WidgetRefreshActivity.kt).

### WidgetRefreshActivity.kt - activity retained for the length of the download

The coroutine `scope` is deliberately never cancelled and captures `this@WidgetRefreshActivity`
for `AppLogs.i` and `flushWidgets()`, and the static `REFRESH` holds that reference from launch
until the job's finally. `finishIfIdle()` can also finish the activity while an attached tap is
still inside `flushWidgets()`. Worst case is a destroyed Activity held for the length of one
download (up to the 2-minute callTimeout).

FIX: pass `applicationContext` to `AppLogs.i` and `updateWidgets`, and keep the coroutine off the
Activity instance.

### WidgetRefreshActivity.kt - updateWidgets called on a hand-made receiver

`flushWidgets()` calls `WidgetProvider().updateWidgets(...)`, instantiating a `BroadcastReceiver`
subclass by hand to reach a plain method that needs no receiver state.

FIX: promote `updateWidgets` to a top-level `internal` function next to `refreshWidgets` in
[WidgetProvider.kt](app/src/main/java/org/cssnr/remotewallpaper/widget/WidgetProvider.kt).

### WidgetRefreshActivity.kt - only the tapped widget is redrawn

`flushWidgets()` redraws the widget ids that asked, so with more than one widget on the home
screen the others keep showing a stale "last updated" time until something else triggers
`WidgetProvider.onReceive(ACTION_APPWIDGET_UPDATE)`, which redraws every widget.

NOT a regression from the WidgetRefreshActivity work - the removed `REFRESH_WIDGET` broadcast
passed `intArrayOf(appWidgetId)` too. Pre-existing.

FIX: redraw all of them in `flushWidgets()` - [WidgetProvider.kt](app/src/main/java/org/cssnr/remotewallpaper/widget/WidgetProvider.kt)
already has a helper that collects every `appWidgetId`.

### HomeFragment.kt:310

DownloadResult.Downloaded wraps a closed OkHttp Response (body consumed in use{} block).
Safe today: callers only read .code/.request.url.

FIX IF: any new caller needs response body.
FIX: store code+url strings instead of Response.
