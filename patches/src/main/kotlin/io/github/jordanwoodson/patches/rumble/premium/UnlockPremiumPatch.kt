package io.github.jordanwoodson.patches.rumble.premium

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import io.github.jordanwoodson.patches.rumble.shared.Constants.COMPATIBILITY_RUMBLE

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium",
    description = "Enables Rumble's local premium state after signing in and refreshing the profile, " +
        "including ad-free playback and hiding premium upsells. " +
        "Server-controlled content and subscription benefits still require server authorization.",
) {
    compatibleWith(COMPATIBILITY_RUMBLE)

    execute {
        val match = ProfilePremiumFingerprint.matchAllOrNull()?.singleOrNull()
            ?: throw PatchException("Rumble: expected exactly one UserProfile.isPremium getter. " +
                "This patch targets 3.21.0; recheck the profile model before updating compatibility.")

        // The profile mapper passes this value to the session's isUserPremiumKey flow.
        // Replace only the read: retain the original return register and all other profile data,
        // including nullable subscription metadata. A signed-out session has no profile to map.
        val fieldRead = match.instructionMatches.first()
        val register = fieldRead.getInstruction<OneRegisterInstruction>().registerA
        match.method.replaceInstruction(fieldRead.index, "const/4 v$register, 0x1")
    }
}
