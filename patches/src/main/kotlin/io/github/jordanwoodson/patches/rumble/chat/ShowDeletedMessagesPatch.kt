package io.github.jordanwoodson.patches.rumble.chat

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import io.github.jordanwoodson.patches.rumble.settings.rumbleSettings
import io.github.jordanwoodson.patches.rumble.settings.settingsPatch
import io.github.jordanwoodson.patches.rumble.shared.Constants.COMPATIBILITY_RUMBLE
import io.github.jordanwoodson.patches.rumble.shared.RUMBLE_CHAT
import io.github.jordanwoodson.patches.rumble.shared.overwrittenNext
import io.github.jordanwoodson.patches.rumble.shared.requireNoBranchTo
import io.github.jordanwoodson.patches.rumble.shared.requireUnwritten
import io.github.jordanwoodson.patches.rumble.shared.single
import io.github.jordanwoodson.patches.shared.settings.Section
import io.github.jordanwoodson.patches.shared.settings.SwitchSetting

private const val KEY = "show_deleted_messages"

@Suppress("unused")
val showDeletedMessagesPatch = bytecodePatch(
    name = "Show deleted messages",
    description = "Shows the text of live chat messages that moderators remove, or that Rumble hides because " +
        "the sender was muted, after Rumble's \"Message was removed by moderator\" label. On by default. Turn " +
        "it off in Lurk settings > Chat.",
) {
    compatibleWith(COMPATIBILITY_RUMBLE)
    dependsOn(settingsPatch, chatEntityPatch)

    execute {
        // Rumble keeps the text in the entity and marks it deleted; the rows draw the label instead.
        val entity = ChatMessageEntityFingerprint.single("live chat message entity").classDef

        val label = resourceId(ResourceType.STRING, "live_chat_deleted_message")
        val sites = DeletedMessageLabelFingerprint.matchAll()
        if (sites.size != 2) {
            throw PatchException("Rumble: expected the deleted message and Rant rows to draw the deleted label, " +
                "found ${sites.size}")
        }
        sites.forEach { match ->
            val method = match.method
            val code = method.implementation!!.instructions.toList()
            val literal = code.indexOfFirst { (it as? WideLiteralInstruction)?.wideLiteral == label }
            // stringResource(id, composer, changed)
            val call = (literal + 1 until code.size).first { index ->
                val reference = (code[index] as? ReferenceInstruction)?.reference as? MethodReference
                code[index].opcode == Opcode.INVOKE_STATIC && reference != null &&
                    reference.returnType == "Ljava/lang/String;" && reference.parameterTypes.size == 3 &&
                    reference.parameterTypes[0] == "I" && reference.parameterTypes[2] == "I"
            }
            if (code[call + 1].opcode != Opcode.MOVE_RESULT_OBJECT) throw PatchException("Rumble: unexpected label code")
            val text = (code[call + 1] as OneRegisterInstruction).registerA
            val insert = call + 2
            method.requireNoBranchTo(insert, "the code after the deleted label")

            val entityField = match.classDef.fields.firstOrNull {
                it.type == entity.type && !AccessFlags.STATIC.isSet(it.accessFlags)
            }
            val load: String
            val entityRegister: Int
            if (entityField != null && !AccessFlags.STATIC.isSet(method.accessFlags)) {
                // A row lambda: the entity is a captured field.
                entityRegister = method.overwrittenNext(insert)
                load = """
                    move-object/from16 v$entityRegister, p0
                    iget-object v$entityRegister, v$entityRegister, ${match.classDef.type}->${entityField.name}:${entity.type}
                """
            } else {
                // The Rant row: the entity is in the register of the nearest call on it.
                val previous = (call downTo 0).first { index ->
                    val reference = (code[index] as? ReferenceInstruction)?.reference as? MethodReference
                    code[index].opcode == Opcode.INVOKE_VIRTUAL && reference?.definingClass == entity.type
                }
                entityRegister = (code[previous] as FiveRegisterInstruction).registerC
                method.requireUnwritten(entityRegister, previous + 1, insert, "the message entity")
                load = ""
            }
            if (text > 15 || entityRegister > 15) throw PatchException("Rumble: unexpected registers for the deleted label")
            method.addInstructions(
                insert,
                load + """
                    invoke-static { v$text, v$entityRegister }, $RUMBLE_CHAT->deletedLabel(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;
                    move-result-object v$text
                """,
            )
        }

        rumbleSettings.add(
            SwitchSetting(
                KEY,
                "Show deleted messages",
                "The text of messages moderators removed, after Rumble's \"removed by moderator\" label.",
                Section.CHAT,
                default = true,
            ),
        )
    }
}
