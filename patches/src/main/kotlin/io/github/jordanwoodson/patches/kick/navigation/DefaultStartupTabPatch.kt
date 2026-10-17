package io.github.jordanwoodson.patches.kick.navigation

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import io.github.jordanwoodson.patches.kick.player.MainActivityCreateFingerprint
import io.github.jordanwoodson.patches.kick.settings.kickMatch
import io.github.jordanwoodson.patches.kick.settings.kickSettings
import io.github.jordanwoodson.patches.kick.settings.kickSettingsPatch
import io.github.jordanwoodson.patches.kick.shared.Constants.COMPATIBILITY_KICK
import io.github.jordanwoodson.patches.shared.settings.ChoiceSetting
import io.github.jordanwoodson.patches.shared.settings.Section

@Suppress("unused")
val defaultStartupTabPatch = bytecodePatch(
    name = "Default startup tab",
    description = "Adds a choice of the screen Kick opens on instead of Home: Browse or Following. Links and " +
        "notifications still open what they point to. Starts on Home. Choose another in Lurk settings > Interface.",
) {
    compatibleWith(COMPATIBILITY_KICK)
    dependsOn(kickSettingsPatch)

    execute {
        // Before the activity reads its intent. Kick's router opens an incoming kick.com/browse or
        // kick.com/following link on that tab, so the launch becomes such a link.
        MainActivityCreateFingerprint.kickMatch("MainActivity.onCreate").method.addInstructions(0,
            "invoke-static/range { p0 .. p1 }, Lio/github/jordanwoodson/extension/shared/kick/KickStartup;" +
                "->rewriteLaunch(Landroid/app/Activity;Landroid/os/Bundle;)V")
        kickSettings.add(
            ChoiceSetting(
                "default_tab",
                "Default startup tab",
                "The screen Kick opens on. Following needs an account.",
                Section.INTERFACE,
                "0",
                listOf("Home" to "0", "Browse" to "1", "Following" to "2"),
            ),
        )
    }
}
