# Twitch patches implementation plan

**Goal:** Implement, verify and release the viable Twitch equivalents of the Rumble
and Kick patch sets, preserving playback and runtime controls.

**Architecture:** Add app-specific bytecode/resource patches using the existing
Morphe bundle and shared Java settings extension. Trace native and React Native
callers before selecting each hook; do not assume the Kick Hermes edits transfer.

**Tech stack:** Kotlin/Morphe, dex/smali, Java Android extensions, Python output
checks, Android SDK 36 emulator, existing Hermes analysis tools.

**Spec:** [Twitch requirements and coverage](../../twitch.md).

## Constraints

- Target `tv.twitch.android.app` 31.6.0 (3106006), original signing certificate
  `1338f9b049893cc70b78432a177582f90bd4bc6296ea4ed35bcc7df59687ac53`.
- Original patches only; official Morphe references are allowed.
- Work on `dev`; generated release metadata stays workflow-owned.
- Temporary files remain under `~/scratch/morphe/`; runtime tests use emulator-5580.
- User authorized implementation and stable publication through the same workflow
  as Kick. Routine design, testing and release steps do not need repeat approval.

## Shared interfaces and review focus

`twitch/settings/SettingsPatch.kt` provides `twitchSettings: SettingsScreen` and
`twitchSettingsPatch`. `twitch/shared/Bytecode.kt` provides the contextual extension
`Fingerprint.twitchMatch(what: String): Match`, requiring exactly one result.
Feature patches depend on settings only when they inject runtime behavior, and
register only their own included keys. Resource-only selections stay independent.

Review these conditions explicitly:

1. Async analytics and ad methods must complete callbacks/emitters instead of leaving
   playback or startup waiting. Privacy owns the output checks.
2. Stored autoplay/quality choices must survive default changes; first-play resets,
   Auto and audio/video transitions need actual emitted-method checks. Playback owns them.
3. Native and React Native discovery must agree; hiding a tab must not break its
   direct-link route or leave an invalid selected tab. Interface owns those checks.
4. A clone must rename all nine authorities, including Amazon's package suffix,
   while retaining provider protections and compiled component names. Resources owns them.
5. Disabled runtime switches and independently selected patches must preserve original
   behavior. Integration owns combined and independent-selection runtime checks.

## Task 1: Input, resources and runtime settings

- [x] Verify/store input with `bin/apk-add --copy`; inspect the actual manifest and splits.
- [x] Decode Java/smali/resources and identify application startup and launcher.
- [x] Write `tests/twitch/verify_resources.py`; run each expectation against stock and
  confirm it fails for the missing label, icon, clone or permission change.
- [x] Add appname, icon, permissions and clone patches under `patches/.../twitch/`.
  Defaults: name `Twitch Lurk`, clone `tv.twitch.android.app.lurk`; those two opt in.
  Remove only the evidenced AD_ID and install-referrer permissions.
- [x] Add unnamed settings dependency, a non-exported Lurk activity and shared
  registry finalization. Initialize Settings at the start of TwitchApplication.onCreate.
- [x] Build and apply the four resource patches, decode output, run positive checks,
  and install stock plus clone on the emulator.

## Task 2: Analytics, consent and ads

- [x] Trace native Spade, RN analytics bridge, SDK initialization, consent model and
  ad eligibility state machines; record precise targets and limitations in docs/twitch.md.
- [x] Write stock-negative checks for callback completion, consent immutability and
  ad-decline behavior before adding analytics/, consent/ and ads/ patches.
- [x] Verify enabled/disabled emitted methods and preservation of unrelated synthetic
  branches. Execute real Spade/ad completion and Crashlytics methods on Android.
  Authenticated playback is unavailable and remains an explicit testing limit.

## Task 3: Playback and sharing

- [x] Trace autoplay preference read/observer plus RN initial props; trace quality
  read/reset/Auto paths and share generation, including web and RN callers.
- [x] Write meaningful tests for unset and saved choices, explicit quality changes,
  timestamps, campaign parameters, unknown parameters and unrelated link hosts.
- [x] Add playback/ and links/ patches and necessary Twitch-specific Java helpers.
- [x] Verify emitted targets, real native recommendation binding on Android and URL
  boundary behavior. Live/video playback requires an unavailable account session.

## Task 4: Discovery, tipping, upsells and local entitlement investigation

- [x] Trace native and RN Clips navigation, Bits prompts, subscription/Turbo promotions,
  and any local entitlement representation. Record evidence for every coverage row.
- [x] Write stock-negative navigation/state tests; implement clips/, tipping/ and
  upsells/ patches with runtime switches, preserving received messages and subscriber status.
- [x] Investigate local entitlement changes; no useful Turbo equivalent was established,
  so omit an unlock patch and document server-controlled benefits precisely.
- [x] Verify disabled paths, explicit clip routing and valid selection after tabs are
  removed using emitted native/Hermes callers. Authenticated clip navigation remains untested.

## Task 5: Integration, review and stable release

- [x] Build and patch all 13 public Twitch patches. Check APK signature and alignment.
- [x] Run output checks, Android helper/target probes and stock/patched UI comparisons.
- [x] Verify settings shortcut, toggles, process restart and independent patch selections.
- [x] Run Rumble/Kick regression checks for the shared bundle and review the complete diff.
- [x] Fix material review findings and update docs with exact scope and test limitations.

The final local artifact passes native callback, interface, loader and playback
probes, plus emitted DEX and independent Hermes checks. The review fixes cover the
recommendation OFF branch, coherent Clips tab/pager indices, explicit clip dispatch,
initial-effect deduplication and defined returns. Source review is clear.

### Release procedure

1. Commit/push the semantic feature to dev; verify the workflow-built prerelease artifact.
2. Merge dev to main without squashing; download and verify the stable release,
   metadata, attestation and patch application.
3. Sync the local branch and report the release with the authenticated testing limits.
