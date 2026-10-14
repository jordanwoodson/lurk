package io.github.jordanwoodson.patches.rumble.upsells

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import io.github.jordanwoodson.patches.rumble.shared.Constants.COMPATIBILITY_RUMBLE
import io.github.jordanwoodson.patches.rumble.shared.single

@Suppress("unused")
val hidePremiumUpsellsPatch = bytecodePatch(
    name = "Hide Premium upsells",
    description = "Hides Rumble Premium promotions: the sheet that opens over videos, the Upgrade Now banner on " +
        "Home and My Feed, the special offer on the account screen, the downloads pitch and Premium gifts in " +
        "live chat. Premium-only videos still say so.",
) {
    compatibleWith(COMPATIBILITY_RUMBLE)

    execute {
        // suspend fun shouldShowPremiumPromo(): Boolean
        PremiumPromoFingerprint.single("Premium promo check").method.addInstructions(
            0,
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
            fingerprint.single("$name composable").method.addInstructions(0, "return-void")
        }

        PremiumGiftsFingerprint.single("LiveChatConfig.giftList getter").method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """,
        )
    }
}
