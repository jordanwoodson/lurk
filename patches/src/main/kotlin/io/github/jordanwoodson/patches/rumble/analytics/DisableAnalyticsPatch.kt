package io.github.jordanwoodson.patches.rumble.analytics

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import io.github.jordanwoodson.patches.rumble.settings.rumbleSettings
import io.github.jordanwoodson.patches.rumble.settings.settingsPatch
import io.github.jordanwoodson.patches.rumble.shared.Constants.COMPATIBILITY_RUMBLE
import io.github.jordanwoodson.patches.rumble.shared.kotlinObject
import io.github.jordanwoodson.patches.rumble.shared.newInstanceTypes
import io.github.jordanwoodson.patches.rumble.shared.parameters
import io.github.jordanwoodson.patches.rumble.shared.singleMethod
import io.github.jordanwoodson.patches.rumble.shared.staticFieldReads
import io.github.jordanwoodson.patches.rumble.shared.unitReference
import io.github.jordanwoodson.patches.shared.settings.Section
import io.github.jordanwoodson.patches.shared.settings.SwitchSetting
import io.github.jordanwoodson.patches.shared.settings.addInstructionsIfEnabled
import io.github.jordanwoodson.patches.shared.settings.isEnabled

private const val STRING = "Ljava/lang/String;"
private const val KEY = "disable_analytics"
private const val KEEP_WATCH_PROGRESS_KEY = "keep_watch_progress"

@Suppress("unused")
val disableAnalyticsPatch = bytecodePatch(
    name = "Disable analytics",
    description = "Stops Rumble's own analytics, which it sends whatever the consent setting: clicks, " +
        "impressions, searches, watch progress and player state, with your user ID, username, advertising ID " +
        "and AppsFlyer ID, to e.rumble.com and its debug log. Signed-in watch history may stop updating " +
        "unless watch progress is kept.",
) {
    compatibleWith(COMPATIBILITY_RUMBLE)
    dependsOn(settingsPatch)

    val keepWatchProgress by booleanOption(
        key = "keepWatchProgress",
        default = false,
        title = "Keep watch progress",
        description = "Keep sending watch progress (video, position and player state). Rumble may use it for " +
            "your watch history and for creators' view counts. Can be changed later in Settings > Lurk.",
    )

    execute {
        val repository = EventRepositoryFingerprint.matchAllOrNull()?.singleOrNull()?.classDef
            ?: throw PatchException("Rumble: expected exactly one event repository. " +
                "This patch targets 3.21.0; recheck the analytics code before updating compatibility.")

        // send(events: List, ...6 strings, continuation): every e.rumble.com upload.
        val sendEvents = repository.singleMethod("event upload") { method ->
            val parameters = method.parameters()
            parameters.size == 8 && parameters[0] == "Ljava/util/List;" &&
                parameters.subList(1, 7).all { it == STRING }
        }
        // Search result clicks and the debug log copies of AppsFlyer and Firebase events.
        val sendSearchClick = repository.singleMethod("search click upload") {
            it.parameters().dropLast(1) == listOf(STRING, STRING, "J", STRING)
        }
        val sendDebugEvent = repository.singleMethod("debug log upload") {
            it.parameters().dropLast(1) == List(6) { STRING } + "Ljava/util/Map;"
        }

        // Callers check the upload's result. Report Success, as after a real upload: the watch time
        // uploader then clears its queue instead of retrying.
        val upload = classDefBy(sendEvents.newInstanceTypes().singleOrNull()
            ?: throw PatchException("Rumble: event upload has no single coroutine"))
        val success = kotlinObject(
            "Success",
            among = upload.methods.flatMap { it.staticFieldReads() }.map { it.definingClass }.toSet(),
        )
        val watchProgress = WatchProgressEventToStringFingerprint.matchAllOrNull()?.singleOrNull()?.classDef?.type
            ?: throw PatchException("Rumble: expected exactly one WatchProgressEvent class")

        // Rumble sends watch progress in its own uploads. While it's kept, let a list through when it
        // holds any.
        sendEvents.addInstructionsIfEnabled(
            KEY,
            """
                ${isEnabled(KEEP_WATCH_PROGRESS_KEY, "v0")}
                if-eqz v0, :skip
                invoke-interface { p1 }, Ljava/util/List;->iterator()Ljava/util/Iterator;
                move-result-object v0
                :next
                invoke-interface { v0 }, Ljava/util/Iterator;->hasNext()Z
                move-result v1
                if-eqz v1, :skip
                invoke-interface { v0 }, Ljava/util/Iterator;->next()Ljava/lang/Object;
                move-result-object v1
                instance-of v1, v1, $watchProgress
                if-nez v1, :lurk_off
                goto :next
                :skip
                sget-object v0, $success
                return-object v0
            """,
            locals = 2,
        )

        listOf(sendSearchClick, sendDebugEvent).forEach { method ->
            method.addInstructionsIfEnabled(
                KEY,
                """
                    sget-object v0, ${method.unitReference()}
                    return-object v0
                """,
            )
        }

        rumbleSettings.add(
            SwitchSetting(
                KEY,
                "Disable analytics",
                "Stops Rumble's own event uploads to e.rumble.com and its debug log.",
                Section.PRIVACY,
            ),
            SwitchSetting(
                KEEP_WATCH_PROGRESS_KEY,
                "Keep watch progress",
                "Still send watch progress, which Rumble may use for your watch history.",
                Section.PRIVACY,
                default = keepWatchProgress == true,
                parent = KEY,
            ),
        )
    }
}
