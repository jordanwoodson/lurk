package io.github.jordanwoodson.patches.twitch.notifications

import app.morphe.patcher.Fingerprint

// Firebase Installations' REST client builds every request here and signs it with the app's certificate.
internal object FirebaseInstallationsRequestFingerprint : Fingerprint(
    returnType = "Ljava/net/HttpURLConnection;",
    parameters = listOf("Ljava/net/URL;", "Ljava/lang/String;"),
    strings = listOf("X-Android-Package", "X-Android-Cert", "x-goog-api-key"),
)
