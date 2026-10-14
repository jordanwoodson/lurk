package io.github.jordanwoodson.patches.twitch.playback

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object AutoplayPreferenceFingerprint : Fingerprint(
    parameters = emptyList(), returnType = "L", strings = listOf("autoplay_enabled", ""),
)

// R8 shares this Function2 class with other lambdas. Only its autoplay preference case is changed.
internal object AutoplayObserverFingerprint : Fingerprint(
    parameters = listOf("Ljava/lang/Object;", "Ljava/lang/Object;"),
    returnType = "Ljava/lang/Object;", strings = listOf("autoplay_enabled", ""),
)

internal object QualityPreferencesFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V", strings = listOf("should_video_auto_quality_downgrade", "last_used_video_quality", "auto"),
)

// Stable React Native library boundary. Exactly one pushNull produces an absent-key result row.
internal object MissingAsyncStorageValueFingerprint : Fingerprint(
    returnType = "V", strings = listOf("catalystLocalStorage", "key", "value"),
    custom = { method, classDef ->
        classDef.type.startsWith("Lcom/reactnativecommunity/asyncstorage/AsyncStorageModule$") &&
            method.name == "doInBackgroundGuarded"
    },
)

internal object RecommendationAutoplayFingerprint : Fingerprint(
    parameters = listOf("Ljava/lang/Object;"), returnType = "Ljava/lang/Object;",
    strings = listOf("player_rec_autoplay", "player_rec_select"),
)

internal object RecommendationCountdownFingerprint : Fingerprint(
    parameters = listOf("L"), returnType = "V",
    strings = listOf("progress", "Recommended model: ", " cannot be bound"),
)
