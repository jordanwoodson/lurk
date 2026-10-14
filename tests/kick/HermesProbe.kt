package io.github.jordanwoodson.patches.kick.shared

import java.io.File
import java.security.MessageDigest

/** Runs against the supplied Kick bundle, not a reconstructed or synthetic bytecode file. */
fun main(args: Array<String>) {
    val original = File(args[0]).readBytes()
    val bundle = HermesBundle(original)
    check(bundle.function("KicksShopButton").offset == 0x8d20c0)
    check(bundle.function("preferencesSettingsSlice").body.size == 97)
    fun rejects(block: () -> Unit) = check(runCatching(block).isFailure)
    rejects { HermesBundle(original.copyOf().also { it[8] = 98 }) }
    rejects { HermesBundle(original.copyOf().also { it[500] = (it[500].toInt() xor 1).toByte() }) }
    rejects { bundle.function("missing") }
    rejects { bundle.function("get") }
    val disabled = HermesBundle(original)
    disabled.returnNull("KicksShopButton", "hide_buttons", defaultEnabled = false)
    check(disabled.preview().contentEquals(original))
    rejects { bundle.replace("KicksShopButton", "invalid", byteArrayOf(52), byteArrayOf(1, 2)) }
    for (name in listOf("KicksShopButton", "GetKicksButton", "ChannelSubButton", "ChannelGiftSubButton")) {
        bundle.returnNull(name, "hide_buttons")
    }
    rejects { bundle.returnNull("KicksShopButton", "overlap") }
    val preview = bundle.previewDefault("disable_previews")
    check(preview.original.size == 97 && preview.replacement.size == 97)
    val patched = bundle.preview()
    check(patched.size == original.size)
    check(MessageDigest.getInstance("SHA-1").digest(patched.copyOfRange(0, patched.size - 20))
        .contentEquals(patched.copyOfRange(patched.size - 20, patched.size)))
    val restored = patched.copyOf()
    for (edit in bundle.edits) edit.original.copyInto(restored, edit.offset)
    MessageDigest.getInstance("SHA-1").digest(restored.copyOfRange(0, restored.size - 20))
        .copyInto(restored, restored.size - 20)
    check(restored.contentEquals(original))
    check(bundle.replacements().size == 5)
    check(bundle.manifest().contains("\"embeddedSha256\""))
    File(args[1]).writeBytes(patched)
    File(args[2]).writeText(bundle.manifest())
    println("PASS: actual v99 headers, fail-closed checks, five edits, footer, and exact disabled restoration")
}
