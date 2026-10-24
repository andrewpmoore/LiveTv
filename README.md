# FreeEPG TV for Android TV / Google TV

A premier 10-foot Electronic Programme Guide (EPG) built in **Kotlin** with **Jetpack Compose for TV**, covering the major UK free-to-air public service streaming platforms for the next 24 hours:

* **BBC iPlayer** (BBC One HD, BBC Two HD, BBC Three HD, BBC Four HD, BBC News HD, CBBC HD, CBeebies HD)
* **ITVX** (ITV1 HD, ITV2 HD, ITV3 HD, ITV4 HD, ITVBe)
* **Channel 4** (Channel 4 HD, E4 HD, More4 HD, Film4 HD, 4seven)
* **Channel 5 / My5** (5 HD, 5ACTION, 5STAR, 5 USA, 5DRAMA)
* **U / UKTV** (U&Dave HD, U&Drama, U&Yesterday, U&W, U&Alibi HD)

---

## Key Features

1. **Next 24 Hours Real-Time Schedules**:
   - Fetches live program listings and detailed metadata directly via unauthenticated real-time linear EPG services.
   - Automatically synchronizes across days (today + tomorrow) to ensure a complete continuous 24-hour window.
   - In-memory and persistent disk caching for instantaneous 0-second launch times and offline resilience.

2. **Rich Dynamic Hero Detail Pane**:
   - As you navigate the guide using the D-pad, the top hero pane instantly updates with:
     - Programme Title and Broadcaster Brand badge (BBC Red, ITV Gold, Channel 4 Green, Channel 5 Magenta, U Teal)
     - Broadcast time window and duration (e.g. `10:30 - 11:45 • 75m`)
     - **● LIVE NOW** indicator and real-time elapsed progress bar (`65% completed`)
     - Metadata pills: **HD**, **NEW**, **Series & Episode Number**, **Subtitles [S]**, **Audio Description [AD]**
     - Full synopsis and episode description
     - Direct prompt hint: `Press [OK] to Watch Live on {Broadcaster}`

3. **Direct Broadcaster Deep Linking**:
   - Selecting a currently-playing programme launches directly into that channel's live broadcast stream or broadcaster application.
   - Resilient intent hierarchy:
     1. Direct live stream custom URIs (`bbc://iplayer/playback/...`, `https://www.channel4.com/now/...`, `https://www.itv.com/watch/live/...`, etc.)
     2. Generic stream intents
     3. Broadcaster application launch intents
     4. Fallback dialog allowing 1-click installation from the **Google Play Store** or web stream playback.

4. **10-Foot Leanback Design**:
   - Full D-pad remote control support with glow focus borders and subtle scale animations.
   - Synchronized horizontal time headers (30-minute intervals: `10:30`, `11:00`, `11:30`...).
   - Fixed channel sidebar on the left.
   - Prominent red **NOW** needle across the entire guide grid.
   - Broadcaster filter tabs along the top to filter between "All Channels", "BBC iPlayer", "ITVX", "Channel 4", "Channel 5", and "U".

---

## Installing on Android TV / Google TV

The debug APK has been compiled and is ready at:
```bash
app/build/outputs/apk/debug/app-debug.apk
```

### Install via ADB (Network or USB):
1. Enable **Developer Options** and **ADB Debugging** on your Android TV device:
   - Go to `Settings > Device Preferences > About > Build` and click **Build** 7 times.
   - Go to `Settings > Device Preferences > Developer Options` and enable `Network Debugging` (or `USB Debugging`).
2. Connect to your Android TV IP address from your terminal:
   ```bash
   adb connect <TV_IP_ADDRESS>:5555
   ```
3. Install the APK:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## Building from Source

Requirements:
- Java 17 (`openjdk@17`)
- Android SDK (API 34/35)

```bash
# Build Debug APK
./gradlew assembleDebug

# Run Unit Tests
./gradlew test
```
# LiveTv
