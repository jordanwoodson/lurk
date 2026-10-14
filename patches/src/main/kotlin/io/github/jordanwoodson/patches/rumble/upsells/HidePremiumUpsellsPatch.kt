package io.github.jordanwoodson.patches.rumble.upsells

import app.morphe.patcher.patch.bytecodePatch
import io.github.jordanwoodson.patches.rumble.settings.rumbleSettings
import io.github.jordanwoodson.patches.rumble.settings.settingsPatch
import io.github.jordanwoodson.patches.rumble.shared.Constants.COMPATIBILITY_RUMBLE
import io.github.jordanwoodson.patches.rumble.shared.single
import io.github.jordanwoodson.patches.shared.settings.Section
import io.github.jordanwoodson.patches.shared.settings.SwitchSetting
import io.github.jordanwoodson.patches.shared.settings.addInstructionsIfEnabled

private const val KEY = "hide_premium_upsells"

@Suppress("unused")
val hidePremiumUpsellsPatch = bytecodePatch(
    name = "Hide Premium upsells",
    description = "Hides Rumble Premium promotions: the sheet that opens over videos, the Upgrade Now banner on " +
        "Home and My Feed, the special offer on the account screen, the downloads pitch and Premium gifts in " +
        "live chat. Premium-only videos still say so.",
) {
    compatibleWith(COMPATIBILITY_RUMBLE)
    dependsOn(settingsPatch)

    execute {
        // suspend fun shouldShowPremiumPromo(): Boolean
        PremiumPromoFingerprint.single("Premium promo check").method.addInstructionsIfEnabled(
            KEY,
            """
                sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                return-object v0
            """,
        )

        // Composables that draw nothing when they return straight away.
        listOf(
            PremiumBannerFingerprint to "PremiumBannerView",
            PremiumOfferBannerFingerprint to "PremiumOfferBanner",
            DownloadsUpsellFingerprint to "DownloadsUpsellView",
        ).forEach { (fingerprint, name) ->
            fingerprint.single("$name composable").method.addInstructionsIfEnabled(KEY, "return-void")
        }

        PremiumGiftsFingerprint.single("LiveChatConfig.giftList getter").method.addInstructionsIfEnabled(
            KEY,
            """
                const/4 v0, 0x0
                return-object v0
            """,
        )

        rumbleSettings.add(
            SwitchSetting(
                KEY,
                "Hide Premium upsells",
                "The Premium sheet, banners and offer, the downloads pitch and live chat gifts.",
                Section.INTERFACE,
            ),
        )
    }
}
