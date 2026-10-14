package io.github.jordanwoodson.patches.twitch.tipping

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

internal object OneChatStateFingerprint : Fingerprint(
    name = "toString", returnType = "Ljava/lang/String;", parameters = emptyList(),
    filters = listOf(string("State(isVisible="), string(", isBitsButtonVisible="), string(", isOneTapEnabled=")),
)

internal object ChatToolbarStateFingerprint : Fingerprint(
    name = "toString", returnType = "Ljava/lang/String;", parameters = emptyList(),
    filters = listOf(string("State(channelInfo="), string(", isBitsButtonVisible="), string(", isBitsSpendingTrayVisibleBasedOnInput=")),
)
