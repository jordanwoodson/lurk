# Twitch patch work

## Objective and current state

Apply the same workflow used for Rumble and Kick: inspect the original app, implement
the viable equivalents of the existing patches, verify the resulting APK and runtime
settings, and publish a tested stable Lurk release through the repository workflow.

The supported target is `tv.twitch.android.app` **31.6.0**, version code **3106006**.
The user supplied the original APKMirror bundle on 2026-10-14. `bin/apk-add --copy`
verified its signature and stored it in `apks/twitch/31.6.0/`. The source upload was
left intact. The universal variant supports Android 12L+ (API 32), with arm64-v8a,
armeabi-v7a, x86 and x86_64 splits. The base APK SHA-256 is
`9e540dc16e8c6bb2c10ff06600f11576afe671b6f4cc7ba3ee80985b523d45b8`.

Allowed sources are APKMirror, Google Play and Uptodown, as required by `CLAUDE.md`.
The supplied variant is listed here:

- [APKMirror release and variants](https://www.apkmirror.com/apk/twitch-interactive-inc/twitch/twitch-live-streaming-31-6-0-release/)
- [APKMirror universal bundle](https://www.apkmirror.com/apk/twitch-interactive-inc/twitch/twitch-live-streaming-31-6-0-release/twitch-live-streaming-31-6-0-android-apk-download/)

APKMirror reports the signing certificate SHA-256 as
`1338f9b049893cc70b78432a177582f90bd4bc6296ea4ed35bcc7df59687ac53`.
The supplied APK's verified signature matches this certificate, which is now pinned
in `twitch/shared/Constants.kt`. Decompiled Java, smali and resources are under
`~/scratch/morphe/twitch/31.6.0/`. Twitch also embeds a Hermes v98 React Native bundle
and web-view JavaScript; native screens alone do not cover every feature.

## Coverage

The implementation provides 13 public Twitch patches. These are original patches
based on this APK; the Rumble and Kick fingerprints do not apply directly.

| Rumble patch area | Twitch equivalent and boundary |
| --- | --- |
| Change app name | Opt-in launcher label, default `Twitch Lurk`. |
| Lurk icon | Purple Lurk artwork, adaptive/round/monochrome launcher resources. |
| Remove tracking permissions | Removes the two present advertising-ID and Play install-referrer permissions. This alone does not stop other identifiers or telemetry. |
| Clone app | Opt-in package `tv.twitch.android.app.lurk`; renames all nine provider authorities, including Amazon's suffix, and the app's signature permission. Amazon sign-in and verified app links are unverified after re-signing. |
| Disable analytics | Native/RN Spade, local vendor consent decisions, ComScore permission, Branch events/tracking mode, Sentry and Crashlytics collection controls. Details below. |
| Clean share links | Native/RN share intents and clipboard paths; removes known tracking parameters only on Twitch-owned hosts. Keeps timestamps, unknown parameters, fragments and unrelated URLs. |
| Disable autoplay | Separate switches for fresh feed-preview defaults and automatic next-video/clip advancement. Manual playback stays available. |
| Default video quality | Opt-in default rendition, Source initially. Saved choices including Auto win; unavailable renditions use Twitch's fallback. |
| Hide Shorts | `Hide Clips`: Home discovery and native channel-profile Clips tabs. Clip routes remain present. |
| Hide tipping and Rants | `Hide Bits tipping`: purchase/cheering controls, including native and RN chat buttons. Received chat messages stay untouched. |
| Hide Premium upsells | `Hide subscription upsells`: Subscribe/Gift prompts and Turbo promotions. Existing subscriber status and native member-management rows remain. |
| Block cookie notice | Hides the automatic notice only. Does not accept consent, change saved consent or disable manual privacy settings. |
| Block ads | `Block client ads`: takes the existing native client ad-decline path and completes its result. Does not remove ads stitched into streams or cover every RN ad path. |
| Unlock Premium | No patch. Local Turbo state exists, but no useful equivalent entitlement change was established. Signed playback/account benefits are server controlled. |

## Implementation evidence

### Privacy and client ads

Native Spade dispatch completes the nullable success callback exactly once rather
than leaving callers waiting. The native batch transport is also guarded. RN's
`createSpadeReporter` retains its object/API while enqueue and flush return their
normal synchronous `undefined`. Local SDK consent lookup returns the actual Denied
enum without writing the account's consent model. ComScore's local permission is false.

Branch retains its SDK request/callback state machine with tracking-disabled mode
forced on and explicit event reporting suppressed. That mode permits some link
requests; the patch does not claim zero Branch traffic. Sentry's three startup
providers and explicit initialization are guarded. Crashlytics' automatic collection
arbiter and both collection setters are disabled, along with explicit report-send,
log, exception and user-data methods. Its fatal handler may still retain local crash
files. Firebase's initialization provider and messaging remain intact. No
Firebase Analytics implementation was found in this APK.

The cookie patch changes only `ConsentOptions.shouldShowNotification`. Manual
settings, vendor values, age restrictions and DMA fields retain their original
values. Pair this with Disable analytics when local SDK restrictions are desired.
The ad patch targets one branch of a merged RxJava callback and returns the same
`Optional<Boolean.FALSE>` as Twitch's own `decline_all_ads` path. Other synthetic
branches remain unchanged. Authenticated streaming is needed to measure its actual
ad coverage; this is not a SureStream/server-side ad remover.

### Playback and sharing

Feed previews change the missing `autoplay_enabled` fallback to Never in both the
initial getter and observer. Existing Always, WiFi-only and Never values remain
authoritative. The next-video control is separate: it hides/cancels the native
recommendation countdown after binding the manual Play card, gates only the automatic
dispatch event, and disables the RN feed's automatic clip-advance hook. Manual
selection and swiping are preserved. This version has no implemented native getter
for the RN clip-advance preference, so Lurk's next-video switch controls that path.

Quality defaults affect missing native live/VOD/clip preferences, the last-used
video fallback after audio-only playback, and an absent RN `preferred-video-quality`
storage row. They never replace saved values. Source maps to `chunked` for live/VOD
and `source` for native clips. RN shares one exact rendition name across content
types; if that name is unavailable, its existing effect resets to Auto. Named
resolutions likewise require an exact match, including frame rate. This is not a
nearest-resolution selector and does not force a bandwidth minimum.

Sharing hooks cover six native/RN intent-producing methods and three clipboard
boundaries. They clean only `EXTRA_TEXT` among intent string extras and preserve
unchanged clipboard objects. Known UTM/click parameters and `tt_*` campaign fields,
including the `tt_click_id` UUID generated by this build, are removed. Timestamp
parameters, clip identifiers, unknown parameters and raw URL encoding are preserved.

### Interface and entitlement

RN Home filtering removes Clips from the tab labels, index model and pager children
together. Explicit clip requests, including initial requests with an empty Following
feed, use Twitch's existing native clip player. The initial effect retains its
deduplication so rerenders do not open the same clip repeatedly. Native channel tabs
filter the app's own `EnumEntries` through a read-only proxy, preserving the expected
interface and order. Explicit clip IDs, clip content and Clips profile scopes retain
their native tab. Native and RN Bits
flags are disabled, while dedicated purchase/tray/one-tap components return null.
Ordinary received chat messages are not filtered.

Subscription suppression distinguishes Subscribe/Gift states from existing
subscription state. RN Subscribe rendering retains the subscribed indicator through
normal, icon and cached-render paths. Dedicated Gift, Turbo and Theatre ad-upgrade
components are hidden. Native Turbo signup/startup offers and the account promotion
row are skipped; existing Turbo members retain their management row, and Subscriptions
and Drops remain. Intentional paywalls, direct purchase flows and other WebView
promotions are outside the tested hooks.

`UserModel.getHasTurbo` feeds stored profile/observer state and multiple consumers,
including portal state, promotion UI, player analytics and Drops. RN also queries
`hasTurbo`/`turboStatus` separately. Changing this bit cannot establish signed playback
or account entitlement, so no `Unlock Turbo` patch is advertised.

### Runtime settings and Hermes

Only included patches register controls in Lurk's non-exported settings activity.
Settings initialize before affected SDK providers and at Application startup, once
per process. Long-pressing the launcher icon opens the Lurk shortcut.

The original Hermes v98 bundle SHA-256 is
`50e73cce33e67272f9eb9a38b8d8e6f1ecd06ceccf4b88cc9dbc091743493fc9`.
Patch-time parsing pins that hash/version, verifies function boundaries and unique
instruction patterns, and permits only equal-length non-overlapping edits. The
runtime loader validates the embedded hash and edit manifest, snapshots switches,
applies original/replacement bytes, and recomputes the Hermes footer. All switches
off must reproduce the independently hashed stock bundle. Changes require a restart.

The shipped release `RNSourceModePolicy` disables Metro and selects the embedded
asset through `RNReactHostFactory` and `DefaultReactHost`; the file path argument is
null. The native asset loader is the hook. Although HotUpdater SDK classes are
present, no app call selecting a HotUpdater bundle URL was found, so no speculative
update bypass is added.

## Verification and limits

The test device is the disposable Android 16 x86_64 `emulator-5580`. Original
Twitch and the patched clone install together. Resource checks cover the label,
all icon layers, exact permission delta, nine provider authorities and their
protection attributes, component class identities and non-exported settings.
The patched APK passes signature and 16 KiB alignment verification; Android's
package compilation in verify mode succeeds.

The real settings activity displays included controls, publishes a launcher
shortcut, persists analytics off/on changes, and restarts into a new process from
its Restart Twitch action. Both states reach the same sign-in screen as stock
without fatal exceptions, missing-method errors or verification errors in the
captured startup logs. The native-only ad/consent selection has exactly two controls,
leaves the RN loader untouched and retains the original Hermes asset without a
manifest. The independent interface/playback selection also patches successfully.

Android probes execute the injected loader and actual app classes, not stand-ins
for the target methods:

- `bash bin/test-twitch <patched.apk>`: embedded defaults, process snapshot,
  independent switches and exact all-off reconstruction of the original bundle.
- `bash bin/test-twitch-privacy <patched.apk>`: real Spade callback once/null/off
  paths, completed client-ad emitter, Crashlytics upload permission and four
  dispatch guards. Disabled controls reach the deliberately empty original
  dependencies, proving they resume original execution.
- `bash bin/test-twitch-interface <patched.apk>`: real enum ABI, ordering,
  immutability and original-list identity, plus native Bits state constructors.
  `--stock <base.apk>` runs the original enum/chat negative controls.
- `bash bin/test-twitch-playback <patched.apk>`: real recommendation binding with
  next-video suppression off must reach the original progress-bar operation.
  `--stock <base.apk>` runs the original control path.
- `bin/test-twitch-interface-hermes <stock.bundle> [patched.bundle]`: production transforms and
  actual caller instructions for the two-tab model/pager, initial selection, swipe
  bounds, explicit native clip callbacks and empty-feed initial-effect deduplication.

The Python verifiers under `tests/twitch/` inspect emitted DEX and use the original
JavaScript/Hermes as behavioral inputs. They check callback completion, untouched
synthetic cases, saved/default quality and autoplay choices, automatic versus
manual playback, consent immutability, interface state and outgoing link boundaries.
An independent Hermes decoder checks all 28 edits across 21 functions, including
instruction boundaries and branches, with all five controls on, all off and each
on alone. `verify_interface_hermes_structure.py` also checks the 18 interface
functions and switch targets using the production interface probe's manifest.
`verify_playback_links.py`,
`verify_interface.py` and `test_privacy_consent.py` compile production Java helpers
with boundary fixtures. The existing Rumble and Kick Android probes also pass on
APKs made with the expanded patch bundle.

**Authenticated live/VOD playback, native clip navigation, chat, actual server ad delivery, purchases,
subscription/member screens and Amazon sign-in remain unverified.** Stock and
patched Twitch both require Log In or Sign Up, including a direct channel URL;
no test account/session was available. Bytecode and callback probes establish
specific local behavior, not complete authenticated end-to-end coverage. The
app also contains WebView code outside the covered native/RN presentation hooks.

Analysis, decoded APKs, build logs and emulator captures remain under
`~/scratch/morphe/twitch/31.6.0/`, `build-twitch/`, `twitch-privacy/` and
`twitch-interface/`. The original upload is untouched.

## Implementation and release requirements

Use original, app-specific fingerprints based on strings, signatures and instruction
patterns. Do not copy third-party Twitch patches. Reuse the repository's shared Lurk
settings screen and extension infrastructure; register only settings for included
patches. Restore original execution when runtime switches are disabled. Features
read only during initialization must clearly require a restart.

Before each behavior change, establish a meaningful failing check against stock or
an isolated production helper. Verify emitted bytecode, register safety, return
contracts and callbacks as well as user-visible behavior. Test the full patch set
and independent combinations where dependencies could hide a failure. Use only the
disposable `emulator-5580` for runtime checks.

The release gate requires successful patching of the verified original package,
APK signature/alignment checks, startup and representative playback/chat checks,
settings toggling, and Rumble/Kick regression checks if shared code changes. Record
unavailable authenticated coverage explicitly. No unsupported feature should be
listed as implemented merely to match another app's patch count.

Work on `dev`. After local verification and review, push a semantic feature commit,
validate the workflow-built prerelease, merge `dev` into `main` without squashing,
and validate the published stable bundle and generated Morphe source metadata.
Do not edit generated release files or publish a release by hand.
