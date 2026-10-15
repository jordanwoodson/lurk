package io.github.jordanwoodson.patches.twitch.chat

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

private const val EVENT_DISPATCHER = "Ltv/twitch/android/core/mvp/viewdelegate/EventDispatcher;"

// Chat timestamps.

// The chat line builder's class: its constructor makes the "H:mm" time formatter (the only one in the app).
internal object ChatLineFormatterFingerprint : Fingerprint(
    name = "<init>", returnType = "V", filters = listOf(string("H:mm")),
)

// Turns a chat message into a list item. Its fourth parameter shows the time: true for chat history only.
internal object ChatLineItemFingerprint : Fingerprint(
    returnType = "L", parameters = listOf("L", "Z", "L", "Z", "L", EVENT_DISPATCHER, EVENT_DISPATCHER),
    strings = listOf("client-nonce", "gifs"),
)
