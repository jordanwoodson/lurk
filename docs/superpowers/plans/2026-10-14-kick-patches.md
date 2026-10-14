# Kick patches implementation plan

> **For agentic workers:** Use superpowers:executing-plans for implementation and superpowers:dispatching-parallel-agents for independent investigations. Track progress here and in the scratch ledger.

**Goal:** Implement the requested Kick equivalents of the Rumble patch set, validate them against Kick 40.32.1, and publish a verified Morphe patch-bundle release through this repository's release workflow.

**Architecture:** Keep app-specific patch definitions and fingerprints under `patches/.../kick/`. Reuse shared Java settings and existing resource editing patterns. Add fail-closed Hermes v99 editing and Kick settings integration for JavaScript behavior, with embedded-bundle update protection. Native SDK patches must preserve asynchronous completion and playback functionality.

**Tech stack:** Kotlin/Morphe 1.15.1, morphe-patches-library 1.8.0, Java Android extension, Hermes v99, existing Android SDK/JDK 21, Python verification scripts.

**Spec:** `docs/kick.md` records the inspected targets and evidence. The user's instruction is to implement all patches and release; uncertain equivalents remain open until researched or explicitly resolved with the user.

## Global constraints

- Work on `dev`; release with semantic commits and the existing GitHub workflow. Do not hand-edit generated patch lists, changelog or README patch table.
- Target only `com.kick.mobile` 40.32.1 / 60006965 and the verified signing certificate.
- Write original patches; only official Morphe repositories may be used for patch API reference.
- Temporary builds, test scripts/output, logs and emulator files go under `~/scratch/morphe/`; reuse installed toolchains and AVD.
- Preserve explicit user preferences, valid playback, authentication and callback/Promise completion.
- Never describe telemetry suppression or hidden ad UI as verified media-ad blocking.
- Preserve Rumble functionality and shared extension packaging; never bundle another Kotlin standard library in the APK extension.
- User authorized implementation and publication. Proceed through routine choices without additional approval gates; user input is pending only for the genuinely ambiguous Rumble-only feature mapping.

## Review focus

- Upgrades with cached Expo updates must still execute the patched bundle.
- A patched default must not override a later explicit quality/preview choice.
- Optional patch combinations must register only their own settings and retain valid bytecode.
- Returning early from SDK methods must resolve promises/callbacks and avoid retry loops.
- Original and cloned app must coexist without provider collisions or weakening exported token-provider access controls.

## Task 1: Resource patches and stock negative controls

Files: `patches/.../kick/{appname,icon,permissions,clone,updates}/`, `tests/kick/verify_apk.py`, `docs/kick.md`.

Interfaces: consume `COMPATIBILITY_KICK`; produce independent Morphe resource patches, including a reusable internal embedded-bundle dependency.

- [x] Create verification that rejects stock tracking permissions, original app label and enabled Expo updates for the selected output profile. Run against stock and observe the intended failures.
- [x] Implement app name, tracking permissions, icon and clone patches with exact manifest/resource validation; implement embedded-bundle protection for Hermes dependents.
- [x] Build/apply resource patches and verify the selected changes in the resulting APK, signature/alignment and fresh/clone installs.

## Task 2: Kick settings and Hermes editing

Files: `patches/.../kick/shared/Hermes.kt`, `patches/.../kick/settings/`, app-specific extension files if needed, `tests/kick/`.

Interfaces: provide guarded function/literal edits of the v99 bundle, exact match validation and SHA-1 correction through `hermesPatch`; provide app-specific settings initialization and runtime access for Hermes patches.

- [x] Test actual bundle parsing/editing contracts, wrong-version and ambiguous-target rejection, unchanged length and valid footer. Run expected negative controls before implementing.
- [x] Implement minimal parser/edit helpers based on the bundled bytecode layout, not decompiled JavaScript text or fixed offsets.
- [x] Integrate shared Lurk settings via `MainApplication.onCreate`, accessible launcher shortcut and a Kick-native/React entry point where supportable.
- [x] Prove a toggled setting affects actual Hermes behavior and restart semantics in the app. Review the disabled Expo controller; document that a real downloaded OTA was not available for a cache-upgrade test.

## Task 3: Playback and UI equivalents

Files: `patches/.../kick/{playback,tipping,upsells,clips,links}/`, fingerprints alongside patches, `tests/kick/`.

Interfaces: consume Hermes helper/settings and embedded-bundle dependency; independently register feature switches/options.

- [x] Add stock-negative/output-positive tests for preview defaults, quality defaults and selected UI component return behavior.
- [x] Implement preview autoplay defaults and initial quality, preserving stored/explicit selections.
- [x] Implement Hide KICKs/gifting, Hide subscription prompts and Hide Clips with parent layout and navigation coverage.
- [x] Implement host-scoped clean sharing if preserving legitimate link parameters can be verified.
- [x] Verify individual and combined patch output and runtime navigation/playback.

## Task 4: Privacy, ads and remaining equivalents

Files: `patches/.../kick/{analytics,ads,consent,premium}/` as supported by research; `tests/kick/`; update evidence in `docs/kick.md`.

Interfaces: consume shared settings, native bytecode helpers, Hermes editing and update protection as needed.

- [x] Trace and test SDK and first-party telemetry behavior before introducing blocking hooks.
- [x] Implement analytics suppression across verified independent paths, preserving Promise completion, login/chat and stream acquisition.
- [x] Investigate actual ad delivery and implement the supported VOD fallback as experimental/off by default; preserve playback acquisition and document that live SSAI removal is unsupported.
- [x] Resolve cookie-consent and Premium-equivalent scope with the user; implement any confirmed corresponding behavior and explicitly record unsupported cases.
- [x] Verify runtime switches and public playback with stock/patched controls; record that zero-telemetry network coverage and authenticated flows remain unverified.

## Task 5: Integration and release

Files: final source/tests/docs, generated release artifacts via workflow only.

- [x] Run full project build, actual APK patch application (all supported patches plus representative optional combinations), DEX/Hermes integrity checks and emulator probes.
- [x] Verify Rumble regression behavior and extension packaging.
- [x] Obtain independent whole-change code review; resolve important findings and rerun covering checks.
- [ ] Commit with semantic messages on `dev`, push, watch release workflow to terminal success, inspect published `.mpp` and Morphe metadata, and validate the downloaded release artifact.
- [ ] If stable publication is required, integrate through the repository's existing main/release process and verify that release too.
- [ ] Run home-lint, report retained scratch files and the published release/version. Mark goal complete only after the scope and published artifact are proven.

## Progress

- Baseline `dev`: `db6357c`; pre-existing changes are the analysis report and first-import Kick certificate pin.
- Independent investigations running: ad delivery/blocking; Hermes/runtime settings and UI targeting.
- Build baseline started; no Android devices were attached at the initial check.
- Implementation and release remain incomplete.

## Reviewed scope and validation limits

- All twelve viable Kick equivalents are implemented. Cookie-notice and Premium-unlock have no established Kick counterpart. Live SSAI removal is unsupported; the VOD fallback is experimental and off by default.
- Local fallback compiler and actual APK patching pass. All 25 Hermes edits in 20 functions pass independent decoding and off-state restoration. The Android loader probe and actual app restart prove setting-selected code executes.
- Native-only and Clips-only combinations register exactly their selected settings and preserve the appropriate Expo mode. Rumble patching and the existing profile/entity probe pass.
- Home, Browse, channel Videos, public live playback and incoming chat work on the test emulator after the stock-proven native-library extraction workaround. Settings and restart action work.
- Independent review caught and resolved the quality reset regression and queued Insider jobs; both now have actual-artifact regression checks.
- Zero network telemetry, authenticated flows, broadcaster token sharing, full live-ad blocking and every cached OTA/server configuration are not claimed. The disabled Expo controller is verified in source and startup; a real downloaded OTA was not available in the stock cache.
- Release workflow/build/download validation remains pending.
