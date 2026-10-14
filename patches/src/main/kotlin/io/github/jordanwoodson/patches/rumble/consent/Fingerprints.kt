package io.github.jordanwoodson.patches.rumble.consent

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// The Ketch Android SDK's two entry points that open its consent WebView, Ketch.load() and
// Ketch.showPreferences(), are both (Boolean, Boolean, Int, Int): Boolean. R8 renames the class,
// but each method logs its own line when an experience is already showing.

internal object KetchLoadFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Z", "Z", "I", "I"),
    strings = listOf("Not loading as an experience is already being shown"),
)

internal object KetchShowPreferencesFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Z", "Z", "I", "I"),
    strings = listOf("Not showing preferences as an experience is already being shown"),
)
