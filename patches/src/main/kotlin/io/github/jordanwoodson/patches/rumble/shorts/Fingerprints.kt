package io.github.jordanwoodson.patches.rumble.shorts

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode
import io.github.jordanwoodson.patches.rumble.shared.composableFingerprint

// The bottom bar's content lambda, which loops over the tab list and draws one item per tab.
internal object BottomNavigationBarFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        string("\$this\$RumbleBottomNavigation"),
        string("com.rumble.battles.navigation.BottomNavigationBar.<anonymous> (", StringComparisonType.STARTS_WITH),
    ),
)

// The horizontal Shorts row (title, "View all" and cards) on Home, My Feed, channels, search and
// My Videos.
internal val ShortsListViewFingerprint =
    composableFingerprint("com.rumble.battles.discover.presentation.views.ShortsListView")

// HomeSectionKind.fromValue(kind): the type of each Home section the server sends. Gson DTOs keep their names.
internal object HomeSectionKindFingerprint : Fingerprint(
    definingClass = "Lcom/rumble/network/dto/home/HomeSectionKind\$Companion;",
    returnType = "Lcom/rumble/network/dto/home/HomeSectionKind;",
    parameters = listOf("Ljava/lang/String;"),
)

// Creator.tabs: the channel page tabs ("all", "shorts", "live", ...).
internal object CreatorTabsFingerprint : Fingerprint(
    definingClass = "Lcom/rumble/network/dto/creator/Creator;",
    returnType = "Ljava/util/List;",
    parameters = emptyList(),
    filters = listOf(fieldAccess(definingClass = "this", name = "tabs", opcode = Opcode.IGET_OBJECT)),
)
