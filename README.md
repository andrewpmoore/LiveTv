<p align="center">
  <img src="docs/branding/launcher-banner.png" alt="Live TV app banner" width="640" />
</p>

# Live TV for Android TV and Google TV

Live TV is a UK electronic programme guide (EPG) for Android TV and Google TV. Browse current and upcoming programmes with your remote, see programme artwork and details, and select a live programme in the guide to start watching in its broadcaster app or through an HDHomeRun tuner.

## Screenshots

### Live EPG guide

The guide below shows current programmes, sorted by channel number, with the selected programme's artwork and live progress.

![Live TV guide with live BBC, ITV and Channel 4 listings and programme artwork](docs/images/epg-guide.png)

### Settings

![Live TV settings for channel ordering and broadcaster filters](docs/images/settings.png)

## Features

- Remote and controller navigation between every broadcaster filter and the EPG grid, with a scrolling programme timeline and live **Now** marker.
- Programme titles, synopses, artwork, broadcast times, episode details and available accessibility metadata. The details panel updates as you move through the guide; select a live programme in the grid to watch it.
- Filters for **All Channels**, **BBC iPlayer**, **ITVX**, **Channel 4**, **Channel 5 / My5** and **Tuner**.
- HDHomeRun discovery through local UDP broadcasts, with hostname and cloud fallbacks. The app discovers the tuner's address automatically and checks again on startup.
- Built-in tuner playback using LibVLC, including MPEG-2 broadcast video, with the option to choose an installed external player in Settings.
- Broadcaster-app playback preferred for channels also available through the tuner. The Tuner filter shows the tuner's full visible lineup.
- Hide weak-signal or unplayable tuner channels using **Hide this channel from the guide** on the playback-error overlay. The action receives controller focus automatically and supports remote OK and gamepad A. Restore hidden channels in Settings.
- Channel ordering **by provider**, **by channel number** or **alphabetically**. New installs default to provider order.
- An optional toggle for adult tuner channels, hidden by default.
- Live listings from Sky's schedule service, with an EPGshare UK XMLTV fallback where the next 24 hours are incomplete. HDHomeRun's XMLTV guide also supplies tuner listings when available.
- Cached programme listings for use when the network is unavailable.
- Optional accessibility integration to navigate supported broadcaster apps automatically after opening a live programme.
- Jost and Montserrat typography, plus matching Live TV launcher icons and TV banner artwork.

The XMLTV backup feed is cached locally for six hours. ITVBe is not included in that backup feed. Font licenses are included in [`app/src/main/assets/fonts`](app/src/main/assets/fonts).

## Download and install

Download an APK from [GitHub Releases](https://github.com/andrewpmoore/LiveTv/releases/latest). Transfer it to the TV and open it with a file manager, or install it over ADB.

To download directly on the TV with Downloader by AFTVnews, enter code **7661331**.

Enable Developer Options and ADB debugging on the TV first. On Google TV Streamer, find these under **Settings → System → About** and **Settings → System → Developer options**. Then connect to the TV and install the APK, substituting its IP address and the downloaded filename:

```sh
adb connect <TV_IP_ADDRESS>:5555
adb install -r <downloaded-apk-filename>.apk
```

Broadcaster-app playback requires the corresponding app to be installed and signed in where required. Enable Live TV's accessibility service in Android settings if you want automatic navigation within supported broadcaster apps.

## Set up an HDHomeRun tuner

1. Connect the tuner and TV to the same local network, and scan your antenna channels using the HDHomeRun app or tuner's web interface.
2. Open Live TV. It discovers the tuner automatically; **Settings → HDHomeRun TV Tuner → Scan LAN** lets you search again.
3. Select **Tuner** in the filter bar to browse the lineup. Choose the built-in or an installed external player under **Settings → Tuner Playback**.

When a channel cannot play, the weak-signal or playback-error overlay offers **Hide this channel from the guide**. Press **OK** or **gamepad A** to hide it, or **Back** to return without hiding it. Hidden channels can be restored under **Settings → Hidden Tuner Channels**.

## Build from source

Requirements: **Java 17** and **Android SDK 35**.

Build a release APK and run its unit tests:

```sh
./gradlew assembleRelease
./gradlew testReleaseUnitTest
```

The release APK is written to `app/build/outputs/apk/release/app-release.apk`.

For a debug build, run `./gradlew assembleDebug`. Its APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Release signing

Use the same signing key for successive releases so Android accepts in-place updates. For local release signing, set these environment variables before building:

- `ANDROID_KEYSTORE_PATH`: path to the `.jks` keystore
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

If no release keystore path is configured, local release builds use the local debug signing key. They still use the release build configuration.

The [GitHub Actions release workflow](.github/workflows/release.yml) runs when a `v*` tag is pushed or when started manually. It uses these repository secrets for persistent signing:

- `ANDROID_KEYSTORE_BASE64`: base64-encoded `.jks` keystore
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Create the keystore once, keep a secure backup, and configure the secrets under **Settings → Secrets and variables → Actions** in GitHub. If these secrets are incomplete, the workflow uses a temporary CI debug key.

Moving between APKs signed with different keys can require uninstalling the old APK first. After reinstalling, enable Live TV's accessibility service again if you use it. Later updates signed with the same key retain the existing app settings.
