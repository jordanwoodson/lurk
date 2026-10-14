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
> **[v1.6.0](https://github.com/jordanwoodson/lurk-morphe-patches/releases/tag/v1.6.0)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;39 patches total
<details open>
<summary><img src="patches/src/main/resources/rumble/icon/mipmap-xxxhdpi/ic_launcher_round.webp" width="28" height="28" align="center" alt="">&nbsp;&nbsp;<b>Rumble</b>&nbsp;&nbsp;•&nbsp;&nbsp;14 patches</summary>
<br>

**🎯 Supported versions:**

| 3.21.0 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Block ads](#block-ads) | Removes video ads (before and during videos and live streams) and the ads in feeds and under videos, signed in or not. The app never requests them, so it never sends the ad server your advertising ID, and no ad or Boosted video impressions or clicks are reported. |  |
| [Block cookie notice](#block-cookie-notice) | Stops Rumble's Ketch consent SDK from loading, so the cookie notice never appears and no consent is recorded. Rumble then keeps the analytics and ad tracking it gates on consent switched off. Push notifications stop too, since Rumble only starts its push service (Airship) with consent. Settings > Data Consent no longer opens. |  |
| [Change app name](#change-app-name) | Changes the name shown under Rumble's launcher icon and in Android's settings. | • App name |
| [Clean share links](#clean-share-links) | Removes everything after the ? from Rumble links before they are shared, so tracking tags such as e9s (where you found the video) can't go out with them. Rumble 3.21.0 shares clean links; this keeps them clean if the server starts adding tags. |  |
| [Clone app](#clone-app) | Installs Rumble under another package name, so it sits alongside the original app with its own sign-in and settings. Use "Change app name" to tell the two apart in the launcher. | • Package name |
| [Default video quality](#default-video-quality) | Starts videos at the chosen quality, or the closest one available, instead of 480p. Once you pick a quality in the player, Rumble keeps using that one. Live streams stay on Auto. | • Quality |
| [Disable analytics](#disable-analytics) | Stops Rumble's own analytics, which it sends whatever the consent setting: clicks, impressions, searches, watch progress and player state, with your user ID, username, advertising ID and AppsFlyer ID, to e.rumble.com and its debug log. Signed-in watch history may stop updating unless watch progress is kept. | • Keep watch progress |
| [Disable autoplay](#disable-autoplay) | Stops Rumble from playing the next video when one ends, and from playing muted previews in feeds, including the featured video on Home. Only the defaults change: Settings > Playback can turn either back on. | • Next video<br>• Feed previews |
| [Hide Premium upsells](#hide-premium-upsells) | Hides Rumble Premium promotions: the sheet that opens over videos, the Upgrade Now banner on Home and My Feed, the special offer on the account screen, the downloads pitch and Premium gifts in live chat. Premium-only videos still say so. |  |
| [Hide Shorts](#hide-shorts) | Removes Shorts from the bottom bar, Home, My Feed, channel pages, categories and search. Shorts links still open. |  |
| [Hide tipping and Rants](#hide-tipping-and-rants) | Hides the Tip buttons on channels, videos and Shorts, Rants (paid live chat messages) and paying with Rumble Wallet. Rants other viewers send still show. |  |
| [Lurk icon](#lurk-icon) | Replaces Rumble's launcher icon with the Lurk icon, including the themed icon on Android 13+. |  |
| [Remove tracking permissions](#remove-tracking-permissions) | Removes the advertising ID, ad measurement, install referrer and phone state permissions. Every SDK and Rumble's own requests then get an all-zero advertising ID. |  |
| [Unlock Premium](#unlock-premium) | Enables Rumble's local premium state after signing in and refreshing the profile, including ad-free playback and hiding premium upsells. Server-controlled content and subscription benefits still require server authorization. |  |

</details>

<details open>
<summary>📦&nbsp;&nbsp;<b>Twitch</b>&nbsp;&nbsp;•&nbsp;&nbsp;13 patches</summary>
<br>

**🎯 Supported versions:**

| 31.6.0 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Block client ads](#block-client-ads) | Declines ads handled by Twitch's native client ad eligibility check using its existing not eligible result. Does not remove ads stitched into streams by the server or guarantee ad-free playback. |  |
| [Block cookie notice](#block-cookie-notice) | Hides Twitch's automatic cookie consent notice without accepting it or changing saved vendor consent. Manual consent settings stay available. Use Disable analytics to block tracking. |  |
| [Change app name](#change-app-name) | Changes the name shown under Twitch's launcher icon and in Android's settings. | • App name |
| [Clean share links](#clean-share-links) | Removes known Twitch attribution and campaign parameters from shared links and copied share text. Keeps timestamps, clip IDs, unknown parameters and fragments. Covers native and React Native sharing. |  |
| [Clone app](#clone-app) | Installs Twitch under another package name alongside the original, with separate sign-in and settings. Amazon sign-in and verified app links may not work after re-signing. | • Package name |
| [Default video quality](#default-video-quality) | Starts playback at the selected rendition when available, with Source as the default. Saved player choices, including Auto, take priority. Unavailable renditions use Twitch's fallback. | • Default rendition |
| [Disable analytics](#disable-analytics) | Stops Twitch's native and React Native Spade event uploads, denies local analytics SDK consent, enables Branch's limited tracking mode, blocks Crashlytics uploads and prevents Sentry initialization. Watch progress, viewing statistics and rewards may stop updating. Restart Twitch after changing this setting. Playback requests still run. |  |
| [Disable autoplay](#disable-autoplay) | Stops the next recommended video and React Native feed clip from playing automatically, and defaults feed previews to Never. Manual Play and saved feed preview choices keep working. | • Feed previews<br>• Next video |
| [Hide Bits tipping](#hide-bits-tipping) | Hides chat Bits buttons, Bits purchase cards, the cheering tray and one-tap Bits controls. Keeps normal chat and Bits messages sent by other viewers. |  |
| [Hide Clips](#hide-clips) | Hides the Clips discovery tab on Home and the Clips tab on native channel profiles. Shared clip links still open. |  |
| [Hide subscription upsells](#hide-subscription-upsells) | Hides Subscribe and Gift a Sub prompts, Turbo promotions and the native Turbo signup/startup offer. Keeps the existing subscribed indicator and legitimate account entitlements. |  |
| [Lurk icon](#lurk-icon) | Replaces Twitch's launcher icon with Lurk artwork, including Android's themed icon. |  |
| [Remove tracking permissions](#remove-tracking-permissions) | Removes advertising ID and Google Play install-referrer permissions. Other identifiers and analytics require separate controls. |  |

</details>

<details open>
<summary>📦&nbsp;&nbsp;<b>Kick</b>&nbsp;&nbsp;•&nbsp;&nbsp;12 patches</summary>
<br>

**🎯 Supported versions:**

| 40.32.1 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Bypass VOD ad sessions](#bypass-vod-ad-sessions) | Experimental: uses the original VOD URL instead of requesting a separate ad-session manifest. Does not remove ads stitched into live streams. Enable in Lurk settings after patching. |  |
| [Change app name](#change-app-name) | Changes Kick's name in the launcher and Android settings. | • App name |
| [Clean share links](#clean-share-links) | Removes known campaign and advertising click parameters from kick.com links in share sheets. Keeps timestamps, clip IDs, unknown parameters and fragments. Kick's current share links are already clean. |  |
| [Clone app](#clone-app) | Installs Kick under another package name alongside the original, with separate sign-in and settings. Sharing sign-in with Kick's broadcaster app may not work after re-signing. | • Package name |
| [Default video quality](#default-video-quality) | Sets the default adaptive quality ceiling to the selected height when that rendition exists. Kick can still choose a lower quality. The player's Auto option uses this ceiling. | • Default quality ceiling |
| [Disable analytics](#disable-analytics) | Disables Firebase Analytics collection, Insider and Datadog initialization/events, Mux/Datazoom player analytics and PAL nonce requests. Playback API requests remain necessary to obtain streams. Restart Kick after changing this setting. This does not remove video ads. |  |
| [Disable autoplay](#disable-autoplay) | Starts Kick's feed previews switched off. Existing preferences and Kick's own autoplay switch keep working. Does not change intentionally opened streams. |  |
| [Hide Clips](#hide-clips) | Hides Clips tabs in Browse, channels and categories, and the popular-clips row on channels. Shared clip links still open. |  |
| [Hide KICKs tipping](#hide-kicks-tipping) | Hides the chat gift-shop button, KICKs purchase button and leaderboard tipping prompt. |  |
| [Hide subscription upsells](#hide-subscription-upsells) | Hides channel Subscribe and Gift a Sub buttons and the leaderboard gift-subscription prompt. Keeps the existing subscribed indicator. |  |
| [Lurk icon](#lurk-icon) | Replaces Kick's launcher icon with Lurk artwork, including Android's themed icon. |  |
| [Remove tracking permissions](#remove-tracking-permissions) | Removes advertising ID, ad attribution and Google Play install-referrer permissions. Other identifiers and analytics require separate controls. |  |

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
