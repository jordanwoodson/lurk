# Lurk patches workspace

Lurk, Jordan's Morphe privacy patch bundle, and the tooling around it. Main focus: privacy-focused
clients for Twitch (`tv.twitch.android.app`), Kick (`com.kick.mobile`) and Rumble (`com.rumble.battles`).
Use "Morphe" only descriptively ("patches for Morphe"), never as part of Lurk's name (see `NOTICE`).
The GitHub repo is `jordanwoodson/lurk-morphe-patches` for discovery: Lurk is the name, "morphe-patches" says what it is.
README.md is the public page. docs/development.md documents the layout, commands and workflow; read it
first. CONTRIBUTING.md has the rules contributors see; keep the two in line with this file.

## Rules

- **APK sources: APKMirror, Google Play or Uptodown only.** Never APKPure or any other mirror, not even
  for a quick look. Store APKs with `bin/apk-add` (checks the pinned signing cert) or `bin/apk-fetch`
  (Google Play).
- **Jordan writes his own patches.** Other people's Twitch/Kick/Rumble patch repos exist; don't copy or
  port them. Morphe's official repos (`bin/refs`) are fine as API reference.
- Generated output (decompiled code, patched APKs, emulator, screenshots) goes to `~/scratch/morphe/`,
  never into the repo. `apks/` and `tools/` are gitignored.
- Use the `dev` branch and semantic commits (`feat:`, `fix:`, `chore:`). A pushed `feat:`/`fix:` on `dev`
  publishes a pre-release through `release.yml`. Don't hand-edit `patches-list.json`,
  `patches-bundle.json`, `CHANGELOG.md` or the README patch list.

## Conventions

- Patches live in `patches/src/main/kotlin/io/github/jordanwoodson/patches/<app>/<feature>/`, with the
  patch in `<Name>Patch.kt` and its fingerprints in `Fingerprints.kt`. Each app's `shared/Constants.kt`
  holds `COMPATIBILITY_<APP>`; every patch calls `compatibleWith(...)` with it.
- When patches are tested on a new app version, update that version in the app's `targets`.
- Fingerprint on strings, method signatures and instruction patterns, not obfuscated class names.
- Java that runs inside the app goes in `extensions/` (`extensions/shared` has `Logger`, tag `lurk`).
  `patches/.../shared/Extensions.kt` has the extension paths and the Logger descriptor.
- Kick is React Native: logic is Hermes bytecode in `assets/index.android.bundle`. Use `hermesPatch`
  from morphe-patches-library for it; `bin/decompile kick` writes a disassembly and decompiled JS.

## Commands

`bin/bootstrap` (setup), `bin/build`, `bin/apk-add <file>`, `bin/apk-fetch <app>`, `bin/apk-info`,
`bin/scan <app>`, `bin/decompile <app>`, `bin/patch <app> [--install=emulator-5580]`,
`bin/emu start|stop|status|shot`, `bin/new-app`, `bin/refs`, `bin/app-icon <app> <artwork.png>`.
