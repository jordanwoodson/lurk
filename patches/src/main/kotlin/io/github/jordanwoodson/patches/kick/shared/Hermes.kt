package io.github.jordanwoodson.patches.kick.shared

import app.morphe.patcher.patch.PatchException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * The small part of Hermes Static v99 needed for bounded edits. Format reference:
 * https://github.com/facebook/hermes/blob/static_h/include/hermes/BCGen/HBC/BytecodeFileFormat.h
 * Opcode reference: the same branch's BytecodeList.def. Older Hermes main has a different layout.
 * No disassembled JavaScript is compiled, and no table, function or branch is relocated.
 */
internal class HermesBundle(input: ByteArray) {
    private val bytes = input.copyOf()
    private val strings: List<String>
    private val functions: List<HermesFunction>
    private val literalStart: Int
    private val literalSize: Int
    private val changes = mutableListOf<HermesEdit>()
    val edits: List<HermesEdit> get() = changes.toList()

    init {
        ensure(bytes.size >= 148, "truncated header")
        ensure(bytes.copyOfRange(0, 8).contentEquals(hex("c61fbc03c103191f")), "invalid magic")
        ensure(u32(8) == 99, "expected Hermes v99")
        ensure(u32(32) == bytes.size, "file length does not match header")
        ensure(digest("SHA-1", bytes.copyOfRange(0, bytes.size - 20))
            .contentEquals(bytes.copyOfRange(bytes.size - 20, bytes.size)), "invalid SHA-1 footer")
        ensure(bytes.sha256() == SOURCE_SHA256, "unrecognized Kick 40.32.1 bundle")
        val functionCount = u32(40)
        val stringCount = u32(52)
        val overflowCount = u32(56)
        val stringStorageSize = u32(60)
        ensure(functionCount in 1..bytes.size / 12, "invalid function count")
        ensure(stringCount in 1..bytes.size / 4, "invalid string count")
        var cursor = checkedEnd(128, functionCount, 12)
        cursor = checkedEnd(align(cursor), u32(44), 4) // String-kind runs.
        cursor = checkedEnd(align(cursor), u32(48), 4) // Identifier hashes.
        val stringTable = align(cursor)
        cursor = checkedEnd(stringTable, stringCount, 4)
        val overflowTable = align(cursor)
        cursor = checkedEnd(overflowTable, overflowCount, 8)
        val stringStorage = align(cursor)
        cursor = checkedEnd(stringStorage, stringStorageSize, 1)
        strings = List(stringCount) { index ->
            val entry = u32Bits(stringTable + index * 4)
            val utf16 = entry and 1L != 0L
            var offset = ((entry ushr 1) and 0x7fffff).toInt()
            var length = (entry ushr 24).toInt()
            if (length == 255) {
                ensure(offset < overflowCount, "string overflow index outside table")
                length = u32(overflowTable + offset * 8 + 4)
                offset = u32(overflowTable + offset * 8)
            }
            val byteLength = length.toLong() * if (utf16) 2 else 1
            ensure(offset.toLong() + byteLength <= stringStorageSize, "string outside storage")
            String(bytes, stringStorage + offset, byteLength.toInt(),
                if (utf16) StandardCharsets.UTF_16LE else StandardCharsets.ISO_8859_1)
        }
        literalStart = align(cursor)
        literalSize = u32(80)
        checkedEnd(literalStart, literalSize, 1)
        functions = List(functionCount) { id ->
            val position = 128 + id * 12
            val first = u32Bits(position)
            val second = u32Bits(position + 4)
            val flags = u8(position + 11)
            val large = flags and 32 != 0
            val header = if (large) {
                ((first and 0x1ffffff) or (((second ushr 14) and 0xff) shl 24)).toInt()
            } else position
            if (large) checkedEnd(header, 36, 1)
            val offset = if (large) u32(header) else (first and 0x1ffffff).toInt()
            val size = if (large) u32(header + 12) else (second and 0x3fff).toInt()
            val name = if (large) u32(header + 16) else ((second ushr 14) and 0xff).toInt()
            val numbers = if (large) u32(header + 20) else ((second ushr 22) and 31).toInt()
            val nonPointers = if (large) u32(header + 24) else ((second ushr 27) and 31).toInt()
            val frame = if (large) u32(header + 28) else u8(header + 8)
            val actualFlags = if (large) u8(header + 35) else flags
            ensure(name < stringCount, "invalid function name index")
            checkedEnd(offset, size, 1)
            HermesFunction(id, strings[name], offset, bytes.copyOfRange(offset, offset + size),
                frame, numbers, nonPointers, actualFlags)
        }
    }

    fun function(name: String): HermesFunction = functions.filter { it.name == name }.singleOrNull()
        ?: fail("expected one function named $name")

    /** An anonymous function must be selected by a unique bytecode anchor, never its build-local ID. */
    fun matchingFunction(anchor: ByteArray): HermesFunction {
        ensure(anchor.isNotEmpty(), "empty function anchor")
        return functions.filter { it.body.positions(anchor).isNotEmpty() }.singleOrNull()
            ?: fail("expected one function with bytecode anchor ${anchor.toHex()}")
    }

    fun returnNull(functionName: String, key: String, defaultEnabled: Boolean = true) {
        val function = function(functionName)
        ensure(function.flags and 0x18 == 0, "$functionName has exception/debug metadata")
        val body = function.body
        ensure(body.size >= 9 && body[0].toInt() and 255 == 0x34,
            "$functionName has an unexpected prologue")
        val length = when (body[3].toInt() and 255) {
            0x40, 0x89 -> 6 // GetParentEnvironment, then CreateFunctionEnvironment or LoadParam.
            0x41 -> 9 // GetParentEnvironment, CreateTopLevelEnvironment.
            else -> fail("$functionName has an unexpected environment instruction")
        }
        val register = function.numberRegisters + function.nonPointerRegisters
        ensure(register in 0..255 && register < function.frameSize, "$functionName has no general register")
        val replacement = ByteArray(length)
        byteArrayOf(0x94.toByte(), register.toByte(), 0x76, register.toByte()).copyInto(replacement)
        // Unreachable (0) pads through the last replaced instruction boundary, after Ret.
        add(function, key, 0, body.copyOfRange(0, length), replacement, defaultEnabled)
    }

    fun replace(functionName: String, key: String, before: ByteArray, after: ByteArray,
                defaultEnabled: Boolean = true) = replace(function(functionName), key, before, after, defaultEnabled)

    fun replaceMatchingFunction(anchor: ByteArray, key: String, before: ByteArray, after: ByteArray,
                                defaultEnabled: Boolean = true) =
        replace(matchingFunction(anchor), key, before, after, defaultEnabled)

    fun replace(function: HermesFunction, key: String, before: ByteArray, after: ByteArray,
                defaultEnabled: Boolean = true) {
        ensure(before.isNotEmpty(), "empty bytecode pattern")
        val offset = function.body.positions(before).singleOrNull()
            ?: fail("expected one pattern in ${function.name.ifEmpty { "anonymous function" }}")
        add(function, key, offset, before, after, defaultEnabled)
    }

    fun replaceBody(functionName: String, key: String, body: ByteArray, defaultEnabled: Boolean = true): HermesEdit {
        val function = function(functionName)
        return add(function, key, 0, function.body, body, defaultEnabled)
    }

    /**
     * Keeps all twelve defaults and closures, changing only showStreamPreviews to false. Two existing
     * literal sequences also fit a short NewObjectWithBuffer instruction; their shorter encodings
     * free six bytes for the property assignment. This function has no branches or exception table.
     */
    fun previewDefault(key: String, defaultEnabled: Boolean = true): HermesEdit {
        val function = function("preferencesSettingsSlice")
        val body = function.body
        ensure(body.size == 97 && function.flags and 0x18 == 0, "unexpected preview slice")
        ensure(body.copyOfRange(0x11, 0x1b).contentEquals(hex("0200930f00000ffa0200")),
            "unexpected preview state object")
        ensure(body.copyOfRange(0x1b, 0x25).contentEquals(hex("0202940f0000dc ca0100")),
            "unexpected PiP state object")
        val values = bytes.copyOfRange(literalStart, literalStart + minOf(literalSize, 65536))
        val mainLiteral = values.positions(hex("1507")).firstOrNull() ?: fail("no short preview literals")
        val pipLiteral = values.positions(hex("03")).firstOrNull() ?: fail("no short PiP literals")
        fun shortObject(register: Int, shape: Int, literal: Int) = byteArrayOf(1, register.toByte(),
            shape.toByte(), (shape ushr 8).toByte(), literal.toByte(), (literal ushr 8).toByte())
        val replacement = body.copyOfRange(0, 0x11) + shortObject(0, 3987, mainLiteral) +
            byteArrayOf(0x96.toByte(), 2, 0x52, 0, 2, 2) + shortObject(2, 3988, pipLiteral) +
            body.copyOfRange(0x25, body.size) + byteArrayOf(0, 0)
        return replaceBody(function.name, key, replacement, defaultEnabled)
    }

    private fun add(function: HermesFunction, key: String, relative: Int, before: ByteArray,
                    after: ByteArray, defaultEnabled: Boolean): HermesEdit {
        ensure(key.matches(Regex("[a-z][a-z0-9_]*")), "invalid setting key")
        ensure(before.size == after.size && before.isNotEmpty(), "edit must preserve bytecode length")
        ensure(functions.count { it.offset == function.offset && it.body.isNotEmpty() } == 1,
            "function bytecode is shared")
        val offset = function.offset + relative
        ensure(relative >= 0 && relative.toLong() + before.size <= function.body.size, "edit outside function")
        ensure(bytes.copyOfRange(offset, offset + before.size).contentEquals(before), "original bytes differ")
        ensure(changes.none { offset < it.offset + it.original.size && it.offset < offset + before.size },
            "overlapping Hermes edits")
        return HermesEdit(key, offset, before.copyOf(), after.copyOf(), defaultEnabled, function.id)
            .also { changes += it }
    }

    /** Full original function bodies make Morphe's first-match byte replacement unambiguous. */
    fun replacements(): Set<Pair<String, String>> = changes.groupBy { it.functionId }.map { (id, edits) ->
        val function = functions[id]
        ensure(bytes.positions(function.body).singleOrNull() == function.offset,
            "function pattern is not globally unique")
        val replacement = function.body.copyOf()
        edits.filter { it.defaultEnabled }.forEach { it.replacement.copyInto(replacement, it.offset - function.offset) }
        function.body.toHex() to replacement.toHex()
    }.toSet()

    fun preview(): ByteArray = bytes.copyOf().also { result ->
        changes.filter { it.defaultEnabled }.forEach { it.replacement.copyInto(result, it.offset) }
        digest("SHA-1", result.copyOfRange(0, result.size - 20)).copyInto(result, result.size - 20)
    }

    fun manifest(): String {
        val entries = changes.sortedBy { it.offset }.joinToString(",") {
            "{\"key\":\"${it.key}\",\"offset\":${it.offset},\"original\":\"${it.original.toHex()}\"," +
                "\"replacement\":\"${it.replacement.toHex()}\",\"default\":${it.defaultEnabled}}"
        }
        return "{\"format\":1,\"version\":99,\"sourceSha256\":\"$SOURCE_SHA256\"," +
            "\"embeddedSha256\":\"${preview().sha256()}\",\"edits\":[$entries]}"
    }

    private fun u8(offset: Int): Int = bytes[offset].toInt() and 255
    private fun u32Bits(offset: Int): Long {
        checkedEnd(offset, 4, 1)
        return (0..3).fold(0L) { value, index -> value or (u8(offset + index).toLong() shl (8 * index)) }
    }
    private fun u32(offset: Int): Int = u32Bits(offset).also {
        ensure(it <= Int.MAX_VALUE, "integer outside supported file size")
    }.toInt()
    private fun checkedEnd(offset: Int, count: Int, size: Int): Int {
        val end = offset.toLong() + count.toLong() * size
        ensure(offset >= 0 && count >= 0 && end <= bytes.size - 20, "table or function outside bundle")
        return end.toInt()
    }
    private fun align(offset: Int) = (offset + 3) and -4

    companion object {
        const val SOURCE_SHA256 = "a9d911b3f8b640d8e56b7527929a093c2356d97992409c453d167fdf39538d9a"
    }
}

internal data class HermesFunction(val id: Int, val name: String, val offset: Int, val body: ByteArray,
    val frameSize: Int, val numberRegisters: Int, val nonPointerRegisters: Int, val flags: Int)

internal data class HermesEdit(val key: String, val offset: Int, val original: ByteArray,
    val replacement: ByteArray, val defaultEnabled: Boolean, val functionId: Int)

internal fun hex(value: String): ByteArray {
    val compact = value.filterNot { it.isWhitespace() }
    ensure(compact.length % 2 == 0, "odd hexadecimal string")
    return compact.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}

private fun ByteArray.toHex() = joinToString("") { "%02x".format(it.toInt() and 255) }
private fun ByteArray.sha256() = digest("SHA-256", this).toHex()
private fun digest(name: String, value: ByteArray) = MessageDigest.getInstance(name).digest(value)
private fun ensure(condition: Boolean, message: String) { if (!condition) fail(message) }
private fun fail(message: String): Nothing = throw PatchException("Kick Hermes: $message")
private fun ByteArray.positions(pattern: ByteArray): List<Int> {
    if (pattern.isEmpty() || pattern.size > size) return emptyList()
    val matches = mutableListOf<Int>()
    for (offset in 0..size - pattern.size) {
        if (this[offset] != pattern[0]) continue
        var index = 1
        while (index < pattern.size && this[offset + index] == pattern[index]) index++
        if (index == pattern.size) matches += offset
    }
    return matches
}
