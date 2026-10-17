package io.github.jordanwoodson.patches.rumble.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.string
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import io.github.jordanwoodson.patches.shared.settings.isEnabled

// The bottom bar's content lambda, which loops over the tab list and draws one item per tab.
internal object BottomNavigationBarFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        string("\$this\$RumbleBottomNavigation"),
        string("com.rumble.battles.navigation.BottomNavigationBar.<anonymous> (", StringComparisonType.STARTS_WITH),
    ),
)

/**
 * Stops the bottom bar drawing the tab whose route is the Kotlin object [routeObjectName] while the
 * switch [key] is on. Other code refers to tabs by their position in the bar's fixed list, so the entry
 * stays in the list; the loop skips drawing it but still counts it.
 */
internal fun BytecodePatchContext.hideBottomTab(routeObjectName: String, key: String) {
    val route = kotlinObject(routeObjectName).substringBefore("->")
    val routeType = classDefBy(route).superclass
        ?: throw PatchException("Rumble: $routeObjectName route has no superclass")

    BottomNavigationBarFingerprint.single("bottom navigation bar").method.apply {
        val instructions = implementation!!.instructions.toList()
        fun calls(index: Int, method: String) = instructions[index].let {
            it is ReferenceInstruction && it.reference.toString() == method
        }
        val next = instructions.indices.single { calls(it, "Ljava/util/Iterator;->next()Ljava/lang/Object;") }
        val loop = (next downTo 0).first { calls(it, "Ljava/util/Iterator;->hasNext()Z") }
        // next() goes to a register that is dead once copied into the tab variable.
        val scratch = (instructions[next + 1] as OneRegisterInstruction).registerA
        val increment = (next until instructions.size).first { instructions[it].opcode == Opcode.ADD_INT_LIT8 }
        val (nextIndex, index) = (instructions[increment] as TwoRegisterInstruction).let { it.registerA to it.registerB }
        val tabCast = (next until instructions.size).first { instructions[it].opcode == Opcode.CHECK_CAST }
        val tab = (instructions[tabCast] as OneRegisterInstruction).registerA
        val tabType = ((instructions[tabCast] as ReferenceInstruction).reference as TypeReference).type
        val routeGetter = classDefBy(tabType).methods.single {
            it.parameterTypes.isEmpty() && it.returnType == routeType
        }
        if (scratch > 15 || tab > 15) throw PatchException("Rumble: unexpected registers in the bottom bar loop")

        addInstructionsWithLabels(
            tabCast + 1,
            """
                ${isEnabled(key, "v$scratch")}
                if-eqz v$scratch, :keep
                invoke-virtual { v$tab }, $tabType->${routeGetter.name}()$routeType
                move-result-object v$scratch
                instance-of v$scratch, v$scratch, $route
                if-eqz v$scratch, :keep
                move/from16 v$index, v$nextIndex
                goto/16 :loop
            """,
            ExternalLabel("keep", getInstruction(tabCast + 1)),
            ExternalLabel("loop", getInstruction(loop)),
        )
    }
}
