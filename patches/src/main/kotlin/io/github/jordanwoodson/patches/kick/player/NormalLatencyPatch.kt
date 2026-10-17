package io.github.jordanwoodson.patches.kick.player

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import io.github.jordanwoodson.patches.kick.settings.kickMatch
import io.github.jordanwoodson.patches.kick.settings.kickSettings
import io.github.jordanwoodson.patches.kick.settings.kickSettingsPatch
import io.github.jordanwoodson.patches.kick.shared.Constants.COMPATIBILITY_KICK
import io.github.jordanwoodson.patches.shared.ivs.IvsLowLatencyFingerprint
import io.github.jordanwoodson.patches.shared.settings.Section
import io.github.jordanwoodson.patches.shared.settings.SwitchSetting

@Suppress("unused")
val normalLatencyPatch = bytecodePatch(
    name = "Normal latency",
    description = "Adds a switch that turns off low-latency mode for live streams: a few seconds more delay, fewer " +
        "rebuffers on weak connections. Starts off. Turn it on in Lurk settings > Playback.",
) {
    compatibleWith(COMPATIBILITY_KICK)
    dependsOn(kickSettingsPatch)

    execute {
        // Kick's player view turns low latency on for every player it creates; nothing else sets it.
        IvsLowLatencyFingerprint.kickMatch("IVS low latency").method.addInstructions(0, """
            invoke-static/range { p1 .. p1 }, $KICK_PLAYER->lowLatency(Z)Z
            move-result p1
        """)
        kickSettings.add(
            SwitchSetting(
                "normal_latency",
                "Normal latency",
                "Turns off low-latency mode for live streams: a few seconds more delay, fewer rebuffers. Applies " +
                    "to streams opened afterwards.",
                Section.PLAYBACK,
                default = false,
            ),
        )
    }
}
