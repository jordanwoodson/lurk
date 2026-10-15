package io.github.jordanwoodson.patches.twitch.ads

import app.morphe.patcher.Fingerprint

// RxJava's Single emitter is the only parameter; R8 renames its class, so match any object type.
internal object ClientAdEligibilityFingerprint : Fingerprint(
    returnType = "V", parameters = listOf("L"),
    strings = listOf("bypass_ad_eligibility", "skip_preroll", "decline_all_ads"),
)
