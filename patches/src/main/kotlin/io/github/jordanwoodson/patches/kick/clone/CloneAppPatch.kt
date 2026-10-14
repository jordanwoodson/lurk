package io.github.jordanwoodson.patches.kick.clone

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import io.github.jordanwoodson.patches.kick.shared.Constants.COMPATIBILITY_KICK
import org.w3c.dom.Element

private const val ORIGINAL = "com.kick.mobile"
private val PACKAGE_NAME = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")

@Suppress("unused")
val cloneAppPatch = resourcePatch(
    name = "Clone app",
    description = "Installs Kick under another package name alongside the original, with separate sign-in " +
        "and settings. Sharing sign-in with Kick's broadcaster app may not work after re-signing.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_KICK)
    val packageName by stringOption(
        key = "packageName", default = "$ORIGINAL.lurk", title = "Package name",
        description = "A unique package name for this install.", required = true,
    ) { it != null && it != ORIGINAL && PACKAGE_NAME.matches(it) }

    fun rename(value: String) = when {
        value == ORIGINAL -> packageName!!
        value.startsWith("$ORIGINAL.") -> packageName!! + value.removePrefix(ORIGINAL)
        else -> value
    }

    finalize {
        document("AndroidManifest.xml").use { doc ->
            val manifest = doc.documentElement
            if (manifest.getAttribute("package") != ORIGINAL) throw PatchException("Kick: unexpected package")
            val nodes = manifest.getElementsByTagName("*")
            val elements = (0 until nodes.length).map { nodes.item(it) as Element }
            val declared = elements.filter { it.tagName == "permission" }
                .map { it.getAttribute("android:name") }.toSet()
            val components = setOf("application", "activity", "activity-alias", "service", "receiver", "provider")
            elements.forEach { element ->
                // Relative class names still resolve to the original compiled code after changing the app ID.
                if (element.tagName in components) {
                    listOf("android:name", "android:targetActivity", "android:parentActivityName").forEach { attribute ->
                        val value = element.getAttribute(attribute)
                        if (value.startsWith('.')) element.setAttribute(attribute, ORIGINAL + value)
                        else if (value.isNotEmpty() && '.' !in value) element.setAttribute(attribute, "$ORIGINAL.$value")
                    }
                }
                listOf("android:name", "android:permission", "android:readPermission", "android:writePermission").forEach { attribute ->
                    val value = element.getAttribute(attribute)
                    if (value in declared) element.setAttribute(attribute, rename(value))
                }
                if (element.tagName == "provider") {
                    element.setAttribute("android:authorities", element.getAttribute("android:authorities")
                        .split(';').joinToString(";", transform = ::rename))
                }
                if (element.tagName == "category") {
                    element.setAttribute("android:name", rename(element.getAttribute("android:name")))
                }
                if (element.hasAttribute("android:taskAffinity")) {
                    element.setAttribute("android:taskAffinity", rename(element.getAttribute("android:taskAffinity")))
                }
                if (element.tagName == "data" && element.getAttribute("android:scheme") == ORIGINAL) {
                    element.setAttribute("android:scheme", packageName!!)
                }
            }
            manifest.setAttribute("package", packageName!!)
        }
    }
}
