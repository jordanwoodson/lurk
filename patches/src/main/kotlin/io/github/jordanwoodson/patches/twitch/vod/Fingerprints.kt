package io.github.jordanwoodson.patches.twitch.vod

import app.morphe.patcher.Fingerprint

// The native VOD player presenter: it alone throws this message from its unused live-only method. The
// class loads a VodModel and implements Twitch's VOD seek interface.
internal object VodPresenterFingerprint : Fingerprint(
    strings = listOf("Should not be called from VodPlayerPresenter"),
)
