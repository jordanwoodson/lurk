package io.github.jordanwoodson.patches.twitch.ads

import app.morphe.patcher.Fingerprint

internal object ClientAdEligibilityFingerprint : Fingerprint(
    returnType = "V", parameters = listOf("Lio/reactivex/internal/operators/single/c;"),
    strings = listOf("bypass_ad_eligibility", "skip_preroll", "decline_all_ads"),
)
