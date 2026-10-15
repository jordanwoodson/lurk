<p align="center">
  <img src="patches-bundle.png" width="128" alt="">
</p>

<h1 align="center">Lurk</h1>

<p align="center">
  <b>Morphe privacy patches for Twitch, Kick and Rumble.</b><br>
  Watch the stream. Skip the tracking.
</p>

<p align="center">
  <a href="https://morphe.software/add-source?github=jordanwoodson/lurk-morphe-patches&name=Lurk"><img src="https://img.shields.io/badge/Add_to_Morphe-39F8AE?style=for-the-badge&logo=android&logoColor=080D1B" alt="Add to Morphe"></a>
</p>

<p align="center">
  <a href="https://github.com/jordanwoodson/lurk-morphe-patches/releases/latest"><img src="https://img.shields.io/github/v/release/jordanwoodson/lurk-morphe-patches?label=release&color=39F8AE" alt="Latest release"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-GPL--3.0-blue" alt="License GPL-3.0"></a>
</p>

Lurk is a set of privacy-focused patches for streaming apps, applied with [Morphe](https://morphe.software):
Twitch, Kick and Rumble to start, more apps later. This repo is both the patch bundle and the workspace
for building it. Lurk has no connection to Twitch, Kick, Rumble or the Morphe project.

### How to use these patches

Tap **Add to Morphe** on an Android phone with [Morphe Manager](https://morphe.software) installed; on
a computer it shows a QR code to scan with the phone. Releases from the `dev` branch are pre-releases:
turn on pre-releases for this source in Manager to get them.

Patched apps get a **Lurk** settings screen that turns included patches off and on without patching
again. Long-press the app icon > Lurk settings; Rumble also has Settings > App info > Lurk.
Kick and Twitch's JavaScript settings require a restart. See the [Kick notes](docs/kick.md) for
the experimental VOD option and the [Twitch notes](docs/twitch.md) for coverage and testing limits.

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.9.0](https://github.com/jordanwoodson/lurk-morphe-patches/releases/tag/v1.9.0)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;71 patches total
<details open>
<summary><img src="assets/twitch-icon-round.webp" width="28" height="28" align="center" alt="">&nbsp;&nbsp;<b>Twitch</b>&nbsp;&nbsp;•&nbsp;&nbsp;45 patches</summary>
<br>

**🎯 Supported versions:**

| 31.6.0 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| Anonymous chat | Adds a switch that reads chat as an anonymous guest, the way Twitch does when signed out, while the rest of the app stays signed in, so you aren't listed among a channel's chatters. Sending chat messages is disabled while it's on. Off by default; applies after a restart. |  |
| Auto-claim channel points | Adds a switch that claims the channel points bonus for your account as soon as its chest appears in a channel's chat, with the same request as tapping the chest. Each bonus is claimed once. Off by default; needs a signed-in account. |  |
| Avoid stitched ads | While Twitch's server stitches an ad break into a live stream, plays the stream instead from a picture-by-picture session, the kind Twitch itself shows during breaks, which gets no ads: at 360p at most, with sound, then back to the stream's own session and quality when the break is over. The player doesn't reload. The playback token for that session is requested without your account. When none can be had, the break plays, and Mute stitched ads still mutes and covers it. Native and React Native players; streams started while it's off are left alone. |  |
| Background audio | Keeps streams and videos playing as audio when you leave the app, using Twitch's own "Continue Playing Audio in the Background" with its audio-only stream and media notification. Changes only the default, Never in Twitch: a choice made in Twitch's settings still wins. |  |
| Block client ads | Declines ads handled by Twitch's native client ad eligibility check using its existing not eligible result. Does not remove ads stitched into streams by the server or guarantee ad-free playback. |  |
| Block cookie notice | Hides Twitch's automatic cookie consent notice without accepting it or changing saved vendor consent. Manual consent settings stay available. Use Disable analytics to block tracking. |  |
| Block display ads | Blocks the ad requests that Block client ads doesn't cover: stream display ads (squeezeback and lower third), audio ads, multi-format ad pods, the Following headliner, feed display and video ads and brand surveys, in both the native and React Native parts of the app. Each request gets the failure or no-ad answer Twitch already handles. Sponsorship and paid-promotion labels are kept. Ads stitched into the stream by the server still play. |  |
| Change app name | Changes the name shown under Twitch's launcher icon and in Android's settings. | • App name |
| Chat appearance | Adds chat look settings: text size, alternating row backgrounds, plain /me messages, and the width and background opacity of the landscape chat panel. Each keeps Twitch's look until changed in Lurk settings. Native live chat, chat history and VOD chat replay; not React Native chat. |  |
| Chat keywords | Highlights chat messages that mention you or contain your highlight words with a tinted background, optionally with a vibration, and hides messages with blocked words or from blocked users. Mentions are on by default; the word and user lists start empty and are set in Lurk settings. Native live chat, chat history and VOD chat replay; not React Native chat. |  |
| Chat overlay | Adds a switch that scrolls live chat messages right to left across the top of the video in landscape (danmaku), as plain white text with the sender's name. Off until turned on in Lurk settings, with choices for speed and number of lines. Native live channels only: not React Native chat, VODs or chat replay. The overlay never takes touches. |  |
| Chat timestamps | Shows the time each live chat message was sent in front of it, the way Twitch already shows it on chat history, in 24-hour or 12-hour format. Whispers are unchanged. |  |
| Clean share links | Removes known Twitch attribution and campaign parameters from shared links and copied share text. Keeps timestamps, clip IDs, unknown parameters and fragments. Covers native and React Native sharing. |  |
| Clone app | Installs Twitch under another package name alongside the original, with separate sign-in and settings. Amazon sign-in and verified app links may not work after re-signing. | • Package name |
| Default startup tab | Adds a choice of the screen Twitch opens on instead of Home: Browse, Following or Activity. Links and notifications still open what they point to. |  |
| Default video quality | Starts playback at the selected rendition when available, with Source as the default. Saved player choices, including Auto, take priority. Unavailable renditions use Twitch's fallback. | • Default rendition |
| Disable analytics | Stops Twitch's native and React Native Spade event uploads, denies local analytics SDK consent, enables Branch's limited tracking mode, blocks Crashlytics uploads, Nielsen audience pings and Sprig surveys, and prevents Sentry initialization. Watch progress, viewing statistics and rewards may stop updating. Restart Twitch after changing this setting. Playback requests still run. |  |
| Disable autoplay | Stops the next recommended video and React Native feed clip from playing automatically, and defaults feed previews to Never. Manual Play and saved feed preview choices keep working. | • Feed previews<br>• Next video |
| Fix notifications | Lets the patched app register for push notifications. Firebase only accepts Twitch's original package name and signing certificate, and refuses the re-signed app, so its registration requests send the original ones. |  |
| Fix stuck start screen | Opens Twitch when its start screen is still waiting after 5 seconds. Twitch waits for Branch's link lookup before it routes, and when that never answers, it stays on a blank start screen until it's closed, unpatched Twitch included. |  |
| Force ExoPlayer | Adds a switch that plays live channels and videos with the ExoPlayer backend Twitch still ships instead of Amazon IVS. Off until turned on in Lurk settings. Clips, encrypted streams and React Native players stay on IVS. Lurk patches that hook IVS, such as Normal latency, Mute stitched ads and Show video stats, don't act on ExoPlayer. |  |
| Hide Bits tipping | Hides chat Bits buttons, Bits purchase cards, the cheering tray and one-tap Bits controls. Keeps normal chat and Bits messages sent by other viewers. |  |
| Hide Clips | Hides the Clips discovery tab on Home and the Clips tab on native channel profiles. Shared clip links still open. |  |
| Hide Home sections | Hides Stories on Home and, each with its own switch, the Continue watching, offline channels and recommended channels sections of Following. |  |
| Hide chat highlights | Hides the Hype Train, Bits and gift leaderboards, other viewers' sub and gift messages and gift banners, pinned cheers and watch-streak pop-ups, each with its own switch. Celebrations can be hidden too. Other chat messages are unchanged. |  |
| Hide mature content warning | Skips the content warning on streams, videos and clips with mature content labels, as if you had chosen Start Watching. Age, region, guardian and account restrictions still apply. Off by default. |  |
| Hide player buttons | Hides the player's Clip, Cast and Share buttons, each with its own switch. Cast stays visible by default. Other ways to clip and share, such as the channel page, are unchanged. |  |
| Hide subscription upsells | Hides Subscribe and Gift a Sub prompts, Turbo promotions and the native Turbo signup/startup offer. Keeps the existing subscribed indicator and legitimate account entitlements. |  |
| Lurk icon | Replaces Twitch's launcher icon with Lurk artwork, including Android's themed icon. |  |
| Mute stitched ads | While an ad break that Twitch's server stitched into a stream plays, mutes the player and covers the video with black, then restores both when the stream returns. Works in the native and React Native players. The ad still plays and the break isn't shorter. |  |
| Navigation bar position | Adds a choice that moves Twitch's bottom navigation bar to the top of the screen and/or hides its labels, leaving only the icons. Twitch's own layout stays until another choice is made in Lurk settings. |  |
| Normal latency | Adds a setting that turns off the player's low-latency mode for live streams: a few seconds more delay to the broadcaster, for fewer rebuffers on a weak connection. Off until turned on in Lurk settings. |  |
| Player gestures | Sets the skip interval of the video (VOD) rewind and forward buttons and the media notification, and adds brightness (left) and volume (right) swipes on the video in landscape. Twitch's -10/+30 seconds stay until you choose another interval in Lurk settings; swipes start off. |  |
| Playlist proxy | Sends live stream playlist requests to a proxy server you set in this patch's options, for example one that returns playlists without stitched ads. The proxy receives each request in full, including your signed playback token, which identifies your account: only use a proxy you trust. An http:// proxy gets an exception from Twitch's HTTPS-only rule for its host alone, and the token then travels unencrypted. Does nothing while the proxy URL is empty. Videos and clips are not proxied. | • Proxy URL |
| Refresh stream | Long-press the player's settings gear, or the middle of the video, to reload a stuck stream through Twitch's own Retry: a new access token and playlist for live streams, the same position for videos. Covers Twitch's native players. |  |
| Remove tracking permissions | Removes advertising ID and Google Play install-referrer permissions. Other identifiers and analytics require separate controls. |  |
| Show deleted messages | Keeps the text of chat messages that moderators delete, or remove with a timeout or ban, struck through and dimmed instead of Twitch's "message deleted" placeholder. A second switch keeps the messages when a moderator clears the whole chat. |  |
| Show video stats | Adds the video stats button Twitch shows only to its staff to the native player: latency to the broadcaster, bitrate, buffer, dropped frames, codecs and server. Everything shown is local to the player. |  |
| Sleep timer | Pauses playback a chosen time after it last started or resumed, through Twitch's own pause for native, portal and React Native players, and says so in a short notice. Choose the time in Lurk settings; it is off until then. |  |
| Tablet layout | Adds a switch that makes Twitch treat the phone as a tablet: Home and the player rotate, the player shows chat beside the video in landscape, and a navigation rail on the left replaces the bottom bar in landscape (and in portrait too with a second switch). Off until turned on in Lurk settings; applies after a restart. |  |
| Third-party badges | Adds switches that show FrankerFaceZ and Chatterino badges and pronouns.alejo.io pronouns next to chatters' names in native chat. Each switch is off until turned on in Lurk settings. The badge switches download that service's public list of all badge holders (api.frankerfacez.com, api.chatterino.com) about once an hour and check it on the phone. The pronouns switch sends the username of each chatter whose message you see to pronouns.alejo.io. 7TV badges are not included. |  |
| Third-party emotes | Adds switches that show 7TV, BetterTTV and FrankerFaceZ emotes in chat, Twitch's chat history and VOD chat replay. Each switch is off until turned on in Lurk settings; when on, it sends the ID of the channel you watch to that service (7tv.io, api.betterttv.net, api.frankerfacez.com) for its emote lists and loads the emote images from its CDN. Not in the emote picker or autocomplete. |  |
| Use native player | Always opens live channels in Twitch's native player, never the React Native "Ultralight" player a server experiment can switch some users to. Most Lurk playback, chat and ad patches hook the native player. |  |
| VOD chapters | Adds a Chapters button to the native video (VOD) player that lists the stream's game and category changes and jumps to one through Twitch's own seek. The list comes from Twitch's public data, fetched with the app's client ID. On by default. |  |
| Watch without signing in | Opens Twitch without an account instead of stopping at the Log In / Sign Up screen, using Twitch's own signed-out mode. Streams, videos and chat can be watched; following, chatting and other account actions still ask you to sign in. Signed-in use is unchanged. |  |

</details>

<details open>
<summary><img src="patches/src/main/resources/rumble/icon/mipmap-xxxhdpi/ic_launcher_round.webp" width="28" height="28" align="center" alt="">&nbsp;&nbsp;<b>Rumble</b>&nbsp;&nbsp;•&nbsp;&nbsp;14 patches</summary>
<br>

**🎯 Supported versions:**

| 3.21.0 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| Block ads | Removes video ads (before and during videos and live streams) and the ads in feeds and under videos, signed in or not. The app never requests them, so it never sends the ad server your advertising ID, and no ad or Boosted video impressions or clicks are reported. |  |
| Block cookie notice | Stops Rumble's Ketch consent SDK from loading, so the cookie notice never appears and no consent is recorded, and ignores consent recorded earlier. Rumble then keeps the analytics and ad tracking it gates on consent switched off. Push notifications stop too, since Rumble only starts its push service (Airship) with consent. Settings > Data Consent no longer opens. |  |
| Change app name | Changes the name shown under Rumble's launcher icon and in Android's settings. | • App name |
| Clean share links | Removes everything after the ? from Rumble links before they are shared, so tracking tags such as e9s (where you found the video) can't go out with them. Rumble 3.21.0 shares clean links; this keeps them clean if the server starts adding tags. |  |
| Clone app | Installs Rumble under another package name, so it sits alongside the original app with its own sign-in and settings. Use "Change app name" to tell the two apart in the launcher. | • Package name |
| Default video quality | Starts videos at the chosen quality, or the closest one available, instead of 480p. Once you pick a quality in the player, Rumble keeps using that one. Live streams stay on Auto. | • Quality |
| Disable analytics | Stops Rumble's own analytics, which it sends whatever the consent setting: clicks, impressions, searches, watch progress and player state, with your user ID, username, advertising ID and AppsFlyer ID, to e.rumble.com and its debug log. Signed-in watch history may stop updating unless watch progress is kept. | • Keep watch progress |
| Disable autoplay | Stops Rumble from playing the next video when one ends, and from playing muted previews in feeds, including the featured video on Home. Only the defaults change: Settings > Playback can turn either back on. | • Next video<br>• Feed previews |
| Hide Premium upsells | Hides Rumble Premium promotions: the sheet that opens over videos, the Upgrade Now banner on Home and My Feed, the special offer on the account screen, the downloads pitch and Premium gifts in live chat. Premium-only videos still say so. |  |
| Hide Shorts | Removes Shorts from the bottom bar, Home, My Feed, channel pages, categories and search. Shorts links still open. |  |
| Hide tipping and Rants | Hides the Tip buttons on channels, videos and Shorts, Rants (paid live chat messages) and paying with Rumble Wallet. Rants other viewers send still show. |  |
| Lurk icon | Replaces Rumble's launcher icon with the Lurk icon, including the themed icon on Android 13+. |  |
| Remove tracking permissions | Removes the advertising ID, ad measurement, install referrer and phone state permissions. Every SDK and Rumble's own requests then get an all-zero advertising ID. |  |
| Unlock Premium | Enables Rumble's local premium state after signing in and refreshing the profile, including ad-free playback and hiding premium upsells. Server-controlled content and subscription benefits still require server authorization. |  |

</details>

<details open>
<summary><img src="assets/kick-icon-round.webp" width="28" height="28" align="center" alt="">&nbsp;&nbsp;<b>Kick</b>&nbsp;&nbsp;•&nbsp;&nbsp;12 patches</summary>
<br>

**🎯 Supported versions:**

| 40.32.1 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| Bypass VOD ad sessions | Experimental: uses the original VOD URL instead of requesting a separate ad-session manifest. Does not remove ads stitched into live streams. Enable in Lurk settings after patching. |  |
| Change app name | Changes Kick's name in the launcher and Android settings. | • App name |
| Clean share links | Removes known campaign and advertising click parameters from kick.com links in share sheets. Keeps timestamps, clip IDs, unknown parameters and fragments. Kick's current share links are already clean. |  |
| Clone app | Installs Kick under another package name alongside the original, with separate sign-in and settings. Sharing sign-in with Kick's broadcaster app may not work after re-signing. | • Package name |
| Default video quality | Sets the default adaptive quality ceiling to the selected height when that rendition exists. Kick can still choose a lower quality. The player's Auto option uses this ceiling. | • Default quality ceiling |
| Disable analytics | Disables Firebase Analytics collection, Insider and Datadog initialization/events, Mux/Datazoom player analytics and PAL nonce requests. Playback API requests remain necessary to obtain streams. Restart Kick after changing this setting. This does not remove video ads. |  |
| Disable autoplay | Starts Kick's feed previews switched off. Existing preferences and Kick's own autoplay switch keep working. Does not change intentionally opened streams. |  |
| Hide Clips | Hides Clips tabs in Browse, channels and categories, and the popular-clips row on channels. Shared clip links still open. |  |
| Hide KICKs tipping | Hides the chat gift-shop button, KICKs purchase button and leaderboard tipping prompt. |  |
| Hide subscription upsells | Hides channel Subscribe and Gift a Sub buttons and the leaderboard gift-subscription prompt. Keeps the existing subscribed indicator. |  |
| Lurk icon | Replaces Kick's launcher icon with Lurk artwork, including Android's themed icon. |  |
| Remove tracking permissions | Removes advertising ID, ad attribution and Google Play install-referrer permissions. Other identifiers and analytics require separate controls. |  |

</details>

<!-- PATCHES_END -->

## 🛠️ Workspace

### Setup

```sh
bin/bootstrap
```

It checks the JDK (21+) and Android SDK, installs jadx, apkeep and hermes-dec through mise
(`mise.toml`), downloads Morphe Desktop and apktool into `tools/` with checksum checks, and checks
GitHub Packages access. Morphe's Gradle plugin is only published on GitHub Packages, so the build needs
a token with `read:packages`. `bin/build` borrows the `gh` CLI token; give it the scope once:

```sh
gh auth refresh -h github.com -s read:packages
```

(Or put `gpr.user` / `gpr.key` in `~/.gradle/gradle.properties`.)

### Layout

| Path | What |
|---|---|
| `patches/src/main/kotlin/io/github/jordanwoodson/patches/<app>/` | Patches for one app. `shared/Constants.kt` holds its package, target versions and pinned signing cert. |
| `patches/.../patches/shared/` | Code shared across apps. |
| `extensions/shared/` | Java code injected into patched apps (`extensions/shared.mpe`). Add `extensions/<app>/` when an app needs its own. |
| `assets/icon.png` | The Lurk icon at full size (1254 px). `patches-bundle.png` is a 512 px copy: Manager shows it for this source, and the README header uses it. |
| `assets/<app>-icon.png` | An app's Lurk launcher artwork, flattened to its two colors. `bin/app-icon` turns it into `patches/src/main/resources/<app>/icon/` (adaptive icon, vector logo, legacy `.webp`), which the app's icon patch copies in. The patches list shows the round legacy icon next to the app. |
| `assets/<app>-icon-round.webp` | The patches list icon for apps whose icon patch recolors Rumble's Lurk logo while patching instead of shipping its own (`twitch`, `kick`). |
| `apks/<app>/<version>/` | Original APKs, added with `bin/apk-add`. Not committed. |
| `tools/` | Downloaded jars and the Exodus tracker list. Not committed. |
| `~/scratch/morphe/` | Decompiled code, patched APKs, the test emulator, reference repos. Regenerable. |
| `~/.local/share/morphe/` | Morphe Desktop data, including the signing keystore. Keep it: updates only install over a patched app signed with the same key. |

Apps known to the workspace are the folders with a `shared/Constants.kt`: `twitch`, `kick`, `rumble`.
Add another with `bin/new-app <slug> "<Launcher name>" <package> [0xRRGGBB]`.

### Getting APKs

Only from **APKMirror**, **Google Play** or **Uptodown**. No other mirrors.

- APKMirror or Uptodown: download in a browser, then `bin/apk-add ~/Downloads/<file>`.
- Google Play: `bin/apk-fetch <app>` (latest version only). It needs a Google account token in
  `~/.config/apkeep/apkeep.ini`:

  ```ini
  [google]
  email = someone@gmail.com
  aas_token = aas_et/...
  ```

  Get the AAS token once with `apkeep -e <email> --oauth-token <oauth2_4/...>`, following
  [apkeep's Google Play guide](https://github.com/EFForg/apkeep/blob/master/USAGE-google-play.md).
  Use a spare account.

`bin/apk-add` reads the package and version, checks the base APK's signature, and compares the signing
cert with the one pinned in the app's `Constants.kt`. It pins the cert on an app's first APK and refuses
a mismatch after that. Files land in `apks/<app>/<version>/`.

All three apps ship on APKMirror as `.apkm` bundles. Kick is React Native: most of its logic is Hermes
bytecode in `assets/index.android.bundle`. Patch it with `hermesPatch` from morphe-patches-library
(already a dependency); its native side (dex) patches like any other app.

### Workflow

```sh
bin/scan rumble          # tracker SDKs (Exodus Privacy signatures) and URL hosts in the app
bin/decompile rumble     # jadx Java, apktool smali, Hermes disassembly → ~/scratch/morphe/rumble/<version>/
# write patches under patches/.../rumble/
bin/patch rumble         # build the bundle, patch the newest stored version → ~/scratch/morphe/out/
bin/emu start            # headless Android 16 emulator (emulator-5580)
bin/patch rumble --no-build --install=emulator-5580
adb -s emulator-5580 logcat -s lurk    # Logger output from patched code
bin/emu shot             # screenshot → ~/scratch/morphe/shots/
```

`bin/app-icon <app> <artwork.png>` regenerates an app's launcher icon from square two-color artwork (a flat logo on a flat background).
`bin/patch` passes anything after `--` to Morphe Desktop, for example
`bin/patch twitch -- --exclusive -e "Disable analytics"`.
`bin/refs` clones Morphe's own repos (patcher docs, patches library, official patches) for API reference.

### Writing a patch

The [Rumble 3.21.0 notes](docs/rumble.md) cover each Rumble patch: what it changes, how it finds
its code without obfuscated names, and how it was tested.

One folder per feature under the app, with the patch and its fingerprints:

```
patches/.../patches/rumble/
  shared/Constants.kt
  analytics/DisableAnalyticsPatch.kt
  analytics/Fingerprints.kt
```

```kotlin
@Suppress("unused")
val disableAnalyticsPatch = bytecodePatch(
    name = "Disable analytics",
    description = "Stops the app from sending usage analytics.",
) {
    compatibleWith(COMPATIBILITY_RUMBLE)

    execute {
        SomeAnalyticsFingerprint.method.returnEarly()
    }
}
```

Fingerprints match on strings, signatures and instruction patterns rather than class names, since R8
renames classes between versions. See the patcher docs in `~/scratch/morphe/refs/morphe-patcher/docs/`
(after `bin/refs`) and the [fingerprinting guide](https://github.com/MorpheApp/morphe-patcher/blob/main/docs/2_2_1_fingerprinting.md).

### Releasing

- Work on the `dev` branch. Pushing `feat:` or `fix:` commits there makes `release.yml` publish a
  pre-release; `chore:` commits don't release. Merge `dev` into `main` (no squash) for a stable release.
- Use semantic commit messages: `feat:`, `fix:`, `chore:`.
- Don't hand-edit generated files: `patches-list.json`, `patches-bundle.json`, `CHANGELOG.md`, or the patches
  list above. Don't create releases by hand.

## 📜 License

GPLv3. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
