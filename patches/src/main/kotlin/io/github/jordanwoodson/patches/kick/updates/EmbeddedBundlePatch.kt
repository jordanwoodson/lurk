package io.github.jordanwoodson.patches.kick.updates

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import io.github.jordanwoodson.patches.kick.shared.Constants.COMPATIBILITY_KICK
import org.w3c.dom.Element

/** Hermes edits must not be superseded by a downloaded Expo bundle, including cached updates. */
internal val embeddedBundlePatch = resourcePatch(
    description = "Uses Kick's embedded app code so downloaded updates cannot replace the patches.",
) {
    compatibleWith(COMPATIBILITY_KICK)
    execute {
        document("AndroidManifest.xml").use { doc ->
            val nodes = doc.getElementsByTagName("meta-data")
            val enabled = (0 until nodes.length).map { nodes.item(it) as Element }
                .singleOrNull { it.getAttribute("android:name") == "expo.modules.updates.ENABLED" }
                ?: throw PatchException("Kick: Expo update configuration missing")
            enabled.setAttribute("android:value", "false")
        }
    }
}
