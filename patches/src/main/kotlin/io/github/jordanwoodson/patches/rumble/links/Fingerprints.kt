package io.github.jordanwoodson.patches.rumble.links

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// Rumble's share use case, share(text, title): Boolean. Every share sheet in the app (videos, live streams,
// Shorts, reposts, playlists, channels) goes through it. EXTRA_TITLE appears nowhere else in the app.
internal object ShareTextFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
    strings = listOf("text/plain", "android.intent.extra.TEXT", "android.intent.extra.TITLE"),
)
