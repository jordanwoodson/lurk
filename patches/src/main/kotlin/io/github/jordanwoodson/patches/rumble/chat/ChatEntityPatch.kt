package io.github.jordanwoodson.patches.rumble.chat

import app.morphe.patcher.patch.bytecodePatch
import io.github.jordanwoodson.patches.rumble.settings.settingsPatch
import io.github.jordanwoodson.patches.rumble.shared.Constants.COMPATIBILITY_RUMBLE
import io.github.jordanwoodson.patches.rumble.shared.RUMBLE_CHAT
import io.github.jordanwoodson.patches.shared.bridge
import io.github.jordanwoodson.patches.rumble.shared.single
import io.github.jordanwoodson.patches.rumble.shared.toStringGetter

/**
 * Gives the extension the fields of a live chat message entity it needs (text, sender, time), through
 * stubs filled with the entity's getters. Shared by the chat patches, so each stub is filled once.
 */
internal val chatEntityPatch = bytecodePatch(
    description = "Lets the chat patches read live chat messages.",
) {
    compatibleWith(COMPATIBILITY_RUMBLE)
    dependsOn(settingsPatch)

    execute {
        val entity = ChatMessageEntityFingerprint.single("live chat message entity").classDef
        mapOf(
            "messageText" to ", message=",
            "userName" to ", userName=",
            "timeReceived" to ", timeReceived=",
        ).forEach { (stub, label) ->
            bridge(
                RUMBLE_CHAT,
                stub,
                """
                    check-cast p0, ${entity.type}
                    invoke-virtual { p0 }, ${entity.toStringGetter(label)}
                    move-result-object p0
                    return-object p0
                """,
            )
        }
    }
}
