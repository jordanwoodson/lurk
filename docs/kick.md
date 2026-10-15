# Kick 40.32.1

Static analysis of the supplied APKMirror bundle on 2026-10-14, compared with the 14
Rumble patches in this repository. **None of the existing patches applies to Kick
unchanged.** All declare Rumble compatibility; most also fingerprint Rumble Kotlin
classes or Compose UI. Kick uses React Native/Expo, Hermes bytecode and native
player modules. Several features are good candidates for independent Kick patches.

The assessment below, up to "Workspace result", was the starting point and is kept as
it was written. Twelve Kick patches have since been implemented; the notes from
"Implemented patch set" on describe them and separate tested behavior from the
remaining limitations.

## Input and analysis

- Package/version: `com.kick.mobile`, `40.32.1`, version code `60006965`.
- Supplied bundle: `com.kick.mobile_40.32.1-60006965_1arch_2dpi_22lang_fd6d4ac6871fe5704e9fdc59b19d8d86_apkmirror.com.apkm`.
- Architecture: arm64-v8a; includes density and language splits.
- `bin/apk-add --copy` verified the base APK signature and pinned its certificate in
  `kick/shared/Constants.kt`: `b12320227268c9c37e190b493120a7f6ce83c0e9a761f9213df677b58d095b73`.
  This is a first-import pin, not a comparison against a previously trusted Kick certificate.
- Base APK SHA-256: `d4e58dc6fac60d983608fda6338c489ed8756f55761cb189489dabd22d9544d2`.
- Hermes bundle SHA-256: `a9d911b3f8b640d8e56b7527929a093c2356d97992409c453d167fdf39538d9a`.
- 11 DEX files; `assets/index.android.bundle` is Hermes v99, approximately 12.5 MiB.
  Its SHA-1 footer matches its contents.

`bin/decompile kick 40.32.1` produced Java, smali, resources, Hermes disassembly and
decompiled JavaScript under `~/scratch/morphe/kick/40.32.1/`. `bin/scan kick 40.32.1`
provided an initial SDK inventory. Code inspection expanded that inventory; SDK
presence alone does not establish that it transmits data in a particular session.

JADX reported decompilation errors. hermes-dec 0.1.7 warns that v99 is not formally
supported and emitted 4,922 warnings, including unknown environment registers.
Its output contains 46,821 function entries, 570 with zero bytecode length. Do not
treat reconstructed JavaScript as executable source or a zero-length entry as proof
that a feature is absent. The targets below are supported by manifest/resource
inspection, named bytecode instructions and native code where available; exact edits
still need validation against smali/Hermes instructions and runtime behavior.

## Patch-by-patch assessment

| Rumble patch | Kick assessment | Evidence and required change |
| --- | --- | --- |
| **Change app name** | Straightforward | Kick uses `@string/app_name`, defined as `Kick` only in the base `res/values/strings.xml`. Reuse the resource-editing approach with Kick compatibility and defaults. |
| **Lurk icon** | Straightforward | Kick has launcher and round icons, but adaptive XML lives in `mipmap-anydpi-v26`, with foreground/background WebP resources. Rumble's exact file list would fail. Supply Kick artwork and handle split-resource precedence. |
| **Remove tracking permissions** | Straightforward | Four of Rumble's eight permissions are present. Remove those four; replace the Rumble-specific validation and referrer-query assumptions. This reduces tracking access but does not disable all telemetry. |
| **Clone app** | Feasible; broader verification | Rename Kick's package, declared permissions and 13 provider authorities. Rumble's required `com.rumble.provider` bytecode target is absent. Review deep links, token sharing and sign-in under the new package/signature. |
| **Disable analytics** | Strong candidate; new implementation | Firebase event logging, Insider, Datadog, Mux/Datazoom and first-party player telemetry have separate paths. Rumble's single event-repository patch cannot cover them. Preserve callback/Promise completion and distinguish telemetry from playback/session requests. |
| **Disable autoplay** | Strong candidate for feed previews | `preferencesSettingsSlice` defaults `showStreamPreviews` to true and has a toggle. Change that default while respecting stored preferences. Rumble's separate next-video behavior was not established in Kick. |
| **Default video quality** | Strong candidate; new implementation | The player store starts with `selectedQualityGroupPreset: 'auto'`; it supports `higherQuality`, `dataSaver`, `advanced` and user-selected quality. Native IVS exposes quality selection and an adaptive-quality ceiling. Decide whether the patch promises a preferred resolution or only a ceiling. |
| **Hide tipping and Rants** | Good equivalent: hide KICKs/gifting entry points | `KicksShopButton`, `GetKicksButton`, purchase/send screens and gift-subscription buttons are identifiable. Hide entry points without changing balances, purchase authorization or received messages. Rants and Rumble Wallet themselves have no direct equivalent. |
| **Hide Premium upsells** | Good equivalent: hide subscription/gift prompts | Kick exposes `ChannelSubButton`, `ChannelGiftSubButton` and subscription sheets. Target these UI components; keep genuine subscriber-only content indicators. There is no matching Rumble Premium promotion system. |
| **Hide Shorts** | Feasible equivalent: Hide Clips | `BrowseClipsScreen`, `ChannelClipsScreen`, Clips routes and clip-list queries exist. This would hide Kick clips, which are a different content category from Rumble Shorts. Route/tab and browse/category entry points need their own coverage. |
| **Clean share links** | Feasible, currently low benefit | Traced channel/live/VOD/clip share hooks construct clean path URLs. No tracking query was found in these paths. A host-scoped sanitizer could be added to Kick's share boundary if needed; do not blindly remove functional queries from every shared URL. |
| **Block ads** | Research candidate; full removal unproven | Kick has PAL, Amazon MediaTailor sessions, manifest/tracking URL selection and player ad events. Rumble's empty-ad-repository result cannot be reused. Blocking measurement or hiding an ad overlay does not establish that media segments are ad-free. |
| **Block cookie notice** | No matching target established | Rumble's two Ketch entry-point strings are absent from Kick's DEX strings, and no equivalent Ketch notice flow was found. Kick has a personalised-ads preference and SDK consent APIs; those are different targets. |
| **Unlock Premium** | No useful direct equivalent established | Rumble overrides `UserProfile.isPremium`. That DTO is absent. Kick's `useSubscribeToChannel` reads channel info, payment configuration and the signed-in user's channel relationship. Faking a local subscribed value would not establish paid access or subscription benefits. |

## Concrete targets

Paths in this section are relative to `~/scratch/morphe/kick/40.32.1/`. Function IDs
identify this exact bundle for investigation; they are not version-stable fingerprints.

### Resources and cloning

`apktool/AndroidManifest.xml` and `apktool/res/values/strings.xml` establish the
label, icons, permissions, providers and update configuration.

The four tracking permissions shared with Rumble are:

```text
com.google.android.gms.permission.AD_ID
android.permission.ACCESS_ADSERVICES_AD_ID
android.permission.ACCESS_ADSERVICES_ATTRIBUTION
com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE
```

Kick does not declare Rumble's `AD_SERVICES_CONFIG`, `READ_PHONE_STATE`, Samsung
store-referrer permission or Huawei store-referrer permission. The existing patch
requires all eight, so merely changing `compatibleWith` would still fail.

Kick declares `com.kick.mobile.SHARE_TOKEN` and
`com.kick.mobile.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. Its exported
`com.kick.mobile.tokenprovider` is protected by the former signature permission;
`expo/modules/kickauthtokenprovider/KickAuthTokenProvider.java` also checks allowed
calling packages. A clone must preserve these restrictions while renaming its own
authority/permission. Sharing authentication with the official broadcaster app
cannot be assumed to work after re-signing. Deep-link schemes include `kick`,
`com.kick.mobile` and `exp+kick-mobile`; verify intent routing with both apps installed.

### Playback preferences

In `hermes/disassembly.hasm` and `hermes/decompiled.js`:

- `preferencesSettingsSlice` initializes `showStreamPreviews: true` and
  `muteStreamPreviews: true`. `toggleShowStreamPreviews` is function **35368**.
  This is a concrete default-setting target that can retain Kick's own toggle.
- `usePlayerStoreIVSModuleSlice` is function **16410**. Its initial object includes
  `selectedQualityGroupPreset: 'auto'`, `autoQualityMode: true` and
  `userSelectedQuality: undefined`.
- `setUserSelectedQuality` (**34162**) updates `autoMaxQuality` from the chosen
  height while keeping adaptive quality enabled. `setSelectedQualityGroupPreset`
  (**34163**) handles `auto`, `higherQuality`, `dataSaver` and `advanced`.
- `jadx/sources/com/kick/mobile/kickivsplayerv2/KickIvsPlayerV2View.java` exposes
  `setQuality(String)`, `setAutoMaxQuality(Integer)` and `setAutoQualityMode(boolean)`.
  A literal resolution and an adaptive upper limit have different behavior.

Changing the global player `autoplay` field is not a verified replacement for
Rumble's next-video default. It could affect intentionally opened streams. Target
the preview preference first and trace any next-video policy separately.

### UI and sharing

Hermes contains these useful component/hook anchors:

| Target | Function ID | Intended use |
| --- | --- | --- |
| `KicksShopButton` | 26415 | Hide chat gift-shop entry point. |
| `GetKicksButton` | 26693 | Hide KICKs purchase entry point. |
| `ChannelSubButton` | 26323 | Hide channel subscription CTA. |
| `ChannelGiftSubButton` | 26307 | Hide gift-subscription CTA. |
| `ChannelSubscribeBottomSheet` | 22554 | Trace subscription sheet entry points. |
| `BrowseClipsScreen` | 26980 | Trace browse Clips UI. |
| `ChannelClipsScreen` | 27776 | Trace channel Clips UI. |
| `useShareStreamLink` | 24095 | Builds live, VOD and clip share payloads. |
| `useShareChannelLink` | 27837 | Builds channel share payloads. |
| `useSubscribeToChannel` | 26320 | Shows why channel subscription state differs from Rumble Premium. |

The traced share payloads use `BASE_URL/channel`, `BASE_URL/channel/videos/id` and
`BASE_URL/channel/clips/id`, passed to the React Native sharing library. This does
not prove every share surface is clean, but makes link cleaning a lower priority.

For UI patches, trace parents as well as components so hiding a button does not leave
an empty row or sheet. Keep navigation routes valid if deep links should still work.

### Analytics and ads

Kick's app-level Hermes `logEvent` wrapper (**14492**) appends current/previous
navigation paths and `app_platform: android_mobile`, then calls Firebase Analytics.
Native integration is through
`io/invertase/firebase/analytics/NativeRNFBTurboAnalytics.java`. Manifest collection
defaults are `firebase_analytics_collection_enabled=true` and
`firebase_analytics_collection_deactivated=false`. Crashlytics' manifest collection
default is already false; runtime changes were not tested.

Other concrete surfaces include:

- `com/useinsider/react/RNInsiderModule.java`: initialization and `tagEvent`.
- Datadog native modules/provider and Hermes tracking configuration.
- `com/kick/mobile/kickivsplayerv2/KickIvsPlayerV2View.java`:
  `initializeMuxAnalyticsListener`, `setMuxAnalyticsConfig`,
  `setDatazoomAnalyticsConfig` and `sendDatazoomAnalyticsEvent`.
- Hermes `postPlayerTelemetry` (**16565**): authenticated POST to
  `/api/v1/stream/{id}/playback` on the mobile subdomain. Despite its name this is
  **playback acquisition**: its response supplies stream URLs and configuration.
  The analytics patch deliberately preserves this call.

FingerprintJS integration also exists, including `RNFingerprintjsProModule` and
`useFingerprintId`. Removing `AD_ID` does not disable this code. It needs separate
dependency analysis before deciding whether to include it in an analytics patch.
Likewise, do not remove Pusher/Centrifugo indiscriminately; the package contains chat
transport integration, not just analytics libraries.

`DatazoomAnalyticsManager.java` creates Amazon MediaTailor sessions with optional
tracking URLs and attaches ad-event listeners. PAL handles nonce requests and playback
signals. Hermes `usePlayerStoreAdsSlice` (**16391**) supplies `onAdEvent` (**34139**),
and playback loaders inspect `manifestUrl` and `trackingUrl`.

These establish ad-related integration, but not a tested way to obtain ad-free
media. The next ad investigation should compare actual live/VOD playback responses
and manifests and test both playback and ad delivery. Do not classify disabling PAL,
Datazoom or ad overlays alone as a complete ad blocker. Datazoom's analytics and ad
session handling are coupled, which also matters for the analytics patch.

## Shared infrastructure and implementation order

**Account for Expo updates before shipping Hermes edits.** The manifest enables
`expo.modules.updates.ENABLED`, sets checks on launch to `ALWAYS`, and configures a
production Expo update URL/channel. Downloaded JavaScript could supersede patched
embedded code. The native `expo/modules/updates/UpdatesController.java` has an
explicit disabled-controller path. A Kick-specific dependency should ensure the
patched embedded bundle is used, including when an existing install has cached
updates; verify fresh installs and upgrades. Disabling updates also gives up those
over-the-air fixes until the app is repatched.

The local Morphe `hermesPatch` helper checks the Hermes magic, performs byte-pattern
replacements and recalculates the SHA-1 footer. It is not a JavaScript compiler or
semantic fingerprint engine. Its replacement helper edits the first match and pads
shorter replacements. Add explicit version/match-count checks and preserve bytecode
layout; do not globally replace shared strings such as `auto` or `true`.

Rumble's runtime-settings dependency also needs a Kick adapter. The shared Java
settings activity/registry and launcher shortcut can be reused, initialized from
`com.kick.mobile.MainApplication.onCreate`. The Rumble Compose settings-row hook
cannot transfer to React Native. Hermes switches need a native bridge or another
explicit way to read settings; including the shared extension alone does not provide
that connection. (The runtime bundle loader under "Implementation contracts" now
provides it.)

Do not advertise cookie-notice blocking or premium unlocking for Kick on the evidence
available here. Recheck all targets when changing the supported version.

## Workspace result

The original upload was left in place; the verified copy is in `apks/kick/40.32.1/`.
About 1.7 GiB of extracted/decompiled analysis material remains in
`~/scratch/morphe/kick/40.32.1/` for implementation follow-up. Temporary resource-split
audit extracts were removed. The resource-split audit found no `app_name` or launcher
icon overrides in the supplied language/density splits.

The original assessment checked the 18 documented Hermes function IDs against
nonempty disassembly entries and covered all 14 Rumble patches. Implementation
details and validation follow.

## Implemented patch set

| Patch | Selected by default | Behavior |
| --- | --- | --- |
| Change app name | No | Configurable launcher label; default `Kick Lurk`. |
| Lurk icon | Yes | Adaptive Lurk icon in Kick green, including themed icon. |
| Remove tracking permissions | Yes | Removes the four verified advertising/referrer permissions. |
| Clone app | No | Separate package, permissions and 13 provider authorities; defaults to `com.kick.mobile.lurk`. |
| Disable analytics | Yes | Firebase collection/event bridge, Insider initialization/events/queued jobs, Datadog initialization, Mux/Datazoom and PAL nonce suppression. Promise completion is preserved. |
| Disable autoplay | Yes | Changes the initial feed-preview preference; saved preferences and Kick's own toggle take precedence. |
| Default video quality | No | Configurable adaptive default/Auto ceiling, consistently applied to initialization, reset and cleanup. Explicit player selections retain Kick's normal rules. |
| Hide KICKs tipping | No | Gift-shop, purchase and leaderboard tipping entry points; received gifts remain. |
| Hide subscription upsells | No | Subscribe/gift-subscription prompts, preserving the subscribed badge. |
| Hide Clips | No | Browse/channel/category tabs and channel clip rail. Direct playback routes remain. |
| Clean share links | Yes | Known campaign/click parameters on Kick URLs only; functional parameters, other hosts and attachments remain. |
| Bypass VOD ad sessions | No | Experimental original-VOD-URL fallback. Its runtime switch also starts off. No live-ad removal claim. |

There is no cookie-notice or Premium-unlock patch: no corresponding Kick feature
was established. The VOD patch does not promise removal of ads already stitched
into the source stream. Its generator still completes with an object, allowing
both the VOD and live-rewind consumers to use their existing original-URL fallback.
Live playback authorization and media URLs remain unchanged.

Long-press the launcher icon and select **Lurk settings**. Settings show only the
patches included in that APK. Hermes and SDK-startup switches require a restart;
the screen offers a restart action. The quality height is a patch-time option;
its runtime switch restores Kick's default when disabled.

Hermes patches disable Expo OTA updates, including the controller path that reads
the update database. Native-only combinations leave Expo enabled. Repatch with a
supported new app version to receive JavaScript updates.

## Implementation contracts and verification

The Hermes helper accepts only the pinned v99 bundle SHA-256, parses its function
and string tables, requires unique targets and rejects overlapping or unequal-length
edits. The official Morphe `hermesPatch` performs the actual replacements and footer
update. A generated manifest records the original/default byte ranges. The native
bundle loader takes a settings snapshot. When every switch is at its patch-time
default it loads the embedded asset as is and deletes any earlier private copy;
otherwise it verifies the embedded hash, chooses ranges from the snapshot, refreshes
SHA-1 and atomically writes an app-private bundle. Disabling every switch
reconstructs the original bundle byte for byte; reloading in the same process keeps
the original snapshot. On a manifest/file error it logs and uses embedded defaults.

Verification tools retained in this repository:

- `tests/kick/verify_resources.py`: actual decoded resource/manifest assertions,
  including signature-protected token sharing, intact internet permission and
  (`--analytics`) Firebase Analytics starting disabled.
- `tests/kick/HermesProbe.kt`: actual-bundle parsing, negative controls, uniqueness,
  overlap rejection, preview literals and exact restoration.
- `tests/kick/test_hermes.py`: independent instruction/branch decoding of emitted
  edits, all-on/all-off/individual combinations and SHA checks.
- `bin/test-kick`: executes the injected Java loader on Android, checking defaults,
  process snapshots, independent switches and exact stock restoration; invokes the
  two Insider job guards and checks synchronous completion without SDK dispatch.
- `tests/kick/verify_privacy.py`: production share/PAL helpers with boundary doubles;
  31 assertions, including host spoofing, functional parameters and switch isolation.
- `tests/kick/verify_quality.py`: interprets actual reset/setter instructions to
  verify first playback, Auto, explicit selections and clearing a selection. Its
  negative control reproduces the initial-only patch's 1080p-to-720p reset bug.
- `tests/kick/verify_analytics_jobs.py`: compares actual decoded job methods with
  stock, including unchanged instructions, branches and exception handlers when off.

The combined APK was built and patched with all twelve patches. Its resource,
signature and alignment checks passed. Native-only and Clips-only combinations
passed and registered exactly their selected settings. Rumble still patched with
the shared extension, and its profile/entity integration probe passed.

Runtime tests use the existing Android emulator. Both stock Kick and the patched
clone initially fail in SoLoader because the x86 emulator's direct-APK loader searches
for x86 libraries while this upload supplies ARM libraries. Extracting the original
ARM libraries into each test install's native-library directory allows startup through
the emulator's existing translation layer. This workaround changes only the emulator;
it is not shipped in a patch or APK. The apps coexist, the patched bundle executes,
Home/Browse/channel screens open, and the Lurk screen/shortcut is registered. Browse
and channel tabs show the intended Clips removal, and Videos navigation works.
Public live playback and incoming chat were exercised with analytics and interface
patches enabled. The final combined asset has 25 edits across 20 functions and six
independent Hermes switches; the native sharing/analytics switches are separate.
The visible Hide Clips switch and Restart Kick action were exercised: the process
restarted, its private bundle matched the selected bytes exactly, and Browse's Clips
tab returned. The launcher shortcut points to the non-exported settings activity.

These tests do not establish zero telemetry from the first process instruction.
Firebase's content provider initializes before the application collection hook, so
Disable analytics also sets the manifest's `firebase_analytics_collection_enabled`
default to false; Firebase prefers the value the startup hook persists, so the switch
still turns collection back on.
Authenticated flows, broadcaster token sharing and every server ad configuration
need device/account coverage. Do not interpret SDK suppression or static VOD fallback
validation as verified removal of every video ad.
