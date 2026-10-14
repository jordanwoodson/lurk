<p align="center">
  <img src="patches-bundle.png" width="128" alt="">
</p>

<h1 align="center">Lurk</h1>

<p align="center">
  <b>Morphe privacy patches for Twitch, Kick and Rumble.</b><br>
  Watch the stream. Skip the tracking.
</p>

<p align="center">
  <a href="https://morphe.software/add-source?github=jordanwoodson/lurk&name=Lurk"><img src="https://img.shields.io/badge/Add_to_Morphe-FFC247?style=for-the-badge&logo=android&logoColor=0E1016" alt="Add to Morphe"></a>
</p>

<p align="center">
  <a href="https://github.com/jordanwoodson/lurk/releases/latest"><img src="https://img.shields.io/github/v/release/jordanwoodson/lurk?label=release&color=FFC247" alt="Latest release"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-GPL--3.0-blue" alt="License GPL-3.0"></a>
</p>

Lurk is a set of privacy-focused patches for streaming apps, applied with [Morphe](https://morphe.software):
Twitch, Kick and Rumble to start, more apps later. This repo is both the patch bundle and the workspace
for building it. Lurk has no connection to Twitch, Kick, Rumble or the Morphe project.

### How to use these patches

Tap **Add to Morphe** on an Android phone with [Morphe Manager](https://morphe.software) installed; on
a computer it shows a QR code to scan with the phone. Releases from the `dev` branch are pre-releases:
turn on pre-releases for this source in Manager to get them.

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.0.0](https://github.com/jordanwoodson/morphe/releases/tag/v1.0.0)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;1 patches total
<details open>
<summary>📦 Rumble&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**🎯 Supported versions:**

| 3.21.0 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Unlock Premium](#unlock-premium) | Enables Rumble's local premium state after signing in and refreshing the profile, including ad-free playback and hiding premium upsells. Server-controlled content and subscription benefits still require server authorization. |  |

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
| `assets/icon.svg` | The Lurk icon. `patches-bundle.png` (Manager's icon for this source) is rendered from it: `mise exec aqua:linebender/resvg@0.47.0 -- resvg -w 512 -h 512 assets/icon.svg patches-bundle.png`. |
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

`bin/patch` passes anything after `--` to Morphe Desktop, for example
`bin/patch twitch -- --exclusive -e "Disable analytics"`.
`bin/refs` clones Morphe's own repos (patcher docs, patches library, official patches) for API reference.

### Writing a patch

The [Rumble 3.21.0 notes](docs/rumble.md) cover the premium patch, sign-in requirement
and its Android integration test.

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
