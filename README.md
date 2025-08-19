[![GitHub Downloads](https://img.shields.io/github/downloads/cssnr/remote-wallpaper-android/total?logo=android)](https://github.com/cssnr/remote-wallpaper-android/releases/latest/download/app-release.apk)
[![GitHub Release Version](https://img.shields.io/github/v/release/cssnr/remote-wallpaper-android?logo=github)](https://github.com/cssnr/remote-wallpaper-android/releases/latest)
[![Lint](https://img.shields.io/github/actions/workflow/status/cssnr/remote-wallpaper-android/lint.yaml?logo=github&logoColor=white&label=lint)](https://github.com/cssnr/remote-wallpaper-android/actions/workflows/lint.yaml)
[![Release](https://img.shields.io/github/actions/workflow/status/cssnr/remote-wallpaper-android/release.yaml?logo=github&logoColor=white&label=release)](https://github.com/cssnr/remote-wallpaper-android/actions/workflows/release.yaml)
[![AGP Version](https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fcssnr%2Fremote-wallpaper-android%2Frefs%2Fheads%2Fmaster%2Fgradle%2Flibs.versions.toml&query=%24.versions.agp&logo=gradle&label=AGP)](https://github.com/cssnr/remote-wallpaper-android/blob/master/gradle/libs.versions.toml#L2)
[![GitHub Last Commit](https://img.shields.io/github/last-commit/cssnr/remote-wallpaper-android?logo=github&label=updated)](https://github.com/cssnr/remote-wallpaper-android/graphs/commit-activity)
[![GitHub Repo Size](https://img.shields.io/github/repo-size/cssnr/remote-wallpaper-android?logo=bookstack&logoColor=white&label=repo%20size)](https://github.com/cssnr/remote-wallpaper-android)
[![GitHub Top Language](https://img.shields.io/github/languages/top/cssnr/remote-wallpaper-android?logo=htmx)](https://github.com/cssnr/remote-wallpaper-android)
[![GitHub Discussions](https://img.shields.io/github/discussions/cssnr/remote-wallpaper-android?logo=github)](https://github.com/cssnr/remote-wallpaper-android/discussions)
[![GitHub Forks](https://img.shields.io/github/forks/cssnr/remote-wallpaper-android?style=flat&logo=github)](https://github.com/cssnr/remote-wallpaper-android/forks)
[![GitHub Repo Stars](https://img.shields.io/github/stars/cssnr/remote-wallpaper-android?style=flat&logo=github)](https://github.com/cssnr/remote-wallpaper-android/stargazers)
[![GitHub Org Stars](https://img.shields.io/github/stars/cssnr?style=flat&logo=github&label=org%20stars)](https://cssnr.com/)
[![Discord](https://img.shields.io/discord/899171661457293343?logo=discord&logoColor=white&label=discord&color=7289da)](https://discord.gg/wXy6m2X8wY)
[![Ko-fi](https://img.shields.io/badge/Ko--fi-72a5f2?logo=kofi&label=support)](https://ko-fi.com/cssnr)

# Remote Wallpaper Android

[![GitHub Release](https://img.shields.io/github/v/release/cssnr/remote-wallpaper-android?style=for-the-badge&logo=android&label=Download%20Android%20APK&color=3ddc84)](https://github.com/cssnr/remote-wallpaper-android/releases/latest/download/app-release.apk)

- [Install](#Install)
  - [Setup](#Setup)
- [Features](#Features)
  - [Planned](#Planned)
  - [Known Issues](#Known-Issues)
- [Screenshots](#Screenshots)
- [Support](#Support)
- [Development](#Development)
  - [Android Studio](#Android-Studio)
  - [Command Line](#Command-Line)
- [Contributing](#Contributing)

Remote Wallpaper Android Application. Set wallpaper from remote URL's at a defined interval.
Supports any link that redirects to an image.

Example Remotes:

- https://picsum.photos/4800/2400
- https://images.cssnr.com/aviation

## Install

[![Get on GitHub](https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/android/get80/github.png)](https://github.com/cssnr/remote-wallpaper-android/releases/latest/download/app-release.apk)
[![Get on Obtainium](https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/android/get80/obtainium.png)](https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/cssnr/remote-wallpaper-android)

<details><summary>📲 Click to View QR Codes 📸 Supports Android 8 (API 26) 2017 +</summary>

[![QR Code](https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/remote-wallpaper/qr-code-github.png)](https://github.com/cssnr/remote-wallpaper-android/releases/latest/download/app-release.apk)

[![QR Code](https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/remote-wallpaper/qr-code-obtainium.png)](https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/cssnr/remote-wallpaper-android/)

</details>

_Note: If installing directly, you may need to allow installation of apps from unknown sources.  
For more information, see [Release through a website](https://developer.android.com/studio/publish#publishing-website)._

<details><summary>View Manual Steps to Install from Unknown Sources</summary>

Note: Downloading and Installing the [apk](https://github.com/cssnr/remote-wallpaper-android/releases/latest/download/app-release.apk)
should take you to the settings area to allow installation if not already enabled. Otherwise:

1. Go to your device settings.
2. Search for "Install unknown apps" or similar.
3. Choose the app you will install the apk file from.
   - Select your web browser to install directly from it.
   - Select your file manager to open it, locate the apk and install from there.
4. Download the [Latest Release](https://github.com/cssnr/remote-wallpaper-android/releases/latest/download/app-release.apk).
5. Open the download apk in the app you selected in step #3.
6. Choose Install and Accept any Play Protect notifications.
7. The app is now installed. Proceed to the [Setup](#Setup) section below.

</details>

### Setup

1. [Install](#Install) and open the app on your device.
2. Select your update interval and initial remote.
3. Click Set wallpaper and Start to get the ball rolling.
4. Or Just Start the App and add a remote from the Remotes.
5. Optionally, add the Widget to refresh from the home screen.

## Features

- Add any remote that redirects to a random image.
- Automatically rotate at configurable intervals.
- Widget with refresh button and updated time.
- History of all wallpaper used with links.

### Planned

- Image Effects; Blur, Grayscale, etc.
- JSON Result Parser with Custom keys.

- [Submit a Feature Request](https://github.com/cssnr/remote-wallpaper-android/discussions/categories/feature-requests)

### Known Issues

- [Open an Issue](https://github.com/cssnr/remote-wallpaper-android/issues)

## Screenshots

<a title="Screenshot" href="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/remote-wallpaper/screenshots/1.jpg">
    <img alt="Screenshot" src="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/remote-wallpaper/screenshots/1.jpg"></a>
<a title="Screenshot" href="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/remote-wallpaper/screenshots/2.jpg">
    <img alt="Screenshot" src="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/remote-wallpaper/screenshots/2.jpg"></a>
<a title="Screenshot" href="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/remote-wallpaper/screenshots/3.jpg">
    <img alt="Screenshot" src="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/remote-wallpaper/screenshots/3.jpg"></a>
<a title="Screenshot" href="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/remote-wallpaper/screenshots/4.jpg">
    <img alt="Screenshot" src="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/remote-wallpaper/screenshots/4.jpg"></a>
<a title="Screenshot" href="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/remote-wallpaper/screenshots/5.jpg">
    <img alt="Screenshot" src="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/remote-wallpaper/screenshots/5.jpg"></a>
<a title="Screenshot" href="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/remote-wallpaper/screenshots/6.jpg">
    <img alt="Screenshot" src="https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/remote-wallpaper/screenshots/6.jpg"></a>

## Support

For general help or to request a feature, see:

- Q&A Discussion: https://github.com/cssnr/remote-wallpaper-android/discussions/categories/q-a
- Request a Feature: https://github.com/cssnr/remote-wallpaper-android/discussions/categories/feature-requests

If you are experiencing an issue/bug or getting unexpected results, you can:

- Report an Issue: https://github.com/cssnr/remote-wallpaper-android/issues
- Chat with us on Discord: https://discord.gg/wXy6m2X8wY
- Provide General Feedback: [https://cssnr.github.io/feedback/](https://cssnr.github.io/feedback/?app=Remote%20Wallpaper%20Android)

# Development

This section briefly covers running and building in [Android Studio](#Android-Studio) and the [Command Line](#Command-Line).

## Android Studio

1. Download and Install Android Studio.

https://developer.android.com/studio

2. Ensure that usb or wifi debugging is enabled in the Android developer settings and verify.

3. Then build or run the app on your device.
   - Import the Project
   - Run Gradle Sync

To Run: Select a device and press Play ▶️

To Build:

- Select the Build Variant (debug or release)
- Build > Generate App Bundles or APK > Generate APKs

## Command Line

_Note: This section is a WIP! For more details see the [release.yaml](.github/workflows/release.yaml)._

You will need to have [ADB](https://developer.android.com/tools/adb) installed.

1. Download and Install the Android SDK Platform Tools.

https://developer.android.com/tools/releases/platform-tools#downloads

Ensure that `adb` is in your PATH.

2. List and verify the device is connected with:

```shell
$ adb devices
List of devices attached
RF9M33Z1Q0M     device
```

3. Build a debug or release apk.

```shell
./gradlew assemble
./gradlew assembleRelease
```

_Note: Use `gradlew.bat` for Windows._

4. Then install the apk to your device with adb.

```shell
$ cd app/build/outputs/apk/debug
$ adb -s RF9M33Z1Q0M install app-debug.apk
```

```shell
$ cd app/build/outputs/apk/release
$ adb -s RF9M33Z1Q0M install app-release-unsigned.apk
```

_Note: you may have to uninstall before installing due to different certificate signatures._

For more details, see the [ADB Documentation](https://developer.android.com/tools/adb#move).

# Contributing

Please consider making a donation to support the development of this project
and [additional](https://cssnr.com/) open source projects.

[![Ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/cssnr)

You can also star this project on GitHub and support other related projects:

- [Django Files Android](https://github.com/django-files/android-client?tab=readme-ov-file#readme)
- [Zipline Android](https://github.com/cssnr/zipline-android?tab=readme-ov-file#readme)
- [NOAA Weather Android](https://github.com/cssnr/noaa-weather-android?tab=readme-ov-file#readme)
- [Remote Wallpaper Android](https://github.com/cssnr/remote-wallpaper-android?tab=readme-ov-file#readme)
- [Tibs3DPrints Android](https://github.com/cssnr/tibs3dprints-android?tab=readme-ov-file#readme)

For a full list of current projects visit: [https://cssnr.github.io/](https://cssnr.github.io/)
