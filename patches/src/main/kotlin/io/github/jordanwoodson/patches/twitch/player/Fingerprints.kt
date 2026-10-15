package io.github.jordanwoodson.patches.twitch.player

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.anyInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private const val IVS_PLAYER = "Lcom/amazonaws/ivs/player/CorePlayerImpl;"
private const val DRAGGABLE = "Ltv/twitch/android/shared/ui/elements/draggable/"

// IVS's only MediaPlayer, used by every native, portal and React Native player. The SDK keeps its names.
internal object IvsPlayFingerprint : Fingerprint(
    definingClass = IVS_PLAYER, name = "play", returnType = "V", parameters = emptyList(),
)

internal object IvsPauseFingerprint : Fingerprint(
    definingClass = IVS_PLAYER, name = "pause", returnType = "V", parameters = emptyList(),
)

internal object IvsReleaseFingerprint : Fingerprint(
    definingClass = IVS_PLAYER, name = "release", returnType = "V", parameters = emptyList(),
)

// The base class of the native player presenters (live, VOD, clips, vertical): its property delegates
// keep their Kotlin names.
internal object PlayerPresenterFingerprint : Fingerprint(
    name = "<clinit>", strings = listOf("playerStateAndEventDisposable", "followStatusDisposable"),
)

// The player error screen's Retry button: its last parameter is the action that retries the player.
internal object PlayerErrorRetryFingerprint : Fingerprint(
    returnType = "V", strings = listOf("At least one non-null argument is required"),
)

// The classic VOD and clip player overlay (player_control_overlay_widget) finding its settings button.
internal object VodPlayerOverlayFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    filters = listOf(
        resourceLiteral(ResourceType.ID, "share_button"),
        resourceLiteral(ResourceType.ID, "settings_button"),
        resourceLiteral(ResourceType.ID, "play_pause_fast_seek_view"),
    ),
)

// The classic live theatre inflating its player overlay (player_control_overlay) and finding its buttons.
internal object LivePlayerOverlayFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    filters = listOf(
        resourceLiteral(ResourceType.LAYOUT, "player_control_overlay"),
        resourceLiteral(ResourceType.ID, "settings_button"),
        resourceLiteral(ResourceType.ID, "share_button"),
    ),
)

// The native player's gesture listener: Twitch emits a long press with its position, then ignores it.
internal object PlayerLongPressFingerprint : Fingerprint(
    name = "onLongPress", returnType = "V", parameters = listOf("Landroid/view/MotionEvent;"),
    filters = listOf(
        methodCall(definingClass = "Landroid/view/MotionEvent;", name = "getX", parameters = emptyList()),
        methodCall(definingClass = "Landroid/view/MotionEvent;", name = "getY", parameters = emptyList()),
    ),
    custom = { _, classDef -> classDef.superclass == "Landroid/view/GestureDetector\$SimpleOnGestureListener;" },
)

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
