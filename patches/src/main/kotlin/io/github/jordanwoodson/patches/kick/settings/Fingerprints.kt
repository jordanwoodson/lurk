package io.github.jordanwoodson.patches.kick.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import io.github.jordanwoodson.patches.shared.settings.SETTINGS_CLASS

internal object ApplicationOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/kick/mobile/MainApplication;",
    name = "onCreate", returnType = "V", parameters = emptyList(),
)

internal object RegistryFingerprint : Fingerprint(
    definingClass = SETTINGS_CLASS, name = "registryJson",
    returnType = "Ljava/lang/String;", parameters = emptyList(),
)

internal object BundleLoaderFingerprint : Fingerprint(
    name = "loadScriptFromAssets", returnType = "V",
    parameters = listOf("Landroid/content/res/AssetManager;", "Ljava/lang/String;", "Z"),
    filters = listOf(string("assetManager"), string("assetURL"), methodCall(
        definingClass = "Lcom/facebook/react/runtime/ReactInstance;",
        name = "access\$loadJSBundleFromAssets",
    )),
)
