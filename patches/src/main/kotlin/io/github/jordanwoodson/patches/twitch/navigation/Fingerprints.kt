package io.github.jordanwoodson.patches.twitch.navigation

import app.morphe.patcher.Fingerprint

// The router every launch and link goes through: it stores the destination in the intent for the main
// screen, or sends a signed-out user to the login screen. A plain launch asks for the Default destination.
internal object DestinationRouterFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("destinationOrdinal", "intent_router"),
    custom = { method, _ -> method.parameterTypes.size == 3 && method.parameterTypes[1].toString() == "Landroid/os/Bundle;" },
)
