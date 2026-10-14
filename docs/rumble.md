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
and replaces that read with `true`. It does not depend on the obfuscated getter name
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
Only the getter's field-read instruction changes. Nullable `premiumData`, account
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
