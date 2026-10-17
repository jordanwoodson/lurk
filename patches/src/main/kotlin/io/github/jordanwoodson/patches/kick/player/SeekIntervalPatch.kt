package io.github.jordanwoodson.patches.kick.player

import app.morphe.patcher.patch.bytecodePatch
import io.github.jordanwoodson.patches.kick.settings.kickSettings
import io.github.jordanwoodson.patches.kick.shared.Constants.COMPATIBILITY_KICK
import io.github.jordanwoodson.patches.kick.shared.hex
import io.github.jordanwoodson.patches.kick.shared.kickHermes
import io.github.jordanwoodson.patches.kick.shared.kickHermesPatch
import io.github.jordanwoodson.patches.shared.settings.ChoiceSetting
import io.github.jordanwoodson.patches.shared.settings.Section

private const val KEY = "vod_skip_seconds"

/**
 * PlayerConfig.controls.skip.scale's `vod` and `live-vod` arrays, [10, 10, 10]: three integer literals in the
 * literal buffer, read only by those two arrays (tests/kick/verify_literals.py). The live edge and clip arrays
 * share their bytes with unrelated literals and stay.
 */
private const val SKIP_SCALE_LITERAL = 201721

@Suppress("unused")
val seekIntervalPatch = bytecodePatch(
    name = "Seek interval",
    description = "Sets how far the rewind and forward buttons skip in videos and rewound live streams: 5, 15, 20 " +
        "or 30 seconds instead of 10. Starts at Kick's 10 seconds. Choose another in Lurk settings > Playback.",
) {
    compatibleWith(COMPATIBILITY_KICK)
    dependsOn(kickHermesPatch)

    execute {
        fun scale(seconds: Int) = hex("73") + ByteArray(12) { if (it % 4 == 0) seconds.toByte() else 0 }
        val seconds = listOf(5, 15, 20, 30)
        kickHermes.dataChoice(SKIP_SCALE_LITERAL, KEY, scale(10), seconds.associate { "$it" to scale(it) })
        kickSettings.add(
            ChoiceSetting(
                KEY,
                "Seek interval",
                "How far rewind and forward skip in videos.",
                Section.PLAYBACK,
                "0",
                listOf("Kick's default (10 s)" to "0") + seconds.map { "$it seconds" to "$it" },
                restart = true,
            ),
        )
    }
}
