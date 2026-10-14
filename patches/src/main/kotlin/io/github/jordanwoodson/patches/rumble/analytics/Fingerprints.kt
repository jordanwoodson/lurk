package io.github.jordanwoodson.patches.rumble.analytics

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// Rumble's event repository: every analytics event the app sends to e.rumble.com, and every copy
// it posts to its debug log (debug/log2), goes through this class. Its constructor checks its
// parameters by name.
internal object EventRepositoryFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    strings = listOf("eventRemoteDataSource", "appRequestName", "watchProgressDao"),
)

// The watch progress event (a Kotlin data class), found by its generated toString().
internal object WatchProgressEventToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("WatchProgressEvent(timeRange="),
)
