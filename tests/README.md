# Tests

Checks for the patched output, per app. None run in CI: they need a patched APK, decoded
output or a Hermes bundle, and for the probes an Android emulator.

- `verify_*.py` and `test_*.py` inspect apktool output and Hermes bundles, or compile an
  extension helper against small stubs and run it on the JVM. Each script's docstring says
  what it takes.
- `*Probe.java` and `*Probe.kt` run real methods from the patched APK on ART. The
  `bin/test-<app>*` scripts compile them, push them to the emulator and run them.

See [docs/development.md](../docs/development.md#testing) for the workflow and the
per-app pages under `docs/` for what each check pins and why.
