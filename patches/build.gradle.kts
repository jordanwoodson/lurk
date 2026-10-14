group = "io.github.jordanwoodson"

patches {
    about {
        name = "Jordan's Morphe Patches"
        description = "Privacy-focused patches for Twitch, Kick, Rumble and other apps"
        source = "git@github.com:jordanwoodson/morphe.git"
        author = "jordanwoodson"
        contact = "https://github.com/jordanwoodson/morphe/issues"
        website = "https://github.com/jordanwoodson/morphe"
        license = "GPLv3"
    }
}

// Separate configuration so gson is available at runtime for the
// generatePatchesList task but never bundled into the APK.
val patchListGeneratorClasspath = configurations.create("patchListGeneratorClasspath")

dependencies {
    // Shared Morphe helpers: hermesPatch (Kick is React Native), string/hex replacement, BytecodeUtils.
    implementation(libs.morphe.patches.library)

    compileOnly(libs.gson)
    patchListGeneratorClasspath(libs.gson)
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("util.PatchListGeneratorKt")
    }

    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}
