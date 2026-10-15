package io.github.jordanwoodson.patches.twitch.overlay

import app.morphe.patcher.Fingerprint

// A live chat line's render model: id, type, tags, badges, color, sender, tokens, ...
internal object LiveChatMessageFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("LiveChatMessage(messageId=", ", sender=", ", messageTokens="),
)
