package io.github.jordanwoodson.patches.rumble.shared

import app.morphe.patcher.patch.PatchException
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private val FIELD_READS = setOf(
    Opcode.IGET, Opcode.IGET_WIDE, Opcode.IGET_OBJECT, Opcode.IGET_BOOLEAN, Opcode.IGET_BYTE, Opcode.IGET_CHAR,
    Opcode.IGET_SHORT,
)
private val RETURNS = setOf(Opcode.RETURN, Opcode.RETURN_WIDE, Opcode.RETURN_OBJECT)

/**
 * The field a Kotlin data class prints after [label] in its toString(), such as ", message=". R8 renames
 * the field, but the label is the source name.
 */
internal fun ClassDef.toStringField(label: String): FieldReference {
    val toString = methods.firstOrNull {
        it.name == "toString" && it.parameterTypes.isEmpty() && it.returnType == "Ljava/lang/String;"
    } ?: throw PatchException("Rumble: $type has no toString")
    val instructions = toString.implementation!!.instructions.toList()
    val labelIndex = instructions.indexOfFirst { it.constString() == label }
    if (labelIndex < 0) throw PatchException("Rumble: $type doesn't print \"$label\"")
    val read = instructions.drop(labelIndex + 1).firstOrNull { it.opcode in FIELD_READS }
        ?: throw PatchException("Rumble: no field after \"$label\" in $type")
    return (read as ReferenceInstruction).reference as FieldReference
}

/**
 * The getter of the field a Kotlin data class prints after [label] in its toString(), as a smali method
 * reference: the one method without parameters that only reads that field and returns it.
 */
internal fun ClassDef.toStringGetter(label: String): String {
    val field = toStringField(label)
    val getters = methods.filter { method ->
        val code = method.implementation?.instructions?.toList() ?: return@filter false
        method.parameterTypes.isEmpty() && method.returnType == field.type && code.size == 2 &&
            code[0].opcode in FIELD_READS && (code[0] as ReferenceInstruction).reference == field &&
            code[1].opcode in RETURNS
    }
    val getter = getters.singleOrNull()
        ?: throw PatchException("Rumble: expected one getter for \"$label\" in $type, found ${getters.size}")
    return "$type->${getter.name}()${getter.returnType}"
}
