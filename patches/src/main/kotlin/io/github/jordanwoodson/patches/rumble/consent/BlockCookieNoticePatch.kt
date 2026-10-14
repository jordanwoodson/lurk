package io.github.jordanwoodson.patches.rumble.consent

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import io.github.jordanwoodson.patches.rumble.shared.Constants.COMPATIBILITY_RUMBLE

@Suppress("unused")
val blockCookieNoticePatch = bytecodePatch(
    name = "Block cookie notice",
    description = "Stops Rumble's Ketch consent SDK from loading, so the cookie notice never appears and no " +
        "consent is recorded. Rumble then keeps the analytics and ad tracking it gates on consent switched off. " +
        "Settings > Data Consent no longer opens.",
) {
    compatibleWith(COMPATIBILITY_RUMBLE)

    execute {
        // Rumble calls load() on every resume. Where no privacy law applies, Ketch records consent to
        // every purpose without showing anything, which starts AppsFlyer and Firebase Analytics.
        // showPreferences() (Settings > Data Consent) records the same defaults as soon as it opens.
        // Returning false is what both do when they cannot create their WebView: the SDK never contacts
        // Ketch, the advertising ID Rumble hands it is never sent, and no consent is stored.
        listOf(KetchLoadFingerprint, KetchShowPreferencesFingerprint).forEach { fingerprint ->
            val match = fingerprint.matchAllOrNull()?.singleOrNull()
                ?: throw PatchException("Rumble: expected exactly one match for " +
                    "${fingerprint.javaClass.simpleName}. This patch targets 3.21.0; " +
                    "recheck the Ketch SDK before updating compatibility.")

            match.method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """,
            )
        }
    }
}
