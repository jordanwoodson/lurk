package io.github.jordanwoodson.patches.kick.ads

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import io.github.jordanwoodson.patches.kick.settings.kickSettings
import io.github.jordanwoodson.patches.kick.shared.Constants.COMPATIBILITY_KICK
import io.github.jordanwoodson.patches.kick.shared.hex
import io.github.jordanwoodson.patches.kick.shared.kickHermes
import io.github.jordanwoodson.patches.kick.shared.kickHermesPatch
import io.github.jordanwoodson.patches.shared.settings.Section
import io.github.jordanwoodson.patches.shared.settings.SwitchSetting

private const val KEY = "bypass_vod_ad_sessions"

@Suppress("unused")
val bypassVodAdSessionsPatch = bytecodePatch(
    name = "Bypass VOD ad sessions",
    description = "Experimental: uses the original VOD URL instead of requesting a separate ad-session manifest. " +
        "Does not remove ads stitched into live streams. Enable in Lurk settings after patching.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_KICK)
    dependsOn(kickHermesPatch)

    execute {
        // The asynchronous wrapper must finish with an object. Both the initial-VOD loader and
        // live-rewind loader fall back to their original VOD URL when manifestUrl is absent.
        // Replace APIRequest.send(request) with {}, keeping the generator state transitions and
        // Promise completion. This does not touch postPlayerTelemetry, which acquires playback URLs.
        // Its compiler-generated wrapper has the same name, so the request construction must
        // identify exactly one function before the name is checked.
        val request = hex("45 06 00 00 62 e5 45 05 06 01 30 ae 02 00 c5 1b 00 00 53 7a 04 00 52 00 07 01 6e 05 05 06 00")
        val function = kickHermes.matchingFunction(request)
        if (function.name != "?anon_0_getVodSession") {
            throw PatchException("Kick: the VOD session request is in an unexpected function")
        }
        kickHermes.replace(
            function, KEY, request,
            hex("45 06 00 00 62 e5 45 05 06 01 30 ae 02 00 c5 1b 00 00 53 7a 04 00 52 00 07 01 05 05 10 05 05"),
            defaultEnabled = false,
        )
        kickSettings.add(
            SwitchSetting(
                KEY,
                "Bypass VOD ad sessions (experimental)",
                "Use the original VOD URL. Turn off if VOD or live rewind fails. Live ads are unaffected.",
                Section.PRIVACY,
                default = false,
                restart = true,
            ),
        )
    }
}
