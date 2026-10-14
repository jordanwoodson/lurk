package io.github.jordanwoodson.patches.kick.permissions

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import io.github.jordanwoodson.patches.kick.shared.Constants.COMPATIBILITY_KICK
import org.w3c.dom.Element

private val TRACKING_PERMISSIONS = setOf(
    "com.google.android.gms.permission.AD_ID",
    "android.permission.ACCESS_ADSERVICES_AD_ID",
    "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
    "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
)

@Suppress("unused")
val removeTrackingPermissionsPatch = resourcePatch(
    name = "Remove tracking permissions",
    description = "Removes advertising ID, ad attribution and Google Play install-referrer permissions. " +
        "Other identifiers and analytics require separate controls.",
) {
    compatibleWith(COMPATIBILITY_KICK)
    execute {
        document("AndroidManifest.xml").use { doc ->
            val manifest = doc.documentElement
            val nodes = manifest.getElementsByTagName("uses-permission")
            val targets = (0 until nodes.length).map { nodes.item(it) as Element }
                .filter { it.getAttribute("android:name") in TRACKING_PERMISSIONS }
            val missing = TRACKING_PERMISSIONS - targets.map { it.getAttribute("android:name") }.toSet()
            if (missing.isNotEmpty()) throw PatchException("Kick: tracking permissions missing: $missing")
            targets.forEach { manifest.removeChild(it) }
        }
    }
}
