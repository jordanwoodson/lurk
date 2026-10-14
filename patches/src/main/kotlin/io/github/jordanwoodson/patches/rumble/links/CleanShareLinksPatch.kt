package io.github.jordanwoodson.patches.rumble.links

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import io.github.jordanwoodson.patches.rumble.shared.Constants.COMPATIBILITY_RUMBLE
import io.github.jordanwoodson.patches.rumble.shared.single

@Suppress("unused")
val cleanShareLinksPatch = bytecodePatch(
    name = "Clean share links",
    description = "Removes everything after the ? from Rumble links before they are shared, so tracking tags such " +
        "as e9s (where you found the video) can't go out with them. Rumble 3.21.0 shares clean links; this " +
        "keeps them clean if the server starts adding tags.",
) {
    compatibleWith(COMPATIBILITY_RUMBLE)

    execute {
        // share(text, title): p1 is the link. Only rumble.com links (and its subdomains) are changed.
        ShareTextFingerprint.single("share use case").method.apply {
            addInstructionsWithLabels(
                0,
                """
                    if-eqz p1, :keep
                    invoke-static { p1 }, Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;
                    move-result-object v0
                    invoke-virtual { v0 }, Landroid/net/Uri;->getHost()Ljava/lang/String;
                    move-result-object v0
                    if-eqz v0, :keep
                    const-string v1, "rumble.com"
                    invoke-virtual { v0, v1 }, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                    move-result v1
                    if-nez v1, :rumble
                    const-string v1, ".rumble.com"
                    invoke-virtual { v0, v1 }, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z
                    move-result v1
                    if-eqz v1, :keep
                    :rumble
                    const/16 v0, 0x3f
                    invoke-virtual { p1, v0 }, Ljava/lang/String;->indexOf(I)I
                    move-result v0
                    if-ltz v0, :keep
                    const/4 v1, 0x0
                    invoke-virtual { p1, v1, v0 }, Ljava/lang/String;->substring(II)Ljava/lang/String;
                    move-result-object p1
                """,
                ExternalLabel("keep", getInstruction(0)),
            )
        }
    }
}
