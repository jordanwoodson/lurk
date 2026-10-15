package io.github.jordanwoodson.patches.twitch.chat

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

private const val EVENT_DISPATCHER = "Ltv/twitch/android/core/mvp/viewdelegate/EventDispatcher;"
private const val RESTRICTIONS = "Ltv/twitch/android/shared/messageinput"

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

// Anonymous chat.

// The IRC login: the nickname and OAuth token the chat client sends after connecting.
internal object IrcCredentialsFingerprint : Fingerprint(
    name = "toString", returnType = "Ljava/lang/String;", parameters = emptyList(),
    strings = listOf("UserCredentials(nickname=", ", oauthToken="),
)

// Sends CAP, PASS and NICK once the chat WebSocket connects. A null token is sent as "PASS listen".
internal object IrcLoginFingerprint : Fingerprint(
    name = "emit", returnType = "Ljava/lang/Object;", parameters = listOf("Ljava/lang/Object;", "L"),
    strings = listOf("Enqueueing IRC auth flow commands (CAP, NICK, PASS)", "listen", "oauth:", "PASS ", "NICK "),
)

// Picks the restriction shown in place of the chat input and checked before a message is sent.
internal object ChatRestrictionFingerprint : Fingerprint(
    returnType = "$RESTRICTIONS/pub/chatrestrictions/ChatRestrictionsDisplayType;",
    parameters = listOf("$RESTRICTIONS/impl/chatrestrictions/ChatRestrictionsState;", "L", "Z"),
    filters = listOf(
        methodCall(name = "getForceHideRestrictionsBanner"),
        methodCall(name = "getBypassSendChatMessageRestrictions"),
        fieldAccess(definingClass = "$RESTRICTIONS/pub/chatrestrictions/ChatRestrictionsDisplayType\$LOGIN;", name = "INSTANCE"),
    ),
)

// The text above the native chat input for each restriction. The log-in one reads "Login to Chat".
internal object ChatRestrictionLabelFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        resourceLiteral(ResourceType.STRING, "chat_input_label_login"),
        methodCall(definingClass = "Landroid/content/Context;", name = "getString"),
        resourceLiteral(ResourceType.STRING, "chat_input_label_emote"),
    ),
)

// The GraphQL mutation every typed chat message (and /me) is sent with.
internal object SendChatMessageMutationFingerprint : Fingerprint(
    name = "toString", returnType = "Ljava/lang/String;", parameters = emptyList(),
    filters = listOf(string("SendChatMessageMutation(input=")),
)

/** The only method that builds the send mutation: (channel, message, nonce, reply parent, continuation). */
internal fun sendChatMessageFingerprint(mutation: String) = Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;", "L"),
    filters = listOf(newInstance(mutation)),
)
