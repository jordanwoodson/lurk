package io.github.jordanwoodson.patches.rumble.ads

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// The video ad use case. Its constructor takes Rumble's ad repository (an interface) first and checks
// its parameters by name; the repository's single implementation is what the patch changes.
internal object VideoAdUseCaseFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    strings = listOf("rumbleAdRepository", "getCustomAdTag", "createPreRollVideoIdUseCase"),
)
