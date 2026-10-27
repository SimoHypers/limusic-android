<div align="center">

# Limusic for Android

**A native Android YouTube Music client. Kotlin + Jetpack Compose, ad-free.**

<p align="center">
  <a href="https://github.com/SimoHypers/limusic-android/releases/latest"><img alt="GitHub Downloads" src="https://img.shields.io/github/downloads/SimoHypers/limusic-android/total?style=for-the-badge&label=DOWNLOADS&color=a4c400"></a>
  <a href="https://github.com/SimoHypers/limusic-android/releases/latest"><img alt="GitHub Release" src="https://img.shields.io/github/v/release/SimoHypers/limusic-android?display_name=release&style=for-the-badge&color=a10935"></a>
  <img alt="License" src="https://img.shields.io/github/license/SimoHypers/limusic-android?style=for-the-badge&color=1881cc">
  <a href="https://simohypers.github.io/limusic/"><img alt="Website" src="https://img.shields.io/badge/WEBSITE-limusic-e5486e?style=for-the-badge"></a>
  <a href="https://ko-fi.com/simohypers"><img alt="Support on Ko-fi" src="https://img.shields.io/badge/KO--FI-support-ff5e5b?style=for-the-badge&logo=kofi&logoColor=white"></a>
  <br>
  <img alt="Android 8.0+" src="https://img.shields.io/badge/Android_8.0+-3DDC84?style=for-the-badge&logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white">
</p>

**Limusic for Android** is the phone side of [Limusic](https://github.com/SimoHypers/limusic).
It talks directly to YouTube's internal API and plays through Media3: no backend server, no ads
in the audio. It started as a fork of [SimpMusic](https://github.com/maxrave-dev/SimpMusic) by
maxrave-dev and its contributors, cut down and rebuilt to work like the desktop app.

</div>

---

## Features

- **Ad-free playback**: streams come straight from YouTube's API, ads never do
- **Search & browse**: songs, albums, artists, playlists and the YTM home feed, with Quick picks up top
- **A Home you arrange**: pin library items as Shortcuts, then set the card size, mood chips and section order in Edit Home
- **Sign in** with your YouTube Music account, several accounts at once with switching between them
- **Your library, on YouTube Music**: likes, follows and saves go to your account; playlists, albums, artists and songs in the desktop's tabs; create, edit, sort and delete playlists. Signed out, everything stays on the phone and moves over later with one tap
- **Queue** with radio and autoplay that work like the desktop's: history above what's playing, no duplicates if you want, shuffle that sticks across queues
- **Synced lyrics**: word by word where the source has the timings, a source picker and timing nudge per song, providers in the order you choose
- **Downloads**: tap a playlist or album and it's on the phone; download the whole library at once, keep it downloaded as it grows, Wi-Fi only if you like
- **Music videos**: optional, the video plays instead of the cover
- **Three player styles**: Limusic's Classic, Material 3 Expressive and Apple Music, with Apple Music's animated artwork where a song has it
- **Colors from the cover**: the playing artwork tints the whole app
- **Sound**: loudness normalization, crossfade (beta), a ten-band equalizer, playback speed and pitch, and a sleep timer
- **Last.fm scrobbling**: approve once in the browser, then a live card shows what each play scrobbles as, with timing settings and rules that clean up titles
- **Discord Rich Presence**: sign in with Discord, then tap the live card preview to choose what it shows
- **Listen Together**: the same rooms as Limusic desktop, so phones and computers listen in sync
- **Android Auto**, lock screen and notification controls, and playback that keeps going in the background

---

<h2 align="center">Download & Install</h2>

<p align="center">
  <a href="https://github.com/SimoHypers/limusic-android/releases/latest">
    <img src="https://img.shields.io/badge/GitHub_Releases-100000?style=for-the-badge&logo=github&logoColor=white" height="40">
  </a>
</p>

| File | For | Size |
|---|---|---|
| `-arm64-v8a.apk` | Nearly every phone from the last ten years. Pick this one if unsure | ~23 MB |
| `-armeabi-v7a.apk` | Older 32-bit phones | ~23 MB |
| `-x86_64.apk` | Emulators and Chromebooks | ~25 MB |
| `-universal.apk` | Any of the above, at twice the size | ~49 MB |

Needs Android 8.0 or newer. Android asks once for permission to install apps from your browser or
file manager. There are no in-app updates yet: install the new release's APK over the old one,
and your library, downloads and sign-in stay.

---

## Scrobbling & Discord

Both live in Settings.

- **Last.fm**: tap Connect Last.fm, approve Limusic in the browser tab that opens,
  and come back connected. Tracks scrobble at the halfway point (or four minutes,
  whichever comes first) unless you move the timing sliders. The page shows your
  profile and a Now scrobbling card comparing YouTube's metadata with what Last.fm
  gets, and the rules (the desktop's presets, or your own) fix titles before they
  are sent. The scrobble switch pauses it without disconnecting.
- **Discord**: tap Sign in with Discord and approve in the browser. Limusic only
  gets to set what you are listening to. The card at the top of the page is a live
  preview: tap a part of it to change what it shows.

Building from source? Last.fm needs your own API credentials, and they are not in
the repo. Get a key at [last.fm/api/account/create](https://www.last.fm/api/account/create)
and add it to `local.properties`:

```
LASTFM_API_KEY=your_key
LASTFM_SECRET=your_secret
```

Without them everything else still builds and runs.

---

## Lyrics

Lyrics come from [Boidu](https://boidu.dev) and LyricsPlus first, then
[LRCLIB](https://lrclib.net), YouTube Music's own lyrics, Netease, QQ Music and
Kugou, and synced lyrics beat plain text. Matching is keyed on the track's
length, because popular songs exist as several cuts and the wrong one drifts out
of time. Results are cached on the phone, so replaying a track is instant.

Boidu and LyricsPlus are the sources with per-word timings, which is what lets a
line highlight word by word as it's sung. Change the order or switch any source
off in **Settings -> Lyrics -> Lyrics sources**.

In the Classic player's lyrics page, the source chip at the bottom shows where
the lyrics came from. Tap it to see every provider's answer for that song, pick
one, search by title, or nudge the timing. The pick sticks for that song.

---

## Listen Together

Synced listening with friends. Everyone streams their own audio from YouTube;
the room only relays play/pause, seeks, track changes and the queue. Open it from
the icon in Home's header, start a room and share the code, or join with
someone's code. The host approves every join.

Rooms run on the same relay as Limusic desktop, so a phone can join a room hosted
on a computer and the other way round. A room on a self-hosted relay shares an
invite of the form `CODE@host`, and pasting that joins it there.

---

## Building from Source

Needs JDK 21 and the Android SDK.

```bash
./gradlew :androidApp:assembleDebug
```

The APKs land in `androidApp/build/outputs/apk/debug/`. Release builds sign with
your own key: point `limusic.signing` in `local.properties` at a properties file
holding `storeFile`, `storePassword`, `keyAlias` and `keyPassword`.

---

## How It Works, Briefly

- SimpMusic's Kotlin client speaks YouTube's InnerTube API. Streams are resolved
  the way the desktop app does it: the web client with a PoToken first, then the
  VISIONOS and TVHTML5_SIMPLY identities when one fails.
- The PoToken comes from BotGuard, run in a hidden WebView. The signature cipher
  and the `n` parameter are solved on the phone in QuickJS, and never sent to a
  third-party server.
- Audio goes through Media3 ExoPlayer with an on-disk cache, inside a foreground
  service whose media session drives the notification, lock screen and Android Auto.
- Your library, downloads and lyrics cache live in a Room database on the phone.

---

## Star History

<a href="https://www.star-history.com/?repos=simohypers%2Flimusic-android&type=date&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/chart?repos=simohypers/limusic-android&type=date&theme=dark&legend=top-left" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/chart?repos=simohypers/limusic-android&type=date&legend=top-left" />
   <img alt="Star History Chart" src="https://api.star-history.com/chart?repos=simohypers/limusic-android&type=date&legend=top-left" />
 </picture>
</a>

---

## Support

Limusic is free and stays free. If it earned a coffee,
[ko-fi.com/simohypers](https://ko-fi.com/simohypers) is where to leave one.

---

## Credits

- [SimpMusic](https://github.com/maxrave-dev/SimpMusic) by maxrave-dev and its
  contributors: the app this one grew from, and most of its code
- [NewPipe](https://github.com/TeamNewPipe/NewPipe): the PoToken minter, ported to this app

---

## Disclaimer

This project is not affiliated with, funded, authorized, endorsed by, or in
any way associated with YouTube, Google LLC, or any of their affiliates and
subsidiaries.

All trademarks, service marks, and intellectual property rights referenced in
this project belong to their respective owners.

---

## License

[GPL-3.0](LICENSE), the license of SimpMusic, which this app is built on.
