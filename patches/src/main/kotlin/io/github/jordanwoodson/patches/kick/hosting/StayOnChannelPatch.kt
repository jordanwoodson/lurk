package io.github.jordanwoodson.patches.kick.hosting

import app.morphe.patcher.patch.bytecodePatch
import io.github.jordanwoodson.patches.kick.settings.kickSettings
import io.github.jordanwoodson.patches.kick.shared.Constants.COMPATIBILITY_KICK
import io.github.jordanwoodson.patches.kick.shared.HermesBundle.Constant
import io.github.jordanwoodson.patches.kick.shared.kickHermes
import io.github.jordanwoodson.patches.kick.shared.kickHermesPatch
import io.github.jordanwoodson.patches.shared.settings.Section
import io.github.jordanwoodson.patches.shared.settings.SwitchSetting

private const val KEY = "stay_on_host"

@Suppress("unused")
val stayOnChannelPatch = bytecodePatch(
    name = "Stay on channel when it hosts",
    description = "When the channel you're watching hosts another one, stays on it instead of switching when the " +
        "countdown ends. Kick's Watch now button still switches. Starts off. Turn it on in Lurk settings > Interface.",
) {
    compatibleWith(COMPATIBILITY_KICK)
    dependsOn(kickHermesPatch)

    execute {
        // The host dialog's countdown calls this when it ends: on a stream, clip or VOD screen it redirects to
        // the hosted channel. Returning at once leaves the dialog with its buttons.
        val countdownEnd = kickHermes.functionUsing(
            "host countdown end",
            reads = listOf("currentPath"),
            loads = listOf("/stream", "/clip", "/vod"),
        )
        kickHermes.returnAtEntry(countdownEnd, KEY, Constant.UNDEFINED, defaultEnabled = false)
        kickSettings.add(
            SwitchSetting(
                KEY,
                "Stay on channel when it hosts",
                "Don't switch to the hosted channel when the countdown ends. Watch now still switches.",
                Section.INTERFACE,
                default = false,
                restart = true,
            ),
        )
    }
}
