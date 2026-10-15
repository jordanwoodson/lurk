# Rumble 3.21.0

`Unlock Premium` targets `com.rumble.battles` **3.21.0 / versionCode 887**, using the
APKMirror APKM and signing certificate pinned in `rumble/shared/Constants.kt`.

Sign in, or sign out and back in after installing, to fetch a fresh profile. The patch
overrides the profile's local premium flag; a signed-out session never fetches that
profile. Server-authorized streams, purchases and subscription metadata are unaffected.
Live signed-in playback has not been verified with an account.

## Implementation

Inspired by the profile-entry approach in
[hoomans-morphe-patches](https://github.com/arandomhooman/hoomans-morphe-patches/blob/018277825a611ad61f3a16a70e562291c06f4081/patches/src/main/kotlin/hooman/morphe/patches/rumble/premium/UnlockPremiumPatch.kt),
which targets 3.14.5. This implementation fingerprints the specific `isPremium:Z`
field read, method signature and two-instruction getter, requires a unique match,
and returns `true` after that read while the `unlock_premium` switch is on (see
[Lurk settings](#lurk-settings)). It does not depend on the obfuscated getter name
or the assumption that the DTO has only one boolean getter.

The independently traced 3.21.0 path is:

```text
UserProfile.l() reads isPremium (JSON is_premium)
  -> we.a.t(...) maps it into lf.h
  -> lf.h.x() returns the entity's premium flag
  -> mf.f passes it to yg.d.d1(...)
  -> session DataStore key isUserPremiumKey
```

These obfuscated names describe this APK only; they are not patch fingerprints.
Only the getter's result changes. Nullable `premiumData`, account
validation, age verification and onboarding retain their original behavior.

## Build and test

With GitHub Packages access configured as described in the workspace README:

```sh
bin/patch rumble
bin/emu start
bash bin/test-rumble ~/scratch/morphe/out/rumble-3.21.0-patched.apk
```

The probe executes the actual APK's profile getter and mapper on `emulator-5580`
using free and paid fixtures, including absent subscription metadata. It also checks
unrelated account state. It does not use a Rumble account or send API requests.
The test is deliberately pinned to 3.21.0 and must be re-derived for other versions.

To confirm the negative control, the same probe against the original base APK must
fail with `Profile must report premium when server is_premium=false`:

```sh
bash bin/test-rumble ~/scratch/morphe/rumble/3.21.0/base.apk
```

## Validation on 2026-10-14

- Stock APK: the expected assertion fails. Patched APK: free and paid fixtures pass.
- Morphe Desktop 1.18.1 applies the patch, rebuilds and signs successfully.
- The signed APK installs and cold-launches to Rumble's sign-in screen on Android 16.
- All six output DEX files pass `dexdump` and D8; APK manifest, signature and alignment
  verification pass. The optional cross-DEX verifier reports the same 42 missing
  optional SDK/reference issues on both stock and patched APKs, with no added issues.
- The workspace Gradle build was blocked by HTTP 401 from GitHub Packages with both
  configured credentials. The tested `.mpp` was compiled against the patcher API in
  the existing Morphe Desktop JAR, using the installed Kotlin compiler and Android D8.
  It contains both JVM classes and Android DEX. The local reproduction script is
  `~/scratch/morphe/build-rumble/build.py`; normal Gradle build validation remains pending
  working `read:packages` credentials.

Artifacts are in `~/scratch/morphe/out/`: `rumble-3.21.0.mpp`,
`rumble-3.21.0-patched.apk` and the Morphe result JSON. Build and verification logs
are in `~/scratch/morphe/build-rumble/`; the launch screenshot is in
`~/scratch/morphe/shots/rumble-premium-launch.png`.

## Lurk icon

`Lurk icon` replaces the launcher icon with `assets/rumble-icon.png`. Rumble 3.21.0 builds its
adaptive icon from `drawable/ic_launcher_background.xml` and `drawable/ic_launcher_foreground.xml`
(`mipmap-anydpi/ic_launcher[_round].xml`) and ships `.webp` fallbacks per density. The patch
overwrites those files under the same names, so resource IDs and the manifest stay untouched, and
adds a themed (monochrome) layer. It fails if any of the files is missing in a new version.

Regenerate the resources after changing the artwork with
`bin/app-icon rumble <artwork.png>`.

Validated on 2026-10-14 with a local build: Morphe Desktop 1.18.1 applies `Lurk icon` and
`Unlock Premium` to 3.21.0; the launcher and the Android 16 splash screen show the new icon and the
app cold-launches to its home feed.

## Block cookie notice

Rumble 3.21.0 shows its cookie and privacy notice through the Ketch consent SDK (organization `rumble`,
property `rumble_android_app`). Rumble's consent service calls `Ketch.load()` at startup and on every
resume. Settings > Data Consent calls `Ketch.showPreferences()`. Each loads Ketch's web experience in a
WebView, which fetches the config for the user's jurisdiction and sends the identities Rumble supplied
(Android advertising ID and AppsFlyer installation ID) to `global.ketchcdn.com`. What it shows depends on
the jurisdiction: a banner or modal where privacy laws apply. In `us___no_privacy_laws` it shows nothing
and records consent to every purpose (`targeted_advertising`, `analytics`, `essential_services`).
Recorded consent starts AppsFlyer and enables Firebase Analytics. Opening Data Consent records the same
defaults as soon as the dialog appears, before anything is tapped.

The patch makes both SDK methods return `false` immediately, which is what they return when they cannot
create the WebView. They are found by the SDK's log lines ("Not loading…" / "Not showing preferences…
as an experience is already being shown") and the `(ZZII)Z` signature, so the R8-renamed class name
doesn't matter. Without stored consent, Rumble's tracker hooks only ever receive an empty purpose set:
AppsFlyer is never initialized, and Firebase Analytics, Firebase Performance and the Facebook SDK's
automatic event logging stay off.
Settings > Data Consent no longer opens anything.

Rumble also keeps the last decision in `consent_cache.json` and applies it at startup without Ketch, so
consent recorded while the switch was off, or before the patch was included, would still start the
trackers on every launch. While the switch is on, the cached model's `hasDecision` getter returns `false`
(found as the class whose `toString` starts `CachedConsent(hasDecision=`; it is the only `()Z` there).
Startup then hands every tracker an empty purpose set, as on a fresh install: Firebase Analytics,
Crashlytics, Performance and Facebook switch their persisted collection off again, and AppsFlyer and
Airship are never started. The cached consent strings still go into ad requests, which Block ads stops.
Turning the switch off uses the cached consent again.

Validated on 2026-10-14 on the Android 16 emulator, fresh data each time, location resolved by Ketch as
US-MO:

- Stock: Ketch loads on launch, sends the advertising ID in its `log` request and writes
  `consent_cache.json` with every purpose granted. AppsFlyer records `first_launch`, Firebase consent is
  `G111` and measurement is enabled.
- Patched (`Block cookie notice` only): launch, signed-out home feed, background/resume and Settings >
  Data Consent make no Ketch requests and no crashes. No `consent_cache.json`, no AppsFlyer
  `first_launch`, Firebase consent `G100` with measurement disabled.
- Morphe Desktop 1.18.1 applies it together with `Unlock Premium`. The bundle was built with the local
  fallback described above, because Gradle still can't reach GitHub Packages.

The visible banner itself wasn't reproduced, since no notice is shown at that location; the patch doesn't
depend on jurisdiction, because both entry points that can display one are disabled.

Block cookie notice also stops push notifications. Rumble only starts Airship, its push service, from
the consent callback, and enables Airship's push feature only with the `essential_services` purpose. With
no consent recorded, Airship never takes off and incoming pushes are dropped.

## Lurk settings

Every Rumble patch that changes code can be switched off and on in the app, without patching again:
Settings > App info > **Lurk**, or long-press the launcher icon > **Lurk settings**. The screen lists
only the patches that were included. Each switch starts at the patch's default (or its patch option)
and keeps the user's choice from then on, including across updates. Reset to defaults forgets those
choices.

| Switch | Key | Takes effect |
|---|---|---|
| Block ads | `block_ads` | at the next ad request |
| Block cookie notice | `block_cookie_notice` | at the next resume |
| Clean share links | `clean_share_links` | at the next share |
| Disable analytics, Keep watch progress | `disable_analytics`, `keep_watch_progress` | at the next upload |
| Hide Premium upsells | `hide_premium_upsells` | when the screen is drawn again |
| Hide Shorts | `hide_shorts` | after a restart |
| Hide tipping and Rants | `hide_tipping` | when the data is loaded again |
| Default video quality (a choice; "Rumble's default" is 0) | `video_quality` | at the next video |
| No autoplay of the next video, No feed previews | `autoplay_next_video_off`, `feed_previews_off` | after a restart |
| Unlock Premium | `unlock_premium` | after signing in again |

Resource patches (Lurk icon, Remove tracking permissions, Clone app, Change app name) change the
APK itself, so they aren't listed.

How it works:

- `rumble/settings/SettingsPatch.kt` is a hidden patch the others depend on. It merges
  `extensions/shared.mpe`, adds the extension's `SettingsActivity` to the manifest (with its own task
  affinity, so the launcher shortcut never opens inside Rumble's picture-in-picture task), and calls
  `Settings.init(context)` first thing in `RumbleApplication.onCreate()`.
- The Lurk row is a fourth `RowItemWithArrowView` after Send Feedback in the `AppInfoSection`
  composable, drawn by a static method the patch adds next to it with the same arguments as Rumble's
  rows. Its onClick is a `kotlin.jvm.functions.Function0` the extension makes with a `Proxy`, since
  the extension doesn't compile against Kotlin.
- Each patch registers its switches at the end of `execute`, so a patch that is left out or fails
  shows none. The settings patch writes them as JSON into `Settings.registryJson()` in `finalize`.
- Injected code calls `Settings.isEnabled(key)` (or `getInt(key)`) and runs Rumble's original code
  when it returns false. Values live in the `lurk_settings` SharedPreferences, written only when
  changed on the screen; unknown keys count as on.
- Restart Rumble (shown after changing a switch that needs it) relaunches the launcher activity and
  exits the process.

Validated on 2026-10-14 on the Android 16 emulator with a local build, every patch except Clone app
and Change app name applied (Gradle still can't reach GitHub Packages):

- The Lurk row appears after Send feedback and opens the screen in Rumble's task; Back returns to Settings.
- The launcher shortcut opens it full screen, also while a video plays in picture-in-picture.
- Hide Shorts off + Restart Rumble: the Shorts tab and Home row come back. On + restart: hidden again.
- The same walk-through (fresh launch, the featured video for 35 s, three scrolls) through the logging
  proxy: with Block ads and Disable analytics on, only Rumble's own hosts and `graph.facebook.com`.
  Both off: `e.rumble.com`, `a.ads.rmbl.ws`, `d4.ads.rmbl.ws`, `imasdk.googleapis.com` and
  `pagead2.googlesyndication.com` are back. Both on again: gone again. Each run started from a stopped
  app, so changes taking effect mid-session weren't checked separately.
- Keep watch progress is disabled while Disable analytics is off. The quality picker, Reset to
  defaults and dark mode (including switching while the screen is open) work.
- `bin/test-rumble` passes on the patched APK. A build with only Clean share links and Clone app
  shows just that switch and works under `com.rumble.battles.lurk`.

## Finding code in 3.21.0

R8 renames Rumble's classes and methods, so the patches below find code through what survives:

- Constructors check each parameter by name (`"eventRemoteDataSource"`, `"rumbleAdRepository"`, …).
- Kotlin data objects return their source name from `toString()` (`"Success"`, `"EmptyVideoAdList"`).
- Enum constants keep their names in `<clinit>` (`"SHORTS"`, `"Tip"`, `"OFF"`).
- Compose functions carry a trace label (`"com.rumble.battles.….ShortsListView (ShortsListView.kt:65)"`),
  matched without the line number.
- Gson DTOs in `com.rumble.network.dto` keep their class and field names; only the getters are renamed.

`shared/Bytecode.kt` has the helpers for these. Each patch fails with a message rather than guess when a
lookup doesn't find exactly one match.

## Disable analytics

Rumble's event repository (constructor strings `eventRemoteDataSource`, `appRequestName`,
`watchProgressDao`) sends every first-party event:

- to `e.rumble.com`: clicks on videos, categories, playlists, related videos, Shorts and the Premium button;
  viewable impressions; downloads; and watch progress. Watch progress carries the advertising ID, volume,
  playback rate, quality and fullscreen state.
- to Rumble's debug log (`debug/log2`): copies of AppsFlyer and Firebase events, plus search result clicks.

Bodies include the installation ID, and most events add the user ID, username and AppsFlyer ID. Only four
of the event use cases check consent, so Block cookie notice doesn't stop these. The patch returns at
the top of the three upload methods: `Success` for event lists (as after an upload, so queued watch time is
cleared rather than retried) and `Unit` for the other two.

The Keep watch progress switch (initially the `keepWatchProgress` option) lets an upload through when its list holds a watch progress event (found
by its `toString`, `"WatchProgressEvent(timeRange="`). Rumble's watch history is server side
(`watch_history.clear`), probably fed by these events; that is untested, as it needs an account.

## Block ads

The ad repository is the one implementation of the interface the video ad use case takes first
(constructor strings `rumbleAdRepository`, `getCustomAdTag`, `createPreRollVideoIdUseCase`). The patch
answers each request with a result Rumble already handles:

- **Video ads** (pre-rolls, mid-rolls and live mid-rolls, played through Google's IMA SDK):
  `EmptyVideoAdList`.
- **Feed and video page display ads** (`a.ads.rmbl.ws` / `a-delivery.rmbl.ws`, which receive the zone,
  keywords, user and video IDs, `ad_device_id` and the consent strings): `WaitingForAds`. Both injectors
  treat it like a failed request.
- **Impression pixels and click/progress beacons**: success. Boosted (promoted) videos stay in feeds,
  but their `d4.ads.rmbl.ws` impressions and clicks are no longer reported.

The Revcontent DTOs in the app are unused.

## Remove tracking permissions

Removes `AD_ID`, `ACCESS_ADSERVICES_AD_ID`, `ACCESS_ADSERVICES_ATTRIBUTION`, `AD_SERVICES_CONFIG`, the
install referrer permission, the Samsung and Huawei store referrer permissions and `READ_PHONE_STATE`.
It also removes the AppsFlyer referrer `<queries>` entries.

- Without `AD_ID`, Google Play services returns the all-zero ID. That covers every SDK and Rumble's own
  `ad_device_id` (home layout, banners, watch progress); Rumble's reader doesn't filter zeros.
- `READ_PHONE_STATE` is declared by the app but never requested, and no code needs it.
- Every other user catches the failure or checks the permission first.

## Hide Shorts

- **Bottom bar:** Other code refers to tabs by their position in the tab list (`ContentViewModel` saves
  index 0 or 3 as the default tab). So the Shorts entry stays in the list, and the bar's drawing loop
  skips it while still counting it. The Shorts route is found by `toString` (`"ShortsScreen"`).
- **Shorts rows:** `ShortsListView` returns immediately. That covers Home, My Feed, channels, search
  and My Videos.
- **Home sections:** `HomeSectionKind` maps `"shorts"` to `HIDE`, so the section isn't loaded either.
- **Channel tabs:** `Creator.tabs` loses `"shorts"`, unless it is the channel's only tab.
- **Category tabs:** the `SHORTS` entry is removed from the list the category tab builder makes.
- **Search filter chips:** `SHORTS` is removed from the copy of the filter enum's entries used for the chips.

`rumble.com/shorts/…` links still open the Shorts player.

## Hide Premium upsells

- The video page's Premium sheet: the check behind `premium_promo_display_delay_days` returns false.
- `PremiumBannerView` (Upgrade Now on Home and My Feed), `PremiumOfferBanner` (account screen) and
  `DownloadsUpsellView` return immediately.
- Live chat's Premium gift list (`LiveChatConfig.giftList`) is null.

The Premium tag on premium-only videos and the comment/chat paywalls stay, since they describe content.
Premium status itself isn't touched; that's Unlock Premium's job.

## Hide tipping and Rants

- `Tip` is dropped from the list of action buttons the channel and video headers may draw.
- `Creator.tipsInfo` and `VideoSource.wallet` (the Shorts tip button) are null.
- `LiveChatConfigRant.enable` returns false, which hides the Rant button.
- `LiveChatConfigRant.walletEnabled` returns false, which disables paying with Rumble Wallet.

Rants other viewers send still show.

## Disable autoplay

Both options change a DataStore default in `UserPreferenceManager` (constructor strings `autoplayOnKey`,
`playbackInFeedKey`, `videoQualityKey`). The patch follows each key to its field, the field's synthetic
accessor, and the inner class reading it. It then changes the value used when nothing is stored, so
Settings > Playback still works.

- **Next video:** `autoplayOnKey ?: true` becomes `?: !autoplay_next_video_off`.
- **Feed previews:** `playbackInFeedKey ?: ALWAYS_ON` becomes `?: OFF` while `feed_previews_off` is on. Home's featured video asks its
  player builder to play even when feed playback is off; the patch clears that flag, so the featured card
  follows the setting too.

## Default video quality

The player chooses its start source with (sources, last picked height, last picked bitrate, the server's
preferred height, isLive, data saver, live uses Auto). That signature is unique, with the string
`sourceList`.

- With no picked quality, Rumble uses `preferred_video_resolution`, which is 480p on phones.
- The patch fills in the chosen height and the highest bitrate, and Rumble's own nearest-height logic does
  the rest.
- A quality picked in the player is stored app-wide and keeps winning.
- Data saver, and Auto for live streams, still take precedence.

## Clone app and Change app name

Clone app sets the manifest package (default `com.rumble.battles.lurk`) and moves everything another
install would conflict with:

- permissions under `com.rumble.battles.` and `com.rumble.READ_RUMBLE_DATA`, with every reference to them
- provider authorities, including `com.rumble.provider`. That one is also a string in code, which the
  patch rewrites; Rumble uses it for the provider its other apps read.
- intent categories and task affinities carrying the package name

Class names stay. The app ID Rumble sends its API (`com.rumble.battles`, a constant) stays, so the server
sees an ordinary Rumble client. The patch runs in `finalize`, after other patches change the manifest.

Change app name sets `app_name`, the application's label; only `res/values` defines it. Quotes,
backslashes and a leading `@` or `?` are escaped.

## Clean share links

Every share sheet goes through one share use case: videos, live streams, Shorts, reposts, playlists and
channels. It takes `(text, title)`, and `android.intent.extra.TITLE` appears nowhere else in the app. The
patch cuts everything from the first `?` off the link when its host is `rumble.com` or a subdomain; other
links are left alone.

3.21.0 doesn't tag the links it shares:

- The use case gets the server's `url`, or `https://rumble.com/shorts/<id>`, unchanged. `e9s` and
  `src_v1_*` only reach analytics events.
- Shared links checked on 2026-10-14 had no query string, including a video opened from a Home
  recommendation row.

So the patch is a guard in case the server or a later version starts adding tags.

## Validation on 2026-10-14 (new patches)

All built with the local fallback (Gradle still gets 401 from GitHub Packages) against Morphe Desktop
1.18.1. All 14 Rumble patches apply together to 3.21.0.

Network was checked on the Android 16 emulator through a logging CONNECT proxy (the emulator's global
`http_proxy`). Each build ran the same walk-through: fresh install, signed out, Home, a live video for 40 s,
three scrolls, background.

- **Stock:** connects to `e.rumble.com`, `a.ads.rmbl.ws`, `d4.ads.rmbl.ws`, `imasdk.googleapis.com`,
  `pagead2.googlesyndication.com`, Ketch, AppsFlyer, Airship, Crashlytics and `app-measurement.com`.
- **Disable analytics + Block ads:** no `e.rumble.com`, `a.ads.rmbl.ws`, IMA or `pagead2` connections. The
  `d4.ads.rmbl.ws` pixel stopped after impressions were included.
- **All privacy patches:** only Rumble's own hosts (`rumble.com`, `hugh.cdn.rumble.cloud`, `wn0`, `web7`)
  and Firebase installations/Remote Config/logging, `graph.facebook.com` (the Facebook SDK's settings
  fetch) and Google Play services. No crashes, and no ART verifier errors in logcat.
- **`keepWatchProgress`:** one `e.rumble.com` connection, when a video starts; no crashes.
- **UI against stock:**
  - The bottom bar has no Shorts tab, and tabs still select correctly.
  - Home has no Shorts row, Premium banner or video Premium sheet.
  - Search shows All / Videos / Channels; stock adds Shorts.
  - On The Dan Bongino Show's channel, stock has a Tip button and Videos / Shorts / Reposts tabs; patched
    has no Tip and Videos / Reposts.
  - Settings shows Autoplay next video off and Playback in feeds Off; turning autoplay back on persists.
  - The featured card stays still (identical frames 2 s apart) but plays when opened.
  - A regular video starts at 1080p (with 360p / 720p / 1080p / Auto available).
- **Clean share links:** the share use case was called directly from the APK on the emulator
  (`app_process`, a Context that captures the share intent). Patched: rumble.com, `www.rumble.com` and
  Shorts links lose their query string and fragment; `notrumble.com` and `example.com` links are unchanged.
  Stock passes all of them through. Sharing a live stream in the app with all patches shows the clean link.
- **Clone:** `com.rumble.battles.lurk`, "Rumble Lurk" with the Lurk icon, installs and runs next to stock
  Rumble.

Not verified at runtime: category tabs, Rants and the Rumble Wallet (both need an account), the account
screen offer banner, and the downloads upsell.
