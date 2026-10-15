package io.github.jordanwoodson.patches.twitch.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import io.github.jordanwoodson.patches.shared.settings.Section
import io.github.jordanwoodson.patches.shared.settings.SwitchSetting
import io.github.jordanwoodson.patches.shared.settings.requireLocals
import io.github.jordanwoodson.patches.twitch.settings.twitchSettings
import io.github.jordanwoodson.patches.twitch.settings.twitchSettingsPatch
import io.github.jordanwoodson.patches.twitch.shared.Constants.COMPATIBILITY_TWITCH
import io.github.jordanwoodson.patches.twitch.shared.twitchMatch

private const val KEY = "mute_stitched_ads"

@Suppress("unused")
val muteStitchedAdsPatch = bytecodePatch(
    name = "Mute stitched ads",
    description = "While an ad break that Twitch's server stitched into a stream plays, mutes the player and " +
        "covers the video with black, then restores both when the stream returns. Works in the native and " +
        "React Native players. The ad still plays and the break isn't shorter.",
) {
    compatibleWith(COMPATIBILITY_TWITCH)
    dependsOn(twitchSettingsPatch)
    execute {
        // Native player: its HLS metadata handler sees each EXT-X-DATERANGE ad cue first. The
        // extension mutes the IVS player itself, which Twitch never does (it mutes with the volume).
        val cue = NativeAdCueFingerprint.twitchMatch("native player HLS metadata handler")
        val player = cue.classDef.fields.singleOrNull {
            it.type == IVS_MEDIA_PLAYER && !AccessFlags.STATIC.isSet(it.accessFlags)
        } ?: throw PatchException("Twitch: native player has no unique IVS player")
        val playerField = "${cue.classDef.type}->${player.name}:$IVS_MEDIA_PLAYER"
        cue.method.requireLocals(1)
        cue.method.addInstructions(0, """
            iget-object v0, p0, $playerField
            invoke-static { v0, p1 }, $ADS->onNativeCue(Ljava/lang/Object;Lorg/json/JSONObject;)V
        """)
        // The native player keeps one IVS player for every stream it loads: a new load ends a break.
        val load = cue.classDef.methods.filter { method ->
            method.implementation?.instructions?.any {
                val reference = (it as? ReferenceInstruction)?.reference as? MethodReference
                it.opcode == Opcode.INVOKE_INTERFACE && reference?.definingClass == IVS_MEDIA_PLAYER &&
                    reference.name == "load"
            } == true
        }.singleOrNull() ?: throw PatchException("Twitch: native player has no unique stream load")
        load.requireLocals(1)
        load.addInstructions(0, """
            iget-object v0, p0, $playerField
            invoke-static { v0 }, $ADS->onNativeLoad(Ljava/lang/Object;)V
        """)

        // Native cover: the full-size root of Twitch's stitched-ad overlay, while it shows an ad.
        val overlay = StitchedAdOverlayFingerprint.twitchMatch("native stitched-ad overlay").method
        val adPlaying = StitchedAdPlayingFingerprint.twitchMatch("native stitched-ad playing state").originalClassDef.type
        if (overlay.implementation!!.instructions.none {
                it.opcode == Opcode.INSTANCE_OF && ((it as ReferenceInstruction).reference as TypeReference).type == adPlaying
            }) throw PatchException("Twitch: stitched-ad overlay no longer draws its ad-playing state")
        val root = classDefBy(BASE_VIEW_DELEGATE).fields.singleOrNull {
            it.type == "Landroid/view/View;" && !AccessFlags.STATIC.isSet(it.accessFlags)
        } ?: throw PatchException("Twitch: view delegate has no unique root view")
        overlay.requireLocals(2)
        overlay.addInstructions(0, """
            iget-object v0, p0, $BASE_VIEW_DELEGATE->${root.name}:Landroid/view/View;
            instance-of v1, p1, $adPlaying
            invoke-static { v0, v1 }, $ADS->coverNativeAd(Landroid/view/View;Z)V
        """)

        // React Native player view: every player's metadata, before the view drops what it doesn't
        // route to its ad state. A new stream URL or a release (the player may return to a pool)
        // ends the break.
        val view = classDefBy(RN_PLAYER_VIEW)
        if (view.methods.none { it.name == "getPlayer" && it.parameterTypes.isEmpty() && it.returnType == IVS_MEDIA_PLAYER }) {
            throw PatchException("Twitch: React Native player view has no player getter")
        }
        RnPlayerMetadataFingerprint.twitchMatch("React Native player metadata").method.apply {
            requireLocals(1)
            addInstructions(0, """
                invoke-direct { p0 }, $RN_PLAYER_VIEW->getPlayer()$IVS_MEDIA_PLAYER
                move-result-object v0
                invoke-static { p0, v0, p1, p2 }, $ADS->onRnCue(Landroid/view/View;Ljava/lang/Object;Ljava/lang/String;Ljava/nio/ByteBuffer;)V
            """)
        }
        RnPlayerStreamUrlFingerprint.twitchMatch("React Native player stream URL").method.addInstructions(0,
            "invoke-static { p0, p1 }, $ADS->onRnStreamUrl(Landroid/view/View;Ljava/lang/String;)V")
        RnPlayerReleaseFingerprint.twitchMatch("React Native player release").method.addInstructions(0,
            "invoke-static { p0 }, $ADS->onRnRelease(Landroid/view/View;)V")

        twitchSettings.add(SwitchSetting(KEY, "Mute stitched ads",
            "Mute and black out ad breaks stitched into streams. The ad still plays for its full length.",
            Section.PLAYBACK))
    }
}
