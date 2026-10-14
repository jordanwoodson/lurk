package io.github.jordanwoodson.patches.twitch.tipping

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import io.github.jordanwoodson.patches.twitch.shared.twitchMatch

internal const val INTERFACE_CLASS = "Lio/github/jordanwoodson/extension/shared/twitch/TwitchInterface;"

/** Identify only a UI-state flag, from its Kotlin diagnostic label, then filter its constructor write. */
internal fun BytecodePatchContext.filterBitsFlag(fingerprint: Fingerprint, marker: String) {
    val state = fingerprint.twitchMatch("chat UI state").method
    val instructions = state.implementation!!.instructions.toList()
    val label = instructions.indices.single { index ->
        ((instructions[index] as? ReferenceInstruction)?.reference as? StringReference)?.string == marker
    }
    val field = instructions.drop(label + 1).firstOrNull { it.opcode == Opcode.IGET_BOOLEAN }
        ?.let { (it as ReferenceInstruction).reference as FieldReference }
        ?: throw PatchException("Twitch: no boolean field after $marker")
    if (field.definingClass != state.definingClass) throw PatchException("Twitch: UI flag belongs to another class")
    val constructors = mutableClassDefBy(state.definingClass).methods.filter { it.name == "<init>" }
    var writes = 0
    constructors.forEach { method ->
        val points = method.implementation!!.instructions.withIndex().filter {
            it.value.opcode == Opcode.IPUT_BOOLEAN &&
                (it.value as ReferenceInstruction).reference.toString() == field.toString()
        }
        points.reversed().forEach { (index, instruction) ->
            val register = (instruction as TwoRegisterInstruction).registerA
            // Transform the value immediately before storage. No extra register or parameter shift,
            // and no object/constructor return is skipped.
            if (register > 255) throw PatchException("Twitch: UI flag register exceeds move-result range")
            method.addInstructions(index, """
                invoke-static/range { v$register .. v$register }, $INTERFACE_CLASS->bitsVisible(Z)Z
                move-result v$register
            """)
            writes++
        }
    }
    if (writes != 1) throw PatchException("Twitch: expected one constructor write for $marker, found $writes")
}
