package io.github.jordanwoodson.patches.twitch.shared

import java.io.File
import java.security.MessageDigest

/** Real Twitch v98 headers and native/RN privacy sink edits, including negative controls. */
fun main(args: Array<String>) {
    val original = File(args[0]).readBytes()
    val bundle = HermesBundle(original)
    check(bundle.function("createSpadeReporter").offset == 0x133047d)
    check(bundle.function("createSpadeReporter").body.size == 736)
    check(bundle.stringId("nativeSpadeEnabled") >= 0)
    fun rejects(block: () -> Unit) = check(runCatching(block).isFailure)
    rejects { HermesBundle(original.copyOf(100)) }
    rejects { HermesBundle(original.copyOf().also { it[8] = 99 }) }
    rejects { HermesBundle(original.copyOf().also { it[500] = (it[500].toInt() xor 1).toByte() }) }
    rejects { bundle.function("missing") }
    rejects { bundle.function("get") }
    val enqueue = bundle.matchingFunction(hex("34 05 00 89 08 01 34 06 01 3b 09 06 0a"))
    val flush = bundle.matchingFunction(hex("34 03 00 93 00 93 0c 93 05 3b 01 03 21"))
    check(enqueue.name == "enqueue" && enqueue.offset == 0x17768d5 && enqueue.body.size == 1042)
    check(flush.name == "flush" && flush.offset == 0x1776ce7 && flush.body.size == 343)
    rejects { bundle.replace(enqueue, "invalid", byteArrayOf(52), byteArrayOf(1, 2)) }
    bundle.returnUndefined(enqueue, "disable_analytics")
    bundle.returnUndefined(flush, "disable_analytics")
    rejects { bundle.returnUndefined(enqueue, "overlap") }
    val patched = bundle.preview()
    check(patched.size == original.size)
    check(MessageDigest.getInstance("SHA-1").digest(patched.copyOfRange(0, patched.size - 20))
        .contentEquals(patched.copyOfRange(patched.size - 20, patched.size)))
    val restored = patched.copyOf()
    bundle.edits.forEach { it.original.copyInto(restored, it.offset) }
    MessageDigest.getInstance("SHA-1").digest(restored.copyOfRange(0, restored.size - 20))
        .copyInto(restored, restored.size - 20)
    check(restored.contentEquals(original))
    check(bundle.replacements().size == 2)
    check(bundle.manifest().contains("\"version\":98"))
    File(args[1]).writeBytes(patched)
    File(args[2]).writeText(bundle.manifest())
    println("PASS: actual Twitch v98 headers, negative inputs, unique sink edits, footer, exact all-off restoration")
}
