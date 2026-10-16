package io.github.jordanwoodson.patches.rumble.tabs

import app.morphe.patcher.patch.bytecodePatch
import io.github.jordanwoodson.patches.rumble.settings.rumbleSettings
import io.github.jordanwoodson.patches.rumble.settings.settingsPatch
import io.github.jordanwoodson.patches.rumble.shared.Constants.COMPATIBILITY_RUMBLE
import io.github.jordanwoodson.patches.rumble.shared.hideBottomTab
import io.github.jordanwoodson.patches.shared.settings.Section
import io.github.jordanwoodson.patches.shared.settings.SwitchSetting

private const val KEY = "hide_create_tab"

@Suppress("unused")
val hideCreateTabPatch = bytecodePatch(
    name = "Hide Create tab",
    description = "Removes the Create (camera and upload) button from the bottom bar. Starts off. Turn it on in " +
        "Lurk settings > Interface.",
) {
    compatibleWith(COMPATIBILITY_RUMBLE)
    dependsOn(settingsPatch)

    execute {
        // The bottom bar's tabs are Discover (Home), ShortsScreen, CameraGalleryScreen (Create), Feeds (My Feed)
        // and Profile (Account).
        hideBottomTab("CameraGalleryScreen", KEY)

        rumbleSettings.add(
            SwitchSetting(
                KEY,
                "Hide Create tab",
                "The camera and upload button in the bottom bar.",
                Section.INTERFACE,
                default = false,
                restart = true,
            ),
        )
    }
}
