package io.github.jordanwoodson.patches.kick.chat

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import io.github.jordanwoodson.patches.kick.settings.kickSettings
import io.github.jordanwoodson.patches.kick.shared.Constants.COMPATIBILITY_KICK
import io.github.jordanwoodson.patches.kick.shared.HermesBundle.Constant
import io.github.jordanwoodson.patches.kick.shared.kickHermes
import io.github.jordanwoodson.patches.kick.shared.kickHermesPatch
import io.github.jordanwoodson.patches.shared.settings.Section
import io.github.jordanwoodson.patches.shared.settings.SwitchSetting

private const val SHOW_DELETED = "chat_show_deleted"

@Suppress("unused")
val showDeletedMessagesPatch = bytecodePatch(
    name = "Show deleted messages",
    description = "Keeps chat messages that moderators delete, or remove with a timeout or ban, struck through " +
        "with a (Deleted), (Timed out) or (Banned) label, the way Kick's chat shows them to moderators, along with " +
        "its ban, timeout and unban lines. Large KICKs gifts in chat also get the moderators' menu, whose Delete " +
        "is meant for the channel's moderators. Chat clears still clear. On by default, as on Twitch. Turn it " +
        "off in Lurk settings > Chat.",
) {
    compatibleWith(COMPATIBILITY_KICK)
    dependsOn(kickHermesPatch)

    execute {
        moderatorChatView()

        kickSettings.add(
            SwitchSetting(
                SHOW_DELETED,
                "Show deleted messages",
                "Keep deleted, timed-out and banned messages, struck through, as moderators see them.",
                Section.CHAT,
                restart = true,
            ),
        )
    }
}

/**
 * Kick's chat list is a web page (chat.mobile.kick.com) in a WebView, with its own realtime connection. The app
 * opens it with a JSON `config` URL parameter that includes the viewer's `userCanModerate`, and the page shows
 * removed messages struck through and labelled, plus ban, timeout and unban lines, when that is true. The config
 * builder loads the value from its environment just before storing it in the literal's userCanModerate slot;
 * that load becomes `true`.
 */
private fun moderatorChatView() {
    val config = kickHermes.functionUsing(
        "web chat config",
        reads = listOf("searchParams", "getRequestBaseHeaders"),
        loads = listOf("https://chat.mobile.kick.com"),
    )
    val code = kickHermes.instructions(config)
    fun fail(what: String): Nothing = throw PatchException("Kick: web chat config $what changed")
    val literal = code.filter { it.opcode == 0x01 || it.opcode == 0x02 } // NewObjectWithBuffer(Long)
        .singleOrNull { "userCanModerate" in kickHermes.objectKeys(it) } ?: fail("object")
    val slot = kickHermes.objectKeys(literal).indexOf("userCanModerate")
    // PutOwnBySlotIdx rObject, rValue, slot
    val store = code.singleOrNull { it.opcode == 0x52 && it.u8(1) == literal.u8(1) && it.u8(3) == slot }
        ?: fail("userCanModerate store")
    // LoadFromEnvironment rValue, rEnvironment, index
    val load = code.getOrNull(code.indexOf(store) - 1)?.takeIf { it.opcode == 0x3b && it.u8(1) == store.u8(2) }
        ?: fail("userCanModerate value")
    if (load.u8(1) < config.numberRegisters + config.nonPointerRegisters) fail("register")
    val value = load.bytes[1]
    kickHermes.replace(config, SHOW_DELETED, load.bytes + store.bytes,
        byteArrayOf(Constant.TRUE.opcode, value, Constant.TRUE.opcode, value) + store.bytes)
}
