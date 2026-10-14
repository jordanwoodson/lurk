extension {
    name = "extensions/shared.mpe"
}

android {
    namespace = "io.github.jordanwoodson.extension.shared"
}

// AGP's built-in Kotlin adds kotlin-stdlib to every module. The extension is plain Java, and the patcher
// would merge the whole library into the patched app, which has its own Kotlin. Keep it out of the dex.
configurations.configureEach {
    if (name.endsWith("RuntimeClasspath")) {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.jetbrains", module = "annotations")
    }
}
