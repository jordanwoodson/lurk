package io.github.jordanwoodson.patches.kick.appname

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import io.github.jordanwoodson.patches.kick.shared.Constants.COMPATIBILITY_KICK
import org.w3c.dom.Element

@Suppress("unused")
val changeAppNamePatch = resourcePatch(
    name = "Change app name",
    description = "Changes Kick's name in the launcher and Android settings.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_KICK)
    val appName by stringOption(
        key = "appName", default = "Kick Lurk", title = "App name",
        description = "The name to show.", required = true,
    ) { !it.isNullOrBlank() }
    execute {
        document("res/values/strings.xml").use { doc ->
            val strings = doc.getElementsByTagName("string")
            val label = (0 until strings.length).map { strings.item(it) as Element }
                .singleOrNull { it.getAttribute("name") == "app_name" }
                ?: throw PatchException("Kick: expected one app_name resource")
            label.textContent = appName!!.trim().replace("\\", "\\\\").replace("'", "\\'")
                .replace("\"", "\\\"").replace(Regex("^([@?])"), "\\\\$1")
        }
    }
}
