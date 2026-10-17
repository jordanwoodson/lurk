package io.github.jordanwoodson.patches.shared

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

/**
 * Gives an empty static stub of [extensionClass] a body that calls into obfuscated app code, so the
 * extension never names R8 types. The stub's Java body must be empty (`return;`) or return null, 0 or
 * false. [smali] replaces it and may use [locals] local registers (v0 up) before the parameters.
 */
internal fun BytecodePatchContext.bridge(extensionClass: String, name: String, smali: String, locals: Int = 0) {
    val classDef = mutableClassDefBy(extensionClass)
    val stub = classDef.methods.singleOrNull { it.name == name }
        ?: throw PatchException("Lurk: no $name stub in $extensionClass")
    if (!AccessFlags.STATIC.isSet(stub.accessFlags)) throw PatchException("Lurk: $name stub is not static")
    val code = stub.implementation!!.instructions.toList()
    val empty = (code.size == 1 && code[0].opcode == Opcode.RETURN_VOID) ||
        (code.size == 2 && code[0].opcode == Opcode.CONST_4 && (code[0] as NarrowLiteralInstruction).narrowLiteral == 0 &&
            code[1].opcode in setOf(Opcode.RETURN, Opcode.RETURN_OBJECT))
    if (!empty) throw PatchException("Lurk: $name stub is not empty")

    val parameterRegisters = stub.parameterTypes.map { if (it.toString() == "J" || it.toString() == "D") 2 else 1 }.sum()
    classDef.methods.remove(stub)
    classDef.methods.add(
        ImmutableMethod(
            stub.definingClass,
            stub.name,
            stub.parameters,
            stub.returnType,
            stub.accessFlags,
            stub.annotations,
            stub.hiddenApiRestrictions,
            MutableMethodImplementation(locals + parameterRegisters),
        ).toMutable().apply { addInstructions(0, smali) },
    )
}
