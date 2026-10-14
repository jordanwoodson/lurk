package io.github.jordanwoodson.patches.shared

/**
 * Extension bundles built from extensions/<module>. Pass these to extendWith() in a patch.
 * Add a per-app module (extensions/<app>) when an app needs its own Java code.
 */
internal const val SHARED_EXTENSION = "extensions/shared.mpe"

/** Smali descriptor of extensions/shared Logger, for tracing hooks from injected smali. */
internal const val LOGGER_CLASS = "Lio/github/jordanwoodson/extension/shared/Logger;"
