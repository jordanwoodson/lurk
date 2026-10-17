package io.github.jordanwoodson.patches.rumble.player

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import io.github.jordanwoodson.patches.rumble.settings.rumbleSettings
import io.github.jordanwoodson.patches.rumble.settings.settingsPatch
import io.github.jordanwoodson.patches.rumble.shared.Constants.COMPATIBILITY_RUMBLE
import io.github.jordanwoodson.patches.rumble.shared.RUMBLE_PLAYBACK
import io.github.jordanwoodson.patches.shared.settings.ChoiceSetting
import io.github.jordanwoodson.patches.shared.settings.Section

@Suppress("unused")
val sleepTimerPatch = bytecodePatch(
    name = "Sleep timer",
    description = "Pauses playback a chosen time after it last started or resumed, including background play " +
        "and picture-in-picture. Starts off. Choose a time in Lurk settings > Playback.",
) {
    compatibleWith(COMPATIBILITY_RUMBLE)
    dependsOn(settingsPatch)

    execute {
        // The player service's Player.Listener: onPlaybackStateChanged(int) checks IDLE (1) and ENDED (4),
        // and onIsPlayingChanged(boolean) is the other method taking one boolean.
        val listeners = mutableListOf<ClassDef>()
        classDefForEach { classDef ->
            if (!classDef.type.startsWith(PLAYER_SERVICE.removeSuffix(";") + "$")) return@classDefForEach
            val checksStates = classDef.methods.any { method ->
                val literals = method.implementation?.instructions?.mapNotNull {
                    (it as? NarrowLiteralInstruction)?.narrowLiteral
                }.orEmpty()
                method.parameterTypes.map(CharSequence::toString) == listOf("I") && method.returnType == "V" &&
                    1 in literals && 4 in literals
            }
            val playing = classDef.methods.count {
                it.parameterTypes.map(CharSequence::toString) == listOf("Z") && it.returnType == "V"
            }
            if (checksStates && playing == 1) listeners += classDef
        }
        val listener = listeners.singleOrNull()
            ?: throw PatchException("Rumble: expected one player service listener, found ${listeners.size}")
        mutableClassDefBy(listener).methods.single {
            it.parameterTypes.map(CharSequence::toString) == listOf("Z") && it.returnType == "V"
        }.addInstruction(
            0,
            "invoke-static/range { p0 .. p1 }, $RUMBLE_PLAYBACK->onPlayingChanged(Ljava/lang/Object;Z)V",
        )

        // The same choices as Twitch's Sleep timer.
        rumbleSettings.add(
            ChoiceSetting(
                "sleep_timer",
                "Sleep timer",
                "Pauses playback this long after it last started or resumed, including background play.",
                Section.PLAYBACK,
                "0",
                listOf(
                    "Off" to "0", "15 minutes" to "15", "30 minutes" to "30", "45 minutes" to "45",
                    "1 hour" to "60", "1.5 hours" to "90", "2 hours" to "120",
                ),
            ),
        )
    }
}
