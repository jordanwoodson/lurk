package io.github.jordanwoodson.patches.kick.chat

import app.morphe.patcher.Fingerprint

// react-native-webview keeps its library names. Kick's chat is a page in one of its WebViews.

internal object PageStartedFingerprint : Fingerprint(
    definingClass = "Lcom/reactnativecommunity/webview/RNCWebViewClient;",
    name = "onPageStarted", returnType = "V",
    parameters = listOf("Landroid/webkit/WebView;", "Ljava/lang/String;", "Landroid/graphics/Bitmap;"),
)

internal object PageFinishedFingerprint : Fingerprint(
    definingClass = "Lcom/reactnativecommunity/webview/RNCWebViewClient;",
    name = "onPageFinished", returnType = "V",
    parameters = listOf("Landroid/webkit/WebView;", "Ljava/lang/String;"),
)
