package io.github.jordanwoodson.patches.twitch.chat

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

private const val EVENT_DISPATCHER = "Ltv/twitch/android/core/mvp/viewdelegate/EventDispatcher;"

// Chat timestamps.

// The chat line builder's class: its constructor makes the "H:mm" time formatter (the only one in the app).
internal object ChatLineFormatterFingerprint : Fingerprint(
    name = "<init>", returnType = "V", filters = listOf(string("H:mm")),
)

// Turns a chat message into a list item. Its fourth parameter shows the time: true for chat history only.
internal object ChatLineItemFingerprint : Fingerprint(
    returnType = "L", parameters = listOf("L", "Z", "L", "Z", "L", EVENT_DISPATCHER, EVENT_DISPATCHER),
    strings = listOf("client-nonce", "gifs"),
)

// Show deleted messages.

// Builds a chat line's spans. A message that is already deleted gets the "<message deleted>" placeholder.
internal object DeletedMessageSpansFingerprint : Fingerprint(
    classFingerprint = ChatLineFormatterFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    filters = listOf(resourceLiteral(ResourceType.STRING, "chat_message_deleted"), string("<"), string(">")),
)

// Re-renders a shown line when its message is deleted later: keeps the name, replaces the message.
internal object DeletedMessageLineFingerprint : Fingerprint(
    returnType = "Landroid/text/Spanned;",
    parameters = listOf("Ljava/lang/String;", "Landroid/text/SpannedString;", "Landroid/content/Context;", EVENT_DISPATCHER, "Z"),
    filters = listOf(resourceLiteral(ResourceType.STRING, "chat_message_deleted")),
)

// The IRC parser: one IRC line to a chat event. CLEARCHAT without a target user clears the whole chat.
internal object IrcEventParserFingerprint : Fingerprint(
    returnType = "L", parameters = listOf("L", "L", "L", "L", "Z"),
    strings = listOf("target-user-id", "target-msg-id", " but no target-msg-id in tags found"),
)
