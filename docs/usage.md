---
icon: lucide/tablet-smartphone
---

# :lucide-tablet-smartphone: Usage

[![Remote Wallpaper Android](assets/images/logo.png){ align=right width=96 }](https://github.com/cssnr/remote-wallpaper-android?tab=readme-ov-file#readme)

- [Remotes](#remotes)
- [Widget](#widget)
  - [Appearance](#appearance)
  - [Foreground Refresh](#foreground-refresh)
- [Settings](#settings)

## :lucide-router: Remotes

Remotes are the heart of the application. A remote is a link to an image.  
This can be a static image but you most likely have a link to a dynamic image or a redirect.

This application will refresh the image at the remote (image) url on a custom interval.

Example Remotes:

- <https://picsum.photos/4800/2400>
- <https://images.cssnr.com/aviation>

Remotes are managed from the **Remotes** page:

- **Add** with the `+` button, then paste a url
- **Activate** by tapping a remote — only one remote is active at a time
- **Delete** by long pressing a remote to select it, or by tapping the select all button, then
  deleting the selection

The first remote you add is activated automatically. Deleting the active remote activates the next
one in the list.

## :lucide-layout-panel-top: Widget

You can add a stats widget to the home screen to display info and functionality.

![Stats Widget](https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/remote-wallpaper/docs/widget.jpg){ style="border-radius: 16px;" }

The widget displays the following information:

- Current Remote
- Update Interval
- Last Updated Time
- Screen Icons (Home/Lock) — only when **Show Screen Icons** is enabled

The widget has the following functions:

- Refresh Wallpaper (refresh button)
- Launch Application (tap anywhere else on the widget)

### :lucide-palette: Appearance

The widget is styled from **Settings** -> **Widget Settings**:

| Setting            | Values              | Default | Description                      |
| ------------------ | ------------------- | ------- | -------------------------------- |
| Text Color         | White/Black/Liberty | White   | Color of the text and icons      |
| Background Color   | White/Black/Liberty | Black   | Color of the widget background   |
| Background Opacity | 0-100               | 35      | Opacity of the widget background |
| Show Screen Icons  | On/Off              | On      | Shows the Home/Lock Screen Icons |

Screen Icons follow the **Screens to Update** setting — the Home icon is shown when the home screen
is updated, the Lock icon when the lock screen is, and both when both are. With Show Screen Icons
off, both icons are hidden and only the text remains.

### :lucide-rotate-cw: Foreground Refresh

When disabled (default) the dynamic system theme (based on wallpaper) will not update until the
screen is turned on/off or another event that triggers a theme update happens.

When enabled, the widget refresh button runs the update through an invisible foreground activity
instead of a background broadcast. This causes the dynamic system theme to update immediately.

**The drawback of the foreground activity is the desktop becomes non-interactive while the update is
running. This time depends on device, network, and wallpaper size, but is usually only about 1-2
seconds.**

## :lucide-settings: Settings

App Settings:

- Update Interval
- Screens to Update (Home/Lock)
- Crop Wallpaper for Device
- Updates on Metered Connection
- Remove Background Restriction

Widget Settings:

- Text Color
- Background Color
- Background Opacity
- Show Screen Icons
- Foreground Widget Refresh

App Information:

- Application Information
- Open Android Settings
- Send Feedback

Debugging:

- Enable Crash Reporting
- Enable Application Logs
- View Logs

Application Logs:

- Wallpaper updates, scheduled work, and widget refreshes
- Entries older than 7 days are purged automatically
- Copy, Share, or Delete the logs from the logs page

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

    These docs are still a work in progress and may not be complete.

    If you need **help** getting started or run into any issues, [support](support.md) is available!
