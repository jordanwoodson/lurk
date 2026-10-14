package io.github.jordanwoodson.patches.kick.links

import app.morphe.patcher.Fingerprint

// Kick's stream/channel share hooks call react-native-share.open({message: url}).
internal object ShareIntentFingerprint : Fingerprint(
    definingClass = "Lcl/json/social/ShareIntent;", name = "open", returnType = "V",
    parameters = listOf("Lcom/facebook/react/bridge/ReadableMap;"),
    strings = listOf("android.intent.extra.TEXT", "isNewTask", "whatsAppNumber"),
)

// Cover React Native's built-in share sheet too, with the same narrow EXTRA_TEXT wrapper.
internal object ReactShareFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/react/modules/share/ShareModule;", name = "share", returnType = "V",
    parameters = listOf("Lcom/facebook/react/bridge/ReadableMap;", "Ljava/lang/String;", "Lcom/facebook/react/bridge/Promise;"),
    strings = listOf("text/plain", "android.intent.extra.TEXT", "Failed to open share dialog"),
)
