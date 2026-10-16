package io.github.jordanwoodson.patches.rumble.chat

import app.morphe.patcher.patch.bytecodePatch
import io.github.jordanwoodson.patches.rumble.settings.rumbleSettings
import io.github.jordanwoodson.patches.rumble.settings.settingsPatch
import io.github.jordanwoodson.patches.rumble.shared.Constants.COMPATIBILITY_RUMBLE
import io.github.jordanwoodson.patches.rumble.shared.single
import io.github.jordanwoodson.patches.shared.settings.Section
import io.github.jordanwoodson.patches.shared.settings.SwitchSetting
import io.github.jordanwoodson.patches.shared.settings.addInstructionsIfEnabled

private const val PINNED = "chat_hide_pinned"
private const val RAIDS = "chat_hide_raid_notices"
private const val GIFTS = "chat_hide_gift_notices"
private const val SPONSOR_READS = "chat_hide_sponsor_reads"
private const val BADGES = "chat_hide_badges"
private const val ALL = "chat_hide_all"

@Suppress("unused")
val hideChatClutterPatch = bytecodePatch(
    name = "Hide chat clutter",
    description = "Hides pinned messages, raid and gift notices and sponsor reads in live chat, each with its " +
        "own switch, and optionally chat badges or the whole chat. Starts off. Turn them on in Lurk " +
        "settings > Chat.",
) {
    compatibleWith(COMPATIBILITY_RUMBLE)
    dependsOn(settingsPatch, liveChatHookPatch)

    execute {
        PinnedMessageSectionFingerprint.single("pinned message section").method
            .addInstructionsIfEnabled(PINNED, "return-void")
        HostReadsMessageFingerprint.single("sponsor read message").method
            .addInstructionsIfEnabled(SPONSOR_READS, "return-void")
        // Raid and gift notices are dropped by the live chat hook.

        ChatBadgesFingerprint.single("chat user badges getter").method.addInstructionsIfEnabled(
            BADGES,
            """
                invoke-static { }, Ljava/util/Collections;->emptyList()Ljava/util/List;
                move-result-object v0
                return-object v0
            """,
        )
        LiveChatDisabledFingerprint.single("video live chat disabled getter").method.addInstructionsIfEnabled(
            ALL,
            """
                sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                return-object v0
            """,
        )

        rumbleSettings.add(
            SwitchSetting(PINNED, "Hide pinned messages", "The message a channel pins above live chat.",
                Section.CHAT, default = false),
            SwitchSetting(RAIDS, "Hide raid notices", "Chat lines announcing a raid. The raid countdown and " +
                "Rumble's own Auto-join Raids setting stay.", Section.CHAT, default = false),
            SwitchSetting(GIFTS, "Hide gift notices", "Chat lines announcing gifted subscriptions.",
                Section.CHAT, default = false),
            SwitchSetting(SPONSOR_READS, "Hide sponsor reads", "A creator's sponsor messages in live chat.",
                Section.CHAT, default = false),
            SwitchSetting(BADGES, "Hide chat badges", "Verified, subscriber and supporter badges next to names, " +
                "and the tint subscribers' messages get. Applies when you open a stream again.", Section.CHAT, default = false),
            SwitchSetting(ALL, "Hide live chat", "The whole live chat of every stream. Applies when you open a " +
                "stream again.", Section.CHAT, default = false),
        )
    }
}
