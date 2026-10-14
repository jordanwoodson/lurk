package io.github.jordanwoodson.patches.rumble.settings

import app.morphe.patcher.Fingerprint
import io.github.jordanwoodson.patches.rumble.shared.composableFingerprint
import io.github.jordanwoodson.patches.shared.settings.SETTINGS_CLASS

// The application class is named in the manifest, so R8 keeps its name.
internal object ApplicationOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/rumble/battles/RumbleApplication;",
    name = "onCreate",
    returnType = "V",
    parameters = emptyList(),
)

// Settings' App Info section: Data Consent, Credits, Send Feedback and the app version.
internal val AppInfoSectionFingerprint =
    composableFingerprint("com.rumble.battles.settings.presentation.AppInfoSection")

// The extension method the settings patch fills with the registered settings.
internal object SettingsRegistryFingerprint : Fingerprint(
    definingClass = SETTINGS_CLASS,
    name = "registryJson",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
)
