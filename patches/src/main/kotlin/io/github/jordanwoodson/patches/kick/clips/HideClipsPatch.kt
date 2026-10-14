package io.github.jordanwoodson.patches.kick.clips

import app.morphe.patcher.patch.bytecodePatch
import io.github.jordanwoodson.patches.kick.settings.kickSettings
import io.github.jordanwoodson.patches.kick.shared.Constants.COMPATIBILITY_KICK
import io.github.jordanwoodson.patches.kick.shared.hex
import io.github.jordanwoodson.patches.kick.shared.kickHermes
import io.github.jordanwoodson.patches.kick.shared.kickHermesPatch
import io.github.jordanwoodson.patches.shared.settings.Section
import io.github.jordanwoodson.patches.shared.settings.SwitchSetting

private const val KEY = "hide_clips"

@Suppress("unused")
val hideClipsPatch = bytecodePatch(
    name = "Hide Clips",
    description = "Hides Clips tabs in Browse, channels and categories, and the popular-clips row on channels. " +
        "Shared clip links still open.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_KICK)
    dependsOn(kickHermesPatch)

    execute {
        // BrowseScreenLayout's memoized screen list: categories, livestreams, clips. Select the
        // anonymous producer by the categories object and translation call, not its function ID.
        val browseTabs = kickHermes.matchingFunction(
            hex("02 02 e9 05 00 00 ed cf 03 00 34 01 00 3b 04 01 00 90 01 15 f5"),
        )
        kickHermes.replace(browseTabs, KEY, hex("09 01 03 00 5a 01 02 00"), hex("09 01 02 00 5a 01 02 00"))
        kickHermes.replace(browseTabs, KEY, hex("5a 01 02 02 76 01"), hex("93 00 93 00 76 01"))

        // ChannelLayout's list: home, about, videos, clips, chat. Remove the Clips append and
        // compact Chat into index 3. r0 already holds undefined throughout both small producers.
        val channelTabs = kickHermes.matchingFunction(
            hex("02 02 2f 1b 00 00 a9 83 04 00 34 01 00 3b 04 01 02 90 01 4a d4"),
        )
        kickHermes.replace(channelTabs, KEY, hex("09 01 05 00 5a 01 02 00"), hex("09 01 04 00 5a 01 02 00"))
        kickHermes.replace(channelTabs, KEY, hex("52 02 03 01 5a 01 02 03"), hex("52 02 03 01 93 00 93 00"))
        kickHermes.replace(channelTabs, KEY, hex("5a 01 02 04 76 01"), hex("5a 01 02 03 76 01"))

        // The category header's screen list has Live and Clips. The existing r1 is undefined;
        // restoring it twice is a harmless, complete four-byte replacement for the Clips append.
        kickHermes.replace(
            "BrowseCategoriesHeader", KEY,
            hex("09 0c 02 00 5a 0c 10 00 5a 0c 15 01"),
            hex("09 0c 01 00 5a 0c 10 00 93 01 93 01"),
        )

        // Remove the PopularClipsRow React element in its parent, preserving the video and
        // recent-category rows. React ignores null children; there is no blank Clips screen.
        kickHermes.replace(
            "ChannelHomeContent", KEY,
            hex("45 11 0e 1b 9c eb 02 0e 28 10 00 00 d3 c9 01 00 52 0e 10 00 6f 0e 12 03 11 0e"),
            hex("45 11 0e 1b 9c eb 02 0e 28 10 00 00 d3 c9 01 00 52 0e 10 00 94 0e 94 0e 94 0e"),
        )
        kickSettings.add(
            SwitchSetting(
                KEY,
                "Hide Clips",
                "Hide Clips tabs and the channel clips row. Shared clip links still open.",
                Section.INTERFACE,
                restart = true,
            ),
        )
    }
}
