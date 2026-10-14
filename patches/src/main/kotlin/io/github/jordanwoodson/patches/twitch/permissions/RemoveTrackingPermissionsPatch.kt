package io.github.jordanwoodson.patches.twitch.permissions

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import io.github.jordanwoodson.patches.twitch.shared.Constants.COMPATIBILITY_TWITCH
import org.w3c.dom.Element

private val TRACKING_PERMISSIONS = setOf(
    "com.google.android.gms.permission.AD_ID",
    "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
)

@Suppress("unused")
val removeTrackingPermissionsPatch = resourcePatch(
    name = "Remove tracking permissions",
    description = "Removes advertising ID and Google Play install-referrer permissions. " +
        "Other identifiers and analytics require separate controls.",
) {
    compatibleWith(COMPATIBILITY_TWITCH)
    execute {
        document("AndroidManifest.xml").use { doc ->
            val manifest = doc.documentElement
            val nodes = manifest.getElementsByTagName("uses-permission")
            val targets = (0 until nodes.length).map { nodes.item(it) as Element }
                .filter { it.getAttribute("android:name") in TRACKING_PERMISSIONS }
            val missing = TRACKING_PERMISSIONS - targets.map { it.getAttribute("android:name") }.toSet()
            if (missing.isNotEmpty()) throw PatchException("Twitch: tracking permissions missing: $missing")
            targets.forEach { manifest.removeChild(it) }
        }
    }
}
