package io.github.jordanwoodson.patches.twitch.overlay

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import io.github.jordanwoodson.patches.shared.settings.ChoiceSetting
import io.github.jordanwoodson.patches.shared.settings.Section
import io.github.jordanwoodson.patches.shared.settings.SwitchSetting
import io.github.jordanwoodson.patches.twitch.emotes.freeRegister
import io.github.jordanwoodson.patches.twitch.emotes.requireNotBranchTarget
import io.github.jordanwoodson.patches.twitch.settings.twitchSettings
import io.github.jordanwoodson.patches.twitch.settings.twitchSettingsPatch
import io.github.jordanwoodson.patches.twitch.shared.Constants.COMPATIBILITY_TWITCH
import io.github.jordanwoodson.patches.twitch.shared.twitchMatch

private const val OVERLAY = "Lio/github/jordanwoodson/extension/shared/twitch/TwitchChatOverlay;"
private const val CONTEXT_WRAPPER = "Landroid/content/ContextWrapper;"

@Suppress("unused")
val chatOverlayPatch = bytecodePatch(
    name = "Chat overlay",
    description = "Adds a switch that scrolls live chat messages right to left across the top of the video in " +
        "landscape (danmaku), as plain white text with the sender's name. Off until turned on in Lurk settings, " +
        "with choices for speed and number of lines. Native live channels only: not React Native chat, VODs or " +
        "chat replay. The overlay never takes touches.",
) {
    compatibleWith(COMPATIBILITY_TWITCH)
    dependsOn(twitchSettingsPatch)
    execute {
        val message = LiveChatMessageFingerprint.twitchMatch("live chat message")
        val type = message.classDef.type
        val parent = message.classDef.superclass ?: throw PatchException("Twitch: live chat message has no parent")
        val sender = message.method.labelledField(", sender=")
        val tokens = message.method.labelledField(", messageTokens=")
        if (tokens.type != "Ljava/util/List;") throw PatchException("Twitch: live chat message tokens aren't a list")

        // The chat list's feeder: takes any chat list event and appends live messages to the list.
        val feeder = Fingerprint(
            returnType = "V",
            parameters = listOf(parent),
            custom = { method, classDef ->
                classDef.fields.any { it.type == CONTEXT_WRAPPER } &&
                    method.implementation?.instructions?.any {
                        it.opcode == Opcode.INSTANCE_OF && ((it as ReferenceInstruction).reference as TypeReference).type == type
                    } == true
            },
        ).twitchMatch("chat list feeder")
        val context = feeder.classDef.fields.singleOrNull { it.type == CONTEXT_WRAPPER }
            ?: throw PatchException("Twitch: chat list feeder has no single context")

        feeder.method.apply {
            val instructions = implementation!!.instructions
            val check = instructions.indexOfFirst {
                it.opcode == Opcode.INSTANCE_OF && ((it as ReferenceInstruction).reference as TypeReference).type == type
            }
            val cast = (check + 1 until instructions.size).firstOrNull {
                instructions[it].opcode == Opcode.CHECK_CAST &&
                    ((instructions[it] as ReferenceInstruction).reference as TypeReference).type == type
            } ?: throw PatchException("Twitch: chat list feeder doesn't cast live messages")
            if (cast - check > 4) throw PatchException("Twitch: live message branch changed")
            val live = (instructions[cast] as OneRegisterInstruction).registerA
            if (live > 15) throw PatchException("Twitch: live message register out of range")
            // Right after the cast: the message is about to be added to the list.
            val insert = cast + 1
            requireNotBranchTarget(insert, "live message branch")
            val a = freeRegister(insert, listOf(live), "chat overlay")
            val b = freeRegister(insert, listOf(live, a), "chat overlay")
            val c = freeRegister(insert, listOf(live, a, b), "chat overlay")
            addInstructions(insert, """
                move-object/from16 v$a, p0
                iget-object v$a, v$a, ${context.definingClass}->${context.name}:${context.type}
                iget-object v$b, v$live, $sender
                iget-object v$c, v$live, $tokens
                invoke-static { v$a, v$b, v$c }, $OVERLAY->onMessage(Landroid/content/Context;Ljava/lang/Object;Ljava/util/List;)V
            """)
        }

        twitchSettings.add(
            SwitchSetting("chat_overlay", "Chat overlay",
                "In landscape, live chat messages scroll across the top of the video. Native live channels only; " +
                    "touches still reach the player.",
                Section.CHAT, default = false),
            ChoiceSetting("chat_overlay_speed", "Chat overlay speed", "How fast messages cross the video.",
                Section.CHAT, "1", listOf("Slow" to "0", "Normal" to "1", "Fast" to "2")),
            ChoiceSetting("chat_overlay_lines", "Chat overlay lines", "How many lines of messages scroll at once.",
                Section.CHAT, "5", listOf("3" to "3", "5" to "5", "8" to "8")),
        )
    }
}

/** The field a toString prints right after [label]: the first object field read after the label's constant. */
private fun MutableMethod.labelledField(label: String): FieldReference {
    val instructions = implementation!!.instructions
    val at = instructions.indexOfFirst { ((it as? ReferenceInstruction)?.reference as? StringReference)?.string == label }
    if (at < 0) throw PatchException("Twitch: no \"$label\" in $definingClass")
    val read = instructions.drop(at + 1).firstOrNull { it.opcode == Opcode.IGET_OBJECT }
        ?: throw PatchException("Twitch: nothing printed after \"$label\"")
    val field = (read as ReferenceInstruction).reference as FieldReference
    if (field.definingClass != definingClass || (read as TwoRegisterInstruction).registerB != parameterlessThis()) {
        throw PatchException("Twitch: \"$label\" isn't followed by its own field")
    }
    return field
}

/** The register of this in an instance method without parameters (toString). */
private fun MutableMethod.parameterlessThis() = implementation!!.registerCount - 1
