package io.github.jordanwoodson.patches.kick.playback

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import io.github.jordanwoodson.patches.kick.settings.kickSettings
import io.github.jordanwoodson.patches.kick.shared.Constants.COMPATIBILITY_KICK
import io.github.jordanwoodson.patches.kick.shared.hex
import io.github.jordanwoodson.patches.kick.shared.kickHermes
import io.github.jordanwoodson.patches.kick.shared.kickHermesPatch
import io.github.jordanwoodson.patches.shared.settings.Section
import io.github.jordanwoodson.patches.shared.settings.SwitchSetting
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val KEY = "default_video_quality"

@Suppress("unused")
val defaultVideoQualityPatch = bytecodePatch(
    name = "Default video quality",
    description = "Sets the default adaptive quality ceiling to the selected height when that rendition exists. " +
        "Kick can still choose a lower quality. The player's Auto option uses this ceiling.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_KICK)
    dependsOn(kickHermesPatch)
    val quality by stringOption(
        key = "quality", default = "1080",
        values = listOf(1080, 720, 480, 360, 240).associate { "${it}p" to "$it" },
        title = "Default quality ceiling", description = "Adaptive playback ceiling. Repatch to change this height.",
        required = true,
    ) { it in listOf("1080", "720", "480", "360", "240") }
    execute {
        val height = quality!!.toInt()
        // The serialized PlayerConfig default is 720. Replacing only the initial store read loses
        // 1080 on the first load: _getResetStreamLoadingStates compares it with the old default and
        // restores getInitialStateFor's 720. Keep all ten consumers coherent without changing the
        // serialized literal table. Each anchor includes the preceding PlayerConfig read; this
        // identifies anonymous functions and repeated reads without relying on function IDs.
        // Explicit quality selection and the original reset/clamping rules remain intact, using
        // the selected ceiling wherever Kick previously used its 720 default (including Auto).
        val reads = listOf(
            "45 09 09 00 89 eb 45 09 09 01 ac d7", // usePlayerStoreIVSModuleSlice
            "45 09 09 01 89 eb 45 09 09 12 ac d7", // onData
            "45 05 05 02 89 eb 45 05 05 03 ac d7", // _getResetStreamLoadingStates
            "45 06 06 01 89 eb 45 05 06 02 ac d7", // setUserSelectedQuality fallback
            "45 04 04 01 89 eb 45 04 04 02 ac d7", // setSelectedQualityGroupPreset: Auto
            "45 04 04 00 89 eb 45 04 04 01 ac d7", // getInitialStateFor
            "45 05 07 10 89 eb 45 06 05 11 ac d7", // cleanupState: no prior ceiling
            "45 04 05 10 89 eb 45 04 04 11 ac d7", // cleanupState: clamp prior ceiling
            "45 07 07 10 89 eb 45 07 07 11 ac d7", // cleanupState: reset preset
            "45 05 05 06 89 eb 45 05 05 07 ac d7", // _applyQualityScale fallback
        )
        for (pattern in reads) {
            val before = hex(pattern)
            val constant = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN)
                .put(0x8c.toByte()).put(before[7]).putInt(height).array()
            kickHermes.replaceMatchingFunction(before, KEY, before, before.copyOfRange(0, 6) + constant)
        }
        kickSettings.add(SwitchSetting(KEY, "Default video quality",
            "Uses an adaptive ${height}p default and Auto ceiling when available; player selections work normally.",
            Section.PLAYBACK, restart = true))
    }
}
