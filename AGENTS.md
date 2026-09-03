# Agent Guide

Android application to update wallpaper from a remote URL on an interval.

- `app/` - Android app source
- `gradle/libs.versions.toml` - Library versions
- `Taskfile.yml` - [task](https://github.com/go-task/task) commands
- `docs/` - [Zensical](https://github.com/zensical/zensical) docs

## Android

- applicationId = org.cssnr.remotewallpaper.dev
- minSdk = 26
- targetSdk = 36
- compileSdk = 37

## Commands

ALWAYS use the `task *` commands

| Command        | Purpose                                  |
| -------------- | ---------------------------------------- |
| `task lint`    | Gradle Lint - DO NOT RUN                 |
| `task compile` | Compile Kotlin - DO NOT truncate output  |
| `task debug`   | Build debug variant (APK)                |
| `task release` | Build release variant (APK)              |
| `task bundle`  | Build Android App Bundle (AAB)           |
| `task check`   | Prettier check (check non-kotlin files)  |
| `task format`  | Prettier write (format non-kotlin files) |

Do NOT run task compile/debug/release/bundle every turn unless it is REQUIRED!!!

## Testing

To test on a device use the `adb` command. If no devices are running and attached, ask the user to do this!

DO NOT uninstall the application to clear data, use: `adb shell pm clear`

For testing 304's you can use any image returned by: https://images.cssnr.com/aviation

Examples:

- https://smashedr.github.io/random-image/aviation/33782_1539281580.jpg
- https://smashedr.github.io/random-image/aviation/61919_1467456365.jpg
