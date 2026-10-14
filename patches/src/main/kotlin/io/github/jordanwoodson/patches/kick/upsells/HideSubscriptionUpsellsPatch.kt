package io.github.jordanwoodson.patches.kick.upsells

import app.morphe.patcher.patch.bytecodePatch
import io.github.jordanwoodson.patches.kick.settings.kickSettings
import io.github.jordanwoodson.patches.kick.shared.Constants.COMPATIBILITY_KICK
import io.github.jordanwoodson.patches.kick.shared.hex
import io.github.jordanwoodson.patches.kick.shared.kickHermes
import io.github.jordanwoodson.patches.kick.shared.kickHermesPatch
import io.github.jordanwoodson.patches.shared.settings.Section
import io.github.jordanwoodson.patches.shared.settings.SwitchSetting

private const val KEY = "hide_subscription_upsells"

@Suppress("unused")
val hideSubscriptionUpsellsPatch = bytecodePatch(
    name = "Hide subscription upsells",
    description = "Hides channel Subscribe and Gift a Sub buttons and the leaderboard gift-subscription prompt. " +
        "Keeps the existing subscribed indicator.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_KICK)
    dependsOn(kickHermesPatch)

    execute {
        // The branch to the existing subscribed icon stays intact. Only the unsubscribed path
        // returns null, after all hooks have run; subscription state and access are unchanged.
        kickHermes.replace(
            "ChannelSubButton",
            KEY,
            hex("45 04 09 16 3b ad b2 a9 00 00 00 04 5e 04 06 0c"),
            hex("45 04 09 16 3b ad b2 a9 00 00 00 04 94 04 76 04"),
        )
        kickHermes.returnNull("ChannelGiftSubButton", KEY)
        kickHermes.returnNull("LeaderboardSubsFooter", KEY)
        kickSettings.add(
            SwitchSetting(
                KEY,
                "Hide subscription upsells",
                "Hide channel subscription buttons and the leaderboard gift prompt.",
                Section.INTERFACE,
                restart = true,
            ),
        )
    }
}
