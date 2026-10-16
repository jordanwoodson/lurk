package io.github.jordanwoodson.patches.rumble.shared

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Format
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ThreeRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

/** Fails if an instruction from [from] up to (not including) [to] writes [register]. */
internal fun MutableMethod.requireUnwritten(register: Int, from: Int, to: Int, what: String) {
    implementation!!.instructions.subList(from, to).forEach { instruction ->
        if (instruction.writes(register)) throw PatchException("Rumble: $what is overwritten")
    }
}

/**
 * The register the instruction at [index] writes without reading it. Nothing else can be using it
 * just before that instruction, so code inserted there may use it.
 */
internal fun MutableMethod.overwrittenNext(index: Int): Int {
    val instruction = implementation!!.instructions[index]
    val written = (instruction as? OneRegisterInstruction)?.registerA
    if (written == null || !instruction.opcode.setsRegister() || instruction.reads(written)) {
        throw PatchException("Rumble: ${instruction.opcode} in $definingClass->$name doesn't free a register")
    }
    return written
}

/** The register number of parameter [index] (0 is `this` in an instance method). */
internal fun MutableMethod.parameterRegister(index: Int): Int {
    val sizes = (if (AccessFlags.STATIC.isSet(accessFlags)) emptyList() else listOf(1)) +
        parameterTypes.map { if (it.toString() == "J" || it.toString() == "D") 2 else 1 }
    return implementation!!.registerCount - sizes.sum() + sizes.take(index).sum()
}

private fun Instruction.writes(register: Int): Boolean {
    if (!opcode.setsRegister()) return false
    val written = (this as? OneRegisterInstruction)?.registerA ?: return false
    return written == register || (opcode.setsWideRegister() && written + 1 == register)
}

/** Whether the instruction reads [register], for the instructions [overwrittenNext] accepts. */
private fun Instruction.reads(register: Int): Boolean = when {
    // check-cast and the 2addr arithmetic read and write vA.
    opcode == Opcode.CHECK_CAST || opcode.name.endsWith("_2ADDR") -> true
    opcode.format == Format.Format35c || opcode.format == Format.Format3rc -> true
    this is ThreeRegisterInstruction -> registerB == register || registerC == register ||
        (opcode.setsWideRegister() && (registerB + 1 == register || registerC + 1 == register))
    this is TwoRegisterInstruction -> registerB == register ||
        (opcode.setsWideRegister() && registerB + 1 == register)
    else -> false
}

/**
 * Fails if a branch jumps to [index]. Code inserted at [index] runs before the instruction there, but a
 * branch to that instruction would skip it.
 */
internal fun MutableMethod.requireNoBranchTo(index: Int, what: String) {
    if (implementation!!.instructions[index].location.labels.isNotEmpty()) {
        throw PatchException("Rumble: $what is a branch target")
    }
}

/**
 * The type of the constructor parameter Kotlin checks against [name] ("saveQueryUseCase"), found through
 * the register its checkNotNullParameter call passes.
 */
internal fun MutableMethod.constructorParameterType(name: String): String {
    val code = implementation!!.instructions.toList()
    val nameIndex = code.indexOfFirst { it.constString() == name }
    if (nameIndex < 0) throw PatchException("Rumble: $definingClass doesn't check $name")
    val nameRegister = (code[nameIndex] as OneRegisterInstruction).registerA
    val check = code.drop(nameIndex + 1).firstOrNull {
        it is FiveRegisterInstruction && it.registerCount == 2 && it.registerD == nameRegister &&
            (it as ReferenceInstruction).reference.toString().endsWith("->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V")
    } as? FiveRegisterInstruction ?: throw PatchException("Rumble: no null check of $name in $definingClass")
    // The checked register, or the parameter it was copied from.
    var register = check.registerC
    val checkIndex = code.indexOf(check as com.android.tools.smali.dexlib2.iface.instruction.Instruction)
    val write = (checkIndex - 1 downTo 0).firstOrNull { code[it].writes(register) }
    if (write != null) {
        val copy = code[write]
        if (copy.opcode !in setOf(Opcode.MOVE_OBJECT, Opcode.MOVE_OBJECT_FROM16, Opcode.MOVE_OBJECT_16)) {
            throw PatchException("Rumble: $name in $definingClass isn't a parameter")
        }
        register = (copy as TwoRegisterInstruction).registerB
    }
    val index = (0..parameterTypes.size).firstOrNull { parameterRegister(it) == register }
        ?: throw PatchException("Rumble: $name in $definingClass isn't a parameter")
    // Index 0 is `this`.
    return parameterTypes[index - 1].toString()
}
