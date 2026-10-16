package io.github.jordanwoodson.patches.rumble.counts

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// Rumble's one count formatter (count, keepDecimal): the number below 1000, else 12K, 1.2M or 3B. Views,
// likes, comments, followers and "watching now" all go through it.
internal object CountFormatterFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    parameters = listOf("J", "Z"),
    strings = listOf("K", "M", "B"),
)
