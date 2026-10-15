package io.github.jordanwoodson.patches.twitch.mature

import app.morphe.patcher.Fingerprint

/**
 * Whether the viewer already acknowledged a content label: Twitch's debug override
 * "override_mature_checks" first, then the "<user>_<label>" entry that Start Watching saves. Every
 * native label gate asks it (live and VOD gating, the content-gate overlay and the clip player).
 */
internal object MatureLabelAcknowledgedFingerprint : Fingerprint(
    returnType = "Z", parameters = listOf("Ljava/lang/String;"),
    strings = listOf("override_mature_checks"),
)
