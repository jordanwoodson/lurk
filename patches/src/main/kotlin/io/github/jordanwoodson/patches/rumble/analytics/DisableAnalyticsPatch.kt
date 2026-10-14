package io.github.jordanwoodson.patches.rumble.analytics

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import io.github.jordanwoodson.patches.rumble.shared.Constants.COMPATIBILITY_RUMBLE
import io.github.jordanwoodson.patches.rumble.shared.kotlinObject
import io.github.jordanwoodson.patches.rumble.shared.newInstanceTypes
import io.github.jordanwoodson.patches.rumble.shared.parameters
import io.github.jordanwoodson.patches.rumble.shared.singleMethod
import io.github.jordanwoodson.patches.rumble.shared.staticFieldReads
import io.github.jordanwoodson.patches.rumble.shared.unitReference

private const val STRING = "Ljava/lang/String;"

@Suppress("unused")
val disableAnalyticsPatch = bytecodePatch(
    name = "Disable analytics",
    description = "Stops Rumble's own analytics, which it sends whatever the consent setting: clicks, " +
        "impressions, searches, watch progress and player state, with your user ID, username, advertising ID " +
        "and AppsFlyer ID, to e.rumble.com and its debug log. Signed-in watch history may stop updating " +
        "unless watch progress is kept.",
) {
    compatibleWith(COMPATIBILITY_RUMBLE)

    val keepWatchProgress by booleanOption(
        key = "keepWatchProgress",
        default = false,
        title = "Keep watch progress",
        description = "Keep sending watch progress (video, position and player state). Rumble may use it for " +
            "your watch history and for creators' view counts.",
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
        val skip = """
            sget-object v0, $success
            return-object v0
        """

        if (keepWatchProgress == true) {
            // Rumble sends watch progress in its own uploads; let a list through when it holds any.
            val watchProgress = WatchProgressEventToStringFingerprint.matchAllOrNull()?.singleOrNull()?.classDef?.type
                ?: throw PatchException("Rumble: expected exactly one WatchProgressEvent class")
            sendEvents.addInstructionsWithLabels(
                0,
                """
                    invoke-interface { p1 }, Ljava/util/List;->iterator()Ljava/util/Iterator;
                    move-result-object v0
                    :next
                    invoke-interface { v0 }, Ljava/util/Iterator;->hasNext()Z
                    move-result v1
                    if-eqz v1, :skip
                    invoke-interface { v0 }, Ljava/util/Iterator;->next()Ljava/lang/Object;
                    move-result-object v1
                    instance-of v1, v1, $watchProgress
                    if-nez v1, :send
                    goto :next
                    :skip
                    $skip
                """,
                ExternalLabel("send", sendEvents.getInstruction(0)),
            )
        } else {
            sendEvents.addInstructions(0, skip)
        }

        listOf(sendSearchClick, sendDebugEvent).forEach { method ->
            method.addInstructions(
                0,
                """
                    sget-object v0, ${method.unitReference()}
                    return-object v0
                """,
            )
        }
    }
}
