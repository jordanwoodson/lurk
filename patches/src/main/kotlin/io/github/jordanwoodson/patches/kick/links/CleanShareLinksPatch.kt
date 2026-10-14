package io.github.jordanwoodson.patches.kick.links

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import io.github.jordanwoodson.patches.kick.settings.kickMatch
import io.github.jordanwoodson.patches.kick.settings.kickSettings
import io.github.jordanwoodson.patches.kick.settings.kickSettingsPatch
import io.github.jordanwoodson.patches.kick.shared.Constants.COMPATIBILITY_KICK
import io.github.jordanwoodson.patches.shared.settings.Section
import io.github.jordanwoodson.patches.shared.settings.SwitchSetting

private const val EXTRA = "Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;"
private const val PRIVACY = "Lio/github/jordanwoodson/extension/shared/kick/KickPrivacy;"

@Suppress("unused")
val cleanShareLinksPatch = bytecodePatch(
    name = "Clean share links",
    description = "Removes known campaign and advertising click parameters from kick.com links in share sheets. " +
        "Keeps timestamps, clip IDs, unknown parameters and fragments. Kick's current share links are already clean.",
) {
    compatibleWith(COMPATIBILITY_KICK)
    dependsOn(kickSettingsPatch)
    execute {
        listOf(ShareIntentFingerprint to 11, ReactShareFingerprint to 2).forEach { (fingerprint, expected) ->
            val method = fingerprint.kickMatch("share boundary with $expected string extras").method
            val calls = method.implementation!!.instructions.withIndex().filter { (_, instruction) ->
                instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                    ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.toString() == EXTRA
            }
            if (calls.size != expected) throw PatchException("Kick: expected $expected share extras, found ${calls.size}")
            calls.forEach { (index, instruction) ->
                val call = instruction as FiveRegisterInstruction
                if (call.registerCount != 3) throw PatchException("Kick: unexpected share extra arguments")
                // The static wrapper has identical argument/return registers and only cleans EXTRA_TEXT.
                method.replaceInstruction(index, "invoke-static { v${call.registerC}, v${call.registerD}, v${call.registerE} }, " +
                    "$PRIVACY->putShareExtra(Landroid/content/Intent;Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;")
            }
        }
        kickSettings.add(SwitchSetting("clean_share_links", "Clean share links",
            "Removes known tracking parameters from Kick links you share; keeps functional parameters.", Section.PRIVACY))
    }
}
