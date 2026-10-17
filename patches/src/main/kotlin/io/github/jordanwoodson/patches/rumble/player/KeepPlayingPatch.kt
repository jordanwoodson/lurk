package io.github.jordanwoodson.patches.rumble.player

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import io.github.jordanwoodson.patches.rumble.settings.rumbleSettings
import io.github.jordanwoodson.patches.rumble.settings.settingsPatch
import io.github.jordanwoodson.patches.rumble.shared.Constants.COMPATIBILITY_RUMBLE
import io.github.jordanwoodson.patches.rumble.shared.RUMBLE_PLAYBACK
import io.github.jordanwoodson.patches.rumble.shared.single
import io.github.jordanwoodson.patches.shared.settings.ChoiceSetting
import io.github.jordanwoodson.patches.shared.settings.Section
import io.github.jordanwoodson.patches.shared.settings.requireLocals

private const val KEY = "audio_focus"

@Suppress("unused")
val keepPlayingPatch = bytecodePatch(
    name = "Keep playing during other sounds",
    description = "Choose whether Rumble pauses when another app plays sound: always (Rumble's behavior), only " +
        "for longer interruptions, or never. Calls still pause playback. Starts off. Choose it in Lurk " +
        "settings > Playback.",
) {
    compatibleWith(COMPATIBILITY_RUMBLE)
    dependsOn(settingsPatch)

    execute {
        // The player service resumes on any focus gain and pauses on any loss.
        AudioFocusChangeFingerprint.single("player service audio focus listener").method.apply {
            requireLocals(1)
            addInstructionsWithLabels(
                0,
                """
                    invoke-static/range { p1 .. p1 }, $RUMBLE_PLAYBACK->ignoreFocusLoss(I)Z
                    move-result v0
                    if-eqz v0, :original
                    return-void
                """,
                ExternalLabel("original", getInstruction(0)),
            )
        }

        rumbleSettings.add(
            ChoiceSetting(
                KEY,
                "Keep playing during other sounds",
                "What Rumble does when another app plays sound. Calls always pause.",
                Section.PLAYBACK,
                default = "0",
                choices = listOf(
                    "Pause (Rumble's default)" to "0",
                    "Keep playing during short sounds" to "1",
                    "Keep playing" to "2",
                ),
            ),
        )
    }
}
