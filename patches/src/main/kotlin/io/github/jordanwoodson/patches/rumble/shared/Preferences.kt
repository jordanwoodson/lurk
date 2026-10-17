package io.github.jordanwoodson.patches.rumble.shared

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

/**
 * Rumble's UserPreferenceManager reads each DataStore key in a flow like `prefs[key] ?: default`. Returns
 * that reader and the index of the instruction producing the default, found through the key's name:
 * constructor (key string -> field), the synthetic accessor for that field, the inner class method
 * that calls the accessor and unboxes the stored value with [unbox], and the branch taken when nothing
 * is stored.
 */
internal fun BytecodePatchContext.preferenceDefault(
    preferences: MutableClass,
    key: String,
    unbox: String,
): Pair<MutableMethod, Int> {
    val constructor = preferences.methods.single { it.name == "<init>" && it.constStrings().contains(key) }
    val constructorCode = constructor.implementation!!.instructions.toList()
    val keyIndex = constructorCode.indexOfFirst { it.constString() == key }
    val field = constructorCode.drop(keyIndex).first { it.opcode == Opcode.IPUT_OBJECT }
        .let { (it as ReferenceInstruction).reference as FieldReference }

    val accessor = preferences.methods.singleOrNull { method ->
        AccessFlags.STATIC.isSet(method.accessFlags) &&
            method.implementation?.instructions?.any {
                it.opcode == Opcode.IGET_OBJECT && (it as ReferenceInstruction).reference == field
            } == true
    } ?: throw PatchException("Rumble: no accessor for the $key preference")
    val accessorCall = "${accessor.definingClass}->${accessor.name}(${accessor.parameterTypes.joinToString("")})" +
        accessor.returnType

    val innerPrefix = preferences.type.removeSuffix(";") + "$"
    val readers = mutableListOf<Pair<ClassDef, Method>>()
    classDefForEach { classDef ->
        if (!classDef.type.startsWith(innerPrefix)) return@classDefForEach
        classDef.methods.filter { it.calls(accessorCall) && it.calls(unbox) }.forEach { readers += classDef to it }
    }
    val (readerClass, readerMethod) = readers.singleOrNull()
        ?: throw PatchException("Rumble: expected one reader for the $key preference, found ${readers.size}")
    val reader = mutableClassDefBy(readerClass).methods.single {
        it.name == readerMethod.name &&
            it.parameterTypes.map(CharSequence::toString) == readerMethod.parameterTypes.map(CharSequence::toString)
    }

    val code = reader.implementation!!.instructions.toList()
    val unboxIndex = code.indexOfFirst { it is ReferenceInstruction && it.reference.toString() == unbox }
    val nullCheck = (unboxIndex downTo 0).first { code[it].opcode == Opcode.IF_EQZ }
    return reader to (code[nullCheck] as BuilderOffsetInstruction).target.location.index
}

/** Whether [method] (a smali method reference) is called anywhere in this method. */
internal fun Method.calls(method: String) = implementation?.instructions?.any {
    it is ReferenceInstruction && it.reference.toString() == method
} == true

/** The string a const-string loads, or null for any other instruction. */
internal fun Instruction.constString() =
    if (opcode == Opcode.CONST_STRING) ((this as ReferenceInstruction).reference as StringReference).string else null

internal fun Method.constStrings() = implementation?.instructions?.mapNotNull { it.constString() }.orEmpty()
