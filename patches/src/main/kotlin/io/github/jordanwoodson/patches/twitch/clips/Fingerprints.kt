package io.github.jordanwoodson.patches.twitch.clips

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

internal object ProfileTabRoutesFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;", parameters = listOf("I"),
    filters = listOf(string("profile_clips"), string("profile_chat"), string("profile_about"), string("profile_home")),
)
