# TODO

- Document Application Logs
- Replace Toast with Snackbar

## ACRA Release

- Add `Crash Reporting` to the [docs](docs)
- Update `acra_info_link` in [strings.xml](app/src/main/res/values/strings.xml)

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

### HomeFragment.kt:192

showAddDialog ignores DownloadResult; a 304 would still toast "Done.".
Unreachable in practice: request sends no validators (etag=null) and client has no Cache.

STATUS: Intentionally not handled - unreachable dead branch.
