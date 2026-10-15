package io.github.jordanwoodson.patches.kick.icon

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import io.github.jordanwoodson.patches.kick.shared.Constants.COMPATIBILITY_KICK
import org.w3c.dom.Element

@Suppress("unused")
val lurkIconPatch = resourcePatch(
    name = "Lurk icon",
    description = "Replaces Kick's launcher icon with Lurk artwork, including Android's themed icon.",
) {
    compatibleWith(COMPATIBILITY_KICK)
    execute {
        // Kick requires Android 10, so every supported device understands adaptive vector icons.
        val loader = object {}.javaClass.classLoader
        val foreground = loader.getResourceAsStream("rumble/icon/drawable/ic_launcher_foreground.xml")
            ?.bufferedReader()?.use { it.readText() }
            ?.takeIf { "#89DA44" in it }
            ?: throw PatchException("Bundled Lurk artwork missing, or its logo is no longer #89DA44")
        get("res/drawable/lurk_launcher_foreground.xml").apply {
            parentFile.mkdirs()
            writeText(foreground.replace("#89DA44", "#53FC18"))
        }
        get("res/drawable/lurk_launcher_background.xml").writeText("""
            <shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
                <solid android:color="#181D24" />
            </shape>
        """.trimIndent())
        get("res/mipmap-anydpi-v26/lurk_launcher.xml").apply {
            parentFile.mkdirs()
            writeText("""
                <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
                    <background android:drawable="@drawable/lurk_launcher_background" />
                    <foreground android:drawable="@drawable/lurk_launcher_foreground" />
                    <monochrome android:drawable="@drawable/lurk_launcher_foreground" />
                </adaptive-icon>
            """.trimIndent())
        }
        document("AndroidManifest.xml").use { doc ->
            val application = doc.getElementsByTagName("application").item(0) as Element
            application.setAttribute("android:icon", "@mipmap/lurk_launcher")
            application.setAttribute("android:roundIcon", "@mipmap/lurk_launcher")
        }
    }
}
