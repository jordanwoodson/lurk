package io.github.jordanwoodson.patches.twitch.player

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import com.android.tools.smali.dexlib2.AccessFlags

// The Create Clip button's state, shared by the live overlay, the VOD/clip overlay and vertical theatre.
internal object ClipButtonStateFingerprint : Fingerprint(
    name = "toString", returnType = "Ljava/lang/String;", parameters = emptyList(),
    strings = listOf("ClipButtonUiState(isClipButtonVisible=", ", isClippingEnabled="),
)

// The live player overlay's options: share and clip.
internal object PlayerOverlayOptionsFingerprint : Fingerprint(
    name = "toString", returnType = "Ljava/lang/String;", parameters = emptyList(),
    strings = listOf("PlayerOverlayOptions(isInfoOptionVisible=", ", isClipOptionVisible="),
)

// AndroidX MediaRouter's cast button (library class, not renamed).
internal object CastButtonVisibilityFingerprint : Fingerprint(
    definingClass = "Landroidx/mediarouter/app/MediaRouteButton;",
    name = "setVisibility", returnType = "V", parameters = listOf("I"),
)

internal object CastButtonAttachedFingerprint : Fingerprint(
    definingClass = "Landroidx/mediarouter/app/MediaRouteButton;",
    name = "onAttachedToWindow", returnType = "V", parameters = emptyList(),
)

// The VOD/clip ("seekable") player overlay: its share button is only shown by the layout default.
internal object SeekableOverlayFingerprint : Fingerprint(
    name = "<init>", returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Landroid/view/View;", "L", "L", "L", "L"),
    filters = listOf(
        resourceLiteral(ResourceType.ID, "create_clip_button_compose_view"),
        resourceLiteral(ResourceType.ID, "share_button"),
        resourceLiteral(ResourceType.ID, "media_route_button"),
    ),
)

// Vertical theatre's Share action (Compose): (onClick, Composer, changed).
internal object VerticalShareActionFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V", parameters = listOf("L", "L", "I"),
    filters = listOf(
        resourceLiteral(ResourceType.STRING, "share"),
        resourceLiteral(ResourceType.DRAWABLE, "share_android"),
    ),
)
