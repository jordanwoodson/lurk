package io.github.jordanwoodson.patches.twitch.clips

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

// The native Home's top tab lists (signed in: Following, Live, Clips; signed out: Live, Clips).
internal object HomeTabsFingerprint : Fingerprint(
    returnType = "Ljava/util/List;", parameters = listOf("Z"),
    custom = { method, _ ->
        val references = method.implementation?.instructions?.mapNotNull {
            (it as? ReferenceInstruction)?.reference?.toString()
        }.orEmpty()
        "Ltv/twitch/android/models/feed/DiscoveryFeedPage\$FeedLocationPage;" in references &&
            references.any { it.startsWith("Ltv/twitch/android/models/feed/FeedLocation\$Clips;->INSTANCE") }
    },
)

internal object ProfileTabRoutesFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;", parameters = listOf("I"),
    filters = listOf(string("profile_clips"), string("profile_chat"), string("profile_about"), string("profile_home")),
)
