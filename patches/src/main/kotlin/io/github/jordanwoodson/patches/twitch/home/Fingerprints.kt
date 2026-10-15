package io.github.jordanwoodson.patches.twitch.home

import app.morphe.patcher.Fingerprint

// Native Home reads its "ShowStoriesShelf" argument while injecting the Home presenter.
internal object HomeStoriesArgumentFingerprint : Fingerprint(
    definingClass = "Ltv/twitch/android/feature/discovery/feed/tab/Hilt_DiscoveryFeedHomeFragment;",
    strings = listOf("ShowStoriesShelf"),
)

// The Following page renders its sections (live, recommended, continue watching, offline...) in order.
internal object FollowingSectionsFingerprint : Fingerprint(
    strings = listOf("Unsupported section in Following page "),
)

internal object ResumeWatchingSectionFingerprint : Fingerprint(
    name = "toString", returnType = "Ljava/lang/String;", parameters = emptyList(),
    strings = listOf("ResumeWatching(vods="),
)
