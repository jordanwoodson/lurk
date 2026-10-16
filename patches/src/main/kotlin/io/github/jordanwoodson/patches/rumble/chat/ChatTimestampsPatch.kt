package io.github.jordanwoodson.patches.rumble.chat

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import io.github.jordanwoodson.patches.rumble.settings.rumbleSettings
import io.github.jordanwoodson.patches.rumble.settings.settingsPatch
import io.github.jordanwoodson.patches.rumble.shared.Constants.COMPATIBILITY_RUMBLE
import io.github.jordanwoodson.patches.rumble.shared.RUMBLE_CHAT
import io.github.jordanwoodson.patches.rumble.shared.calls
import io.github.jordanwoodson.patches.rumble.shared.requireNoBranchTo
import io.github.jordanwoodson.patches.rumble.shared.requireUnwritten
import io.github.jordanwoodson.patches.rumble.shared.single
import io.github.jordanwoodson.patches.rumble.shared.toStringGetter
import io.github.jordanwoodson.patches.shared.settings.Section
import io.github.jordanwoodson.patches.shared.settings.SwitchSetting

private const val KEY = "chat_timestamps"
private val MOVES = setOf(Opcode.MOVE_OBJECT, Opcode.MOVE_OBJECT_FROM16, Opcode.MOVE_OBJECT_16)

@Suppress("unused")
val chatTimestampsPatch = bytecodePatch(
    name = "Chat timestamps",
    description = "Shows the time each live chat message was sent in front of the sender's name. On by " +
        "default. Turn it off in Lurk settings > Chat.",
) {
    compatibleWith(COMPATIBILITY_RUMBLE)
    dependsOn(settingsPatch, chatEntityPatch)

    execute {
        val entity = ChatMessageEntityFingerprint.single("live chat message entity").classDef
        val userName = entity.toStringGetter(", userName=")

        // LiveChatContentView(modifier, ..., message, userName, ...) draws a message row. The regular and the
        // deleted message rows call it from lambdas that capture the message entity; the pinned message and
        // Premium purchase views take the entity as a parameter and keep Rumble's name.
        val contentView = LiveChatContentViewFingerprint.single("live chat message content").method
        val contentCall = "${contentView.definingClass}->${contentView.name}(" +
            contentView.parameterTypes.joinToString("") + ")V"
        val rows = mutableListOf<Pair<ClassDef, Method>>()
        classDefForEach { classDef ->
            if (classDef.fields.none { it.type == entity.type && !AccessFlags.STATIC.isSet(it.accessFlags) }) {
                return@classDefForEach
            }
            classDef.methods.filter { it.calls(contentCall) }.forEach { rows += classDef to it }
        }
        if (rows.size != 2) throw PatchException("Rumble: expected two chat rows with a message entity, found ${rows.size}")

        rows.forEach { (classDef, row) ->
            val method = mutableClassDefBy(classDef).methods.single {
                it.name == row.name && it.parameterTypes.map(CharSequence::toString) ==
                    row.parameterTypes.map(CharSequence::toString)
            }
            val code = method.implementation!!.instructions.toList()
            val call = code.indices.single {
                code[it].opcode == Opcode.INVOKE_STATIC_RANGE &&
                    (code[it] as ReferenceInstruction).reference.toString() == contentCall
            }
            // The fourth argument (userName), followed back through copies to the getter's result.
            var name = (code[call] as RegisterRangeInstruction).startRegister + 3
            var write = call
            while (true) {
                val register = name
                write = (write - 1 downTo 0).first { index ->
                    code[index].opcode.setsRegister() && (code[index] as? OneRegisterInstruction)?.registerA == register
                }
                if (code[write].opcode in MOVES) name = (code[write] as TwoRegisterInstruction).registerB else break
            }
            val getter = code[write - 1]
            if (code[write].opcode != Opcode.MOVE_RESULT_OBJECT || getter.opcode != Opcode.INVOKE_VIRTUAL ||
                (getter as ReferenceInstruction).reference.toString() != userName
            ) {
                throw PatchException("Rumble: the sender's name in ${classDef.type} isn't the entity's userName")
            }
            val entityRegister = (getter as FiveRegisterInstruction).registerC
            method.requireNoBranchTo(write + 1, "the code after the sender's name")
            if (name > 15 || entityRegister > 15) throw PatchException("Rumble: unexpected registers for the sender's name")
            method.addInstructions(
                write + 1,
                """
                    invoke-static { v$name, v$entityRegister }, $RUMBLE_CHAT->nameWithTime(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;
                    move-result-object v$name
                """,
            )
        }

        rumbleSettings.add(
            SwitchSetting(
                KEY,
                "Chat timestamps",
                "The time each message was sent, before the sender's name.",
                Section.CHAT,
                default = true,
            ),
        )
    }
}
