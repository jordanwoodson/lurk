package io.github.jordanwoodson.patches.rumble.playback

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.smali.ExternalLabel
import io.github.jordanwoodson.patches.rumble.settings.rumbleSettings
import io.github.jordanwoodson.patches.rumble.settings.settingsPatch
import io.github.jordanwoodson.patches.rumble.shared.Constants.COMPATIBILITY_RUMBLE
import io.github.jordanwoodson.patches.rumble.shared.single
import io.github.jordanwoodson.patches.shared.settings.ChoiceSetting
import io.github.jordanwoodson.patches.shared.settings.Section
import io.github.jordanwoodson.patches.shared.settings.intValue
import io.github.jordanwoodson.patches.shared.settings.requireLocals

private val QUALITIES = listOf(2160, 1440, 1080, 720, 480, 360, 240)
private const val KEY = "video_quality"

@Suppress("unused")
val defaultVideoQualityPatch = bytecodePatch(
    name = "Default video quality",
    description = "Starts videos at the chosen quality, or the closest one available, instead of 480p. Once you " +
        "pick a quality in the player, Rumble keeps using that one. Live streams stay on Auto.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_RUMBLE)
    dependsOn(settingsPatch)

    val quality by stringOption(
        key = "quality",
        default = "1080",
        values = QUALITIES.associate { "${it}p" to it.toString() },
        title = "Quality",
        description = "The video height to start with, such as 1080. Can be changed later in Settings > Lurk.",
        required = true,
    ) { it?.toIntOrNull() in 144..4320 }

    execute {
        val height = quality!!.toInt()

        // Rumble passes the quality last picked in the player (height and bitrate), or nulls. With nulls it
        // takes the video's preferred height from the server, which for phones is 480p. Fill in the
        // chosen height and the highest bitrate instead; the closest available height is then picked.
        // A height of 0 on the settings screen leaves Rumble's choice.
        VideoSourceSelectionFingerprint.single("video source selection").method.apply {
            requireLocals(1)
            addInstructionsWithLabels(
                0,
                """
                    if-nez p2, :picked
                    ${intValue(KEY, "v0")}
                    if-eqz v0, :picked
                    invoke-static { v0 }, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
                    move-result-object p2
                    const v0, 0x7fffffff
                    invoke-static { v0 }, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
                    move-result-object p3
                """,
                ExternalLabel("picked", getInstruction(0)),
            )
        }

        rumbleSettings.add(
            ChoiceSetting(
                KEY,
                "Default video quality",
                "Until you pick a quality in the player.",
                Section.PLAYBACK,
                default = height.toString(),
                choices = listOf("Rumble's default" to "0") +
                    (QUALITIES + height).distinct().sortedDescending().map { "${it}p" to it.toString() },
            ),
        )
    }
}
