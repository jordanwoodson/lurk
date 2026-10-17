package io.github.jordanwoodson.patches.rumble.playback

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import io.github.jordanwoodson.patches.rumble.settings.rumbleSettings
import io.github.jordanwoodson.patches.rumble.settings.settingsPatch
import io.github.jordanwoodson.patches.rumble.shared.Constants.COMPATIBILITY_RUMBLE
import io.github.jordanwoodson.patches.rumble.shared.RUMBLE_PLAYBACK
import io.github.jordanwoodson.patches.rumble.shared.single
import io.github.jordanwoodson.patches.shared.settings.Section
import io.github.jordanwoodson.patches.shared.settings.SwitchSetting

private const val KEY = "resume_signed_out"

@Suppress("unused")
val resumeSignedOutPatch = bytecodePatch(
    name = "Resume videos when signed out",
    description = "Starts videos where you left off when you aren't signed in, from the positions Rumble " +
        "already keeps on the phone. Signed in, Rumble's own watch progress is unchanged. On by default. Turn " +
        "it off in Lurk settings > Playback.",
) {
    compatibleWith(COMPATIBILITY_RUMBLE)
    dependsOn(settingsPatch)

    execute {
        // After reading the position kept on the phone, Rumble asks the session whether it is signed in:
        // signed in it starts at the server's position (or the local one when the server has none), signed out
        // at 0. The signed-out branch is a move of 0 over the register that holds the local position on both
        // paths into it (read back from the coroutine after a suspension, or just read).
        WatchPositionFingerprint.single("watch position use case").method.apply {
            val code = implementation!!.instructions.toList()
            val unbox = code.indexOfFirst {
                it is ReferenceInstruction && it.reference.toString() == "Ljava/lang/Boolean;->booleanValue()Z"
            }
            val branch = code.getOrNull(unbox + 2)
            if (unbox < 0 || code[unbox + 1].opcode != Opcode.MOVE_RESULT || branch?.opcode != Opcode.IF_EQZ) {
                throw PatchException("Rumble: unexpected signed-in check in the watch position use case")
            }
            val signedOut = (branch as BuilderOffsetInstruction).target.location.index
            val zero = code[signedOut]
            if (zero.opcode != Opcode.MOVE_WIDE) throw PatchException("Rumble: unexpected signed-out position")
            val position = (zero as TwoRegisterInstruction).registerA
            val zeroRegister = zero.registerB
            val zeroIsConstant = code.any {
                it.opcode == Opcode.CONST_WIDE_16 && (it as OneRegisterInstruction).registerA == zeroRegister &&
                    (it as WideLiteralInstruction).wideLiteral == 0L
            }
            val restored = code.any {
                it.opcode == Opcode.IGET_WIDE && (it as TwoRegisterInstruction).registerA == position
            }
            if (!zeroIsConstant || !restored) throw PatchException("Rumble: unexpected watch position registers")

            replaceInstruction(
                signedOut,
                "invoke-static/range { v$position .. v${position + 1} }, $RUMBLE_PLAYBACK->signedOutPosition(J)J",
            )
            addInstruction(signedOut + 1, "move-result-wide v$position")
        }

        rumbleSettings.add(
            SwitchSetting(
                KEY,
                "Resume videos when signed out",
                "Start videos where you left off, from the positions Rumble keeps on the phone.",
                Section.PLAYBACK,
                default = true,
            ),
        )
    }
}
