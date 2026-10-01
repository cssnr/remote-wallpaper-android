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

### WidgetRefreshActivity.kt - the relaunch guard never fires in practice

`startRefresh(intent, relaunched = savedInstanceState != null)` is verified to work as a
mechanism: the relaunch Android schedules goes through `ActivityThread`
`handleRelaunchActivityInner()` -> `callActivityOnStop(saveState = true)` ->
`callActivityOnSaveInstanceState()` -> `r.state = new Bundle()`, so `savedInstanceState` is a
non-null (possibly empty) Bundle on the relaunch, and null on a genuine tap.

Confirmed on device 2026-09-30: the guard never fires. With a slow remote the asset-path
change that drives the restart arrived ~1.4s after the wallpaper was set, by which time the
activity had already finished and its empty task was gone:

```
17:01:37.260  WallpaperManager   Wallpaper set completion.
17:01:37.460  DONE / flushWidgets: [11, 12]
17:01:38.065  VRI[WidgetRefreshActivity]  visibilityChanged true->false
17:01:38.889  ActivityThread  ApplicationInfo updating ... assets removed/add
```

The guard is correct but dead in this timing. It is also best effort in general - the restart is
an asset-path configuration change, and reporting on it found the wallpaper change leaving the
activity alone roughly one time in ten.

Do not rely on the ETag as a safety net: the remote tested (picsum.photos) returned
`etag=null, lastModified=null`, so a missed restart would produce a full duplicate download with
no conditional-request protection.

FIX if duplicates ever appear: suppress re-entry with a short post-completion time window
instead of relying on the bundle.

Also note the flag is "was recreated", not "the wallpaper changed". Rotation, night mode, font
scale and locale all take the same branch and simply finish early, which is correct once the
refresh is done.

See [WidgetRefreshActivity.kt](app/src/main/java/org/cssnr/remotewallpaper/widget/WidgetRefreshActivity.kt).

### WidgetRefreshActivity.kt - activity retained for the length of the download

The explicit `this@WidgetRefreshActivity` references are gone - the work reads through
`applicationContext` - but the coroutine still reaches the instance implicitly through
`flushWidgets()` and `finishIfIdle()`, and the static `REFRESH` holds the job from launch until
its finally. `finishIfIdle()` can also finish the activity while an attached tap is still inside
`flushWidgets()`. Bounded by the 2-minute callTimeout, so this is a short-lived retention of a
lightweight object rather than an unbounded leak.

FIX: move `scope`, `REFRESH` and `PENDING_WIDGET_IDS` to an application-scoped holder and have
the job take the ids it needs as locals, so nothing captures the Activity. Only worth doing if
the bounded window is not acceptable.

### WidgetRefreshActivity.kt - which constraint the Activity satisfies is not isolated

The Activity is load-bearing - the broadcast + `goAsync()` approach on master does not reliably
update the wallpaper from a widget tap. What has not been pinned down is which constraint that
was, because two candidates are both fixed by this design:

- the `goAsync()` receiver limit (Android docs: the system expects `PendingResult.finish()`
  "very quickly (under 10 seconds)"), which a 30 second download blows through
- `IMPORTANCE_FOREGROUND`, which the code comment claims `setWallpaper()` requires

AOSP master `WallpaperManagerService.isFromForegroundApp()` is
`mActivityManager.getPackageImportance(callingPackage) == IMPORTANCE_FOREGROUND`, but the result
only lands on the wallpaper and the wallpaper-changed broadcast
(`EXTRA_FROM_FOREGROUND_APP`) - `setWallpaper()` never branches on it or throws. The concept is
absent entirely on API 26 and API 29, so it cannot be a long-standing gate. It may still be
enforced by an OEM build, which is the likely reason the broadcast approach failed in practice.

This is only worth resolving if it changes the design. If the receiver limit was the real
constraint, `FLAG_NOT_TOUCHABLE | FLAG_NOT_FOCUSABLE` and the transparent theme are not needed
for the wallpaper to apply and a plain foreground service would be simpler. If foreground
importance was the real constraint, the current design is right and should not be simplified.
Either way the Activity stays.

Also confirmed on device 2026-09-30: `taskAffinity=""` + `singleTask` + `excludeFromRecents` is
not reaped early and produces no Android 12+ splash, but tapping refresh is visibly not free -
a real task goes on top, so systemui recomputes the navigation bar appearance (the 3-button bar
goes partially opaque) and the task is listed in Recents while it is current.

The Recents entry is inherent to launching any activity; `excludeFromRecents` only hides the
task once you navigate away from it. Also confirmed: the manifest's `excludeFromRecents` is
folded into the launch flags by the system (observed `flg=0x10800000` =
NEW_TASK | EXCLUDE_FROM_RECENTS), which is why repeating it on the Intent in `updateWidgets`
was redundant.

FIX if it matters: nothing on the manifest side helps. This needs a different mechanism (a
foreground service) or has to be accepted as the cost of downloading in the foreground.

### WidgetRefreshActivity.kt - updateWidgets called on a hand-made receiver

`flushWidgets()` calls `WidgetProvider().updateWidgets(...)`, instantiating a `BroadcastReceiver`
subclass by hand to reach a plain method that needs no receiver state.

FIX: promote `updateWidgets` to a top-level `internal` function next to `refreshWidgets` in
[WidgetProvider.kt](app/src/main/java/org/cssnr/remotewallpaper/widget/WidgetProvider.kt).

### WidgetRefreshActivity.kt - only the tapped widget is redrawn

`flushWidgets()` redraws the widget ids that asked, so with more than one widget on the home
screen the others keep showing a stale "last updated" time.

Currently masked by `MainActivity.kt:386-393`, whose preference listener calls `refreshWidgets()`
on any `last_update` change and redraws every widget. That only works while `MainActivity` is
alive - from a cold launcher-only state it does not fire.

Observed 2026-09-30 with three widgets on the home screen: four taps (two each on widgets 11 and
12, three taps inside 0.86s) produced three lifecycle callbacks - `onCreate` plus two
`onNewIntent`, both attaching to the in-flight update - and one download. `PENDING_WIDGET_IDS`
correctly ended as `[11, 12]`; widget 10 was never tapped and was only updated by the
preference-listener masking above.

UNVERIFIED: one of the four taps produced no lifecycle callback at all. Harmless here if the
dropped tap was a duplicate, since discarding a duplicate is the point of the attach path - but
if a first tap can be dropped, a widget tap silently does nothing.

CONFIRM: tap one widget twice with ~3s between the taps against a slow remote and confirm two
callbacks arrive (`onCreate`, then `onNewIntent`). If only one does, rapid widget taps are being
dropped by the system and the widget needs a fallback.

NOT a regression from the WidgetRefreshActivity work - the removed `REFRESH_WIDGET` broadcast
passed `intArrayOf(appWidgetId)` too. Pre-existing.

FIX: drop `PENDING_WIDGET_IDS` entirely and redraw every `appWidgetId`, the way
[WidgetProvider.kt](app/src/main/java/org/cssnr/remotewallpaper/widget/WidgetProvider.kt)
`refreshWidgets()` already collects them. Removes ~20 lines and the shared mutable set.

### HomeFragment.kt:310

DownloadResult.Downloaded wraps a closed OkHttp Response (body consumed in use{} block).
Safe today: callers only read .code/.request.url.

FIX IF: any new caller needs response body.
FIX: store code+url strings instead of Response.
