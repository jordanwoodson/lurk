package io.github.jordanwoodson.patches.kick.emotes

import app.morphe.patcher.Fingerprint

// React Native's OkHttp client provider keeps its library name.

internal object ClientBuilderFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/react/modules/network/OkHttpClientProvider;",
    name = "createClientBuilder", returnType = "Lokhttp3/OkHttpClient\$Builder;", parameters = emptyList(),
)
