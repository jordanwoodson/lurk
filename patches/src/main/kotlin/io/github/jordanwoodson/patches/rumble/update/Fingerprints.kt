package io.github.jordanwoodson.patches.rumble.update

import app.morphe.patcher.Fingerprint

// ShouldForceNewAppVersionUseCase(versionName): whether Remote Config's forced_app_version is newer than
// this build, which shows the "update required" screen.
internal object ForceUpdateFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;"),
    strings = listOf("forced_app_version", "ShouldForceNewAppVersionUseCase"),
)

// ShouldSuggestNewAppVersionUseCase(versionName, continuation): whether to show the "new version
// available" dialog (suggested_app_version, at most once a day).
internal object SuggestUpdateFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/String;", "L"),
    strings = listOf("suggested_app_version", "ShouldSuggestNewAppVersionUseCase"),
)
