package io.github.jordanwoodson.patches.rumble.tipping

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import io.github.jordanwoodson.patches.rumble.settings.rumbleSettings
import io.github.jordanwoodson.patches.rumble.settings.settingsPatch
import io.github.jordanwoodson.patches.rumble.shared.Constants.COMPATIBILITY_RUMBLE
import io.github.jordanwoodson.patches.rumble.shared.enumConstant
import io.github.jordanwoodson.patches.rumble.shared.enumWith
import io.github.jordanwoodson.patches.rumble.shared.parameters
import io.github.jordanwoodson.patches.rumble.shared.single
import io.github.jordanwoodson.patches.shared.settings.Section
import io.github.jordanwoodson.patches.shared.settings.SwitchSetting
import io.github.jordanwoodson.patches.shared.settings.addInstructionsIfEnabled

private const val KEY = "hide_tipping"

@Suppress("unused")
val hideTippingPatch = bytecodePatch(
    name = "Hide tipping and Rants",
    description = "Hides the Tip buttons on channels, videos and Shorts, Rants (paid live chat messages) and " +
        "paying with Rumble Wallet. Rants other viewers send still show.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_RUMBLE)
    dependsOn(settingsPatch)

    execute {
        // Drop Tip from the buttons the channel and video headers may show.
        val tip = enumWith("Follow", "Join", "Tip", "Shop").enumConstant("Tip")
        CreatorActionButtonsFingerprint.single("creator action buttons").method.apply {
            val buttons = parameters().indexOf("Ljava/util/List;")
            if (buttons < 0 || !AccessFlags.STATIC.isSet(accessFlags)) {
                throw PatchException("Rumble: unexpected creator action buttons signature")
            }
            // A static composable: parameter n is register pn.
            addInstructionsIfEnabled(
                KEY,
                """
                    new-instance v0, Ljava/util/ArrayList;
                    move-object/from16 v1, p$buttons
                    invoke-direct { v0, v1 }, Ljava/util/ArrayList;-><init>(Ljava/util/Collection;)V
                    sget-object v1, $tip
                    invoke-virtual { v0, v1 }, Ljava/util/ArrayList;->remove(Ljava/lang/Object;)Z
                    move-object/16 p$buttons, v0
                """,
                locals = 2,
            )
        }

        val returnNull = """
            const/4 v0, 0x0
            return-object v0
        """
        CreatorTipsInfoFingerprint.single("Creator.tipsInfo getter").method.addInstructionsIfEnabled(KEY, returnNull)
        VideoSourceWalletFingerprint.single("VideoSource.wallet getter").method.addInstructionsIfEnabled(KEY, returnNull)

        RantsEnabledFingerprint.single("LiveChatConfigRant.enable getter").method.addInstructionsIfEnabled(
            KEY,
            """
                sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                return-object v0
            """,
        )
        RantsWalletFingerprint.single("LiveChatConfigRant.walletEnabled getter").method.addInstructionsIfEnabled(
            KEY,
            """
                const/4 v0, 0x0
                return v0
            """,
        )

        rumbleSettings.add(
            SwitchSetting(KEY, "Hide tipping and Rants", "Tip buttons, Rants and paying with Rumble Wallet.", Section.INTERFACE),
        )
    }
}
