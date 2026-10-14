package io.github.jordanwoodson.patches.twitch.consent

import app.morphe.patcher.Fingerprint

internal object ConsentOptionsFingerprint : Fingerprint(
    name = "toString", returnType = "Ljava/lang/String;", parameters = emptyList(),
    strings = listOf("ConsentOptions(isDeniedUnderage=", ", shouldShowNotification=", ", shouldShowSettingsPage="),
)
