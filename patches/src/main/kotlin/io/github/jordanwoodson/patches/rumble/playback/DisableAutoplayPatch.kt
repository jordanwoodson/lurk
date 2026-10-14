package io.github.jordanwoodson.patches.rumble.playback

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import io.github.jordanwoodson.patches.rumble.settings.rumbleSettings
import io.github.jordanwoodson.patches.rumble.settings.settingsPatch
import io.github.jordanwoodson.patches.rumble.shared.Constants.COMPATIBILITY_RUMBLE
import io.github.jordanwoodson.patches.rumble.shared.enumConstant
import io.github.jordanwoodson.patches.rumble.shared.single
import io.github.jordanwoodson.patches.shared.settings.SETTINGS_CLASS
import io.github.jordanwoodson.patches.shared.settings.Section
import io.github.jordanwoodson.patches.shared.settings.SwitchSetting
import io.github.jordanwoodson.patches.shared.settings.isEnabled

private const val NEXT_VIDEO_KEY = "autoplay_next_video_off"
private const val FEED_PREVIEWS_KEY = "feed_previews_off"

@Suppress("unused")
val disableAutoplayPatch = bytecodePatch(
    name = "Disable autoplay",
    description = "Stops Rumble from playing the next video when one ends, and from playing muted previews in " +
        "feeds, including the featured video on Home. Only the defaults change: Settings > Playback can turn " +
        "either back on.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_RUMBLE)
    dependsOn(settingsPatch)

    val nextVideo by booleanOption(
        key = "nextVideo",
        default = true,
        title = "Next video",
        description = "Don't play the next video when one ends (Autoplay next video). Can be changed later in " +
            "Settings > Lurk.",
    )
    val feedPreviews by booleanOption(
        key = "feedPreviews",
        default = true,
        title = "Feed previews",
        description = "Don't play muted previews in feeds or the featured video on Home (Playback in feeds: Off). " +
            "Can be changed later in Settings > Lurk.",
    )

    execute {
        val preferences = UserPreferencesFingerprint.single("user preference manager").classDef

        // autoplayOnKey ?: true  ->  ?: !nextVideoOff
        // The default is a branch target; the replaced instruction keeps the branch, and the rest goes after it.
        preferenceDefault(preferences, "autoplayOnKey", "Ljava/lang/Boolean;->booleanValue()Z").let { (reader, default) ->
            val register = (reader.implementation!!.instructions[default] as OneRegisterInstruction).registerA
            reader.replaceInstruction(default, "const-string v$register, \"$NEXT_VIDEO_KEY\"")
            reader.addInstructions(
                default + 1,
                """
                    invoke-static/range { v$register .. v$register }, $SETTINGS_CLASS->isEnabled(Ljava/lang/String;)Z
                    move-result v$register
                    xor-int/lit8 v$register, v$register, 0x1
                """,
            )
        }

        // playbackInFeedKey ?: ALWAYS_ON.value  ->  ?: (feedPreviewsOff ? OFF : ALWAYS_ON).value
        preferenceDefault(preferences, "playbackInFeedKey", "Ljava/lang/Integer;->intValue()I").let { (reader, default) ->
            val alwaysOn = reader.implementation!!.instructions[default]
            if (alwaysOn.opcode != Opcode.SGET_OBJECT) throw PatchException("Rumble: unexpected feed playback default")
            val register = (alwaysOn as OneRegisterInstruction).registerA
            val alwaysOnField = (alwaysOn as ReferenceInstruction).reference as FieldReference
            val off = classDefBy(alwaysOnField.type).enumConstant("OFF")
            reader.replaceInstruction(default, "const-string v$register, \"$FEED_PREVIEWS_KEY\"")
            reader.addInstructionsWithLabels(
                default + 1,
                """
                    invoke-static/range { v$register .. v$register }, $SETTINGS_CLASS->isEnabled(Ljava/lang/String;)Z
                    move-result v$register
                    if-eqz v$register, :always_on
                    sget-object v$register, $off
                    goto :picked
                    :always_on
                    sget-object v$register, $alwaysOnField
                """,
                ExternalLabel("picked", reader.getInstruction(default + 1)),
            )
        }

        // Home asks for its featured video to play even when feed playback is off. While previews are off,
        // let Rumble's setting decide.
        FeaturedVideoPlayerFingerprint.single("featured video player").method.apply {
            addInstructionsWithLabels(
                0,
                """
                    if-eqz p3, :keep
                    ${isEnabled(FEED_PREVIEWS_KEY, "p3")}
                    xor-int/lit8 p3, p3, 0x1
                """,
                ExternalLabel("keep", getInstruction(0)),
            )
        }

        rumbleSettings.add(
            SwitchSetting(
                NEXT_VIDEO_KEY,
                "No autoplay of the next video",
                "Autoplay next video is off unless you turn it on in Settings > Playback.",
                Section.PLAYBACK,
                default = nextVideo == true,
                restart = true,
            ),
            SwitchSetting(
                FEED_PREVIEWS_KEY,
                "No feed previews",
                "Playback in feeds is off unless you change it in Settings > Playback, and the featured " +
                    "video on Home follows it.",
                Section.PLAYBACK,
                default = feedPreviews == true,
                restart = true,
            ),
        )
    }
}

/**
 * The preference manager reads each DataStore key in a flow like `prefs[key] ?: default`. Returns that
 * reader and the index of the instruction producing the default, found through the key's name:
 * constructor (key string -> field), the synthetic accessor for that field, the inner class method
 * that calls the accessor and unboxes the stored value with [unbox], and the branch taken when nothing
 * is stored.
 */
private fun BytecodePatchContext.preferenceDefault(
    preferences: MutableClass,
    key: String,
    unbox: String,
): Pair<MutableMethod, Int> {
    val constructor = preferences.methods.single { it.name == "<init>" && it.constStrings().contains(key) }
    val constructorCode = constructor.implementation!!.instructions.toList()
    val keyIndex = constructorCode.indexOfFirst { it.constString() == key }
    val field = constructorCode.drop(keyIndex).first { it.opcode == Opcode.IPUT_OBJECT }
        .let { (it as ReferenceInstruction).reference as FieldReference }

    val accessor = preferences.methods.singleOrNull { method ->
        AccessFlags.STATIC.isSet(method.accessFlags) &&
            method.implementation?.instructions?.any {
                it.opcode == Opcode.IGET_OBJECT && (it as ReferenceInstruction).reference == field
            } == true
    } ?: throw PatchException("Rumble: no accessor for the $key preference")
    val accessorCall = "${accessor.definingClass}->${accessor.name}(${accessor.parameterTypes.joinToString("")})" +
        accessor.returnType

    val innerPrefix = preferences.type.removeSuffix(";") + "$"
    val readers = mutableListOf<Pair<ClassDef, Method>>()
    classDefForEach { classDef ->
        if (!classDef.type.startsWith(innerPrefix)) return@classDefForEach
        classDef.methods.filter { it.calls(accessorCall) && it.calls(unbox) }.forEach { readers += classDef to it }
    }
    val (readerClass, readerMethod) = readers.singleOrNull()
        ?: throw PatchException("Rumble: expected one reader for the $key preference, found ${readers.size}")
    val reader = mutableClassDefBy(readerClass).methods.single {
        it.name == readerMethod.name &&
            it.parameterTypes.map(CharSequence::toString) == readerMethod.parameterTypes.map(CharSequence::toString)
    }

    val code = reader.implementation!!.instructions.toList()
    val unboxIndex = code.indexOfFirst { it is ReferenceInstruction && it.reference.toString() == unbox }
    val nullCheck = (unboxIndex downTo 0).first { code[it].opcode == Opcode.IF_EQZ }
    return reader to (code[nullCheck] as BuilderOffsetInstruction).target.location.index
}

private fun Method.calls(method: String) = implementation?.instructions?.any {
    it is ReferenceInstruction && it.reference.toString() == method
} == true

private fun com.android.tools.smali.dexlib2.iface.instruction.Instruction.constString() =
    if (opcode == Opcode.CONST_STRING) ((this as ReferenceInstruction).reference as StringReference).string else null

private fun Method.constStrings() = implementation?.instructions?.mapNotNull { it.constString() }.orEmpty()
