package io.github.jordanwoodson.patches.twitch.account

import app.morphe.patcher.Fingerprint

// The Eppo "loggedOutExperiment": its variant names live in the static initializer of the class whose
// boolean getter decides between the main UI and the login screen for a signed-out user.
internal object LoggedOutExperimentFingerprint : Fingerprint(
    name = "<clinit>",
    returnType = "V",
    strings = listOf("variant1", "variant7", "no_nudges"),
)
