---
icon: lucide/tablet-smartphone
---

# :lucide-tablet-smartphone: Usage

[![Remote Wallpaper Android](assets/images/logo.png){ align=right width=96 }](https://github.com/cssnr/remote-wallpaper-android?tab=readme-ov-file#readme)

- [Remotes](#remotes)
- [Widget](#widget)
- [Settings](#settings)

## :lucide-router: Remotes

Remotes are the heart of the application. A remote is a link to an image.  
This can be a static image but you most likely have a link to a dynamic image or a redirect.

Therefore, this application will refresh the image at the remote (image) url on a custom interval.

Example Remotes:

- <https://picsum.photos/4800/2400>
- <https://images.cssnr.com/aviation>

## :lucide-layout-panel-top: Widget

You can add a stats widget to the home screen to display info and functionality.

![Stats Widget](https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/remote-wallpaper/docs/widget.jpg){ style="border-radius: 16px;" }

The widget displays the following information:

- Current Remote
- Update Interval
- Last Updated Time

The widget has the following functions:

- Refresh Wallpaper
- Launch Application

## :lucide-settings: Settings

Application Settings:

- Update Interval
- Screens to Update (Home/Lock)

Widget Settings:

- Text Color
- Background Color
- Background Opacity

Application Logs:

- Wallpaper Updates
- Work Manager Runs
- Widget Refreshes

### :lucide-bug: Crash Reporting

Without crash reporting, fixing a bug requires you to:

- Stop what you're doing and open a browser
- Go to the GitHub repo and create an Issue
- Explain exactly what you were doing when the app crashed
- Hope I can re-create the bug myself to get the stack trace

That's a heavy ask for an app that's already broken — it leaves you with a bad experience and
me without enough data to fix it.

To close that gap without compromising your data or privacy, this app uses
[ACRA](https://github.com/ACRA/acra) — an open-source crash reporting library. Reports are received by a
self-hosted [Acrarium](https://github.com/F43nd1r/Acrarium) backend that runs on my own infrastructure,
so crash data doesn't go to any third parties — no Google or other big-data services.

**You can turn crash reporting on or off at any time with a toggle on the Settings page.**

#### What Gets Collected

ACRA only sends reports when the app hits an unhandled crash. By default,
it only sends the technical context needed to diagnose the crash:

- The **stack trace** of the crash, plus the app and Android versions
- Basic **device context** — e.g. the device model and OS version
- A short extract of the app's **own logcat** (the last ~200 lines)

It does **not** track usage or activity, collect a device identifier, or send system or other apps'
logs. Each report is **anonymized** and sent directly to my server, so only I receive the data.

&nbsp;

!!! example "Support"

    These docs are still a work in progress are may not be complete.

    If you need **help** getting started or run into any issues, [support](support.md) is available!
