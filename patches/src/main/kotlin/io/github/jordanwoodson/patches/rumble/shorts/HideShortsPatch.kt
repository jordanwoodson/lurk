package io.github.jordanwoodson.patches.rumble.shorts

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import io.github.jordanwoodson.patches.rumble.settings.rumbleSettings
import io.github.jordanwoodson.patches.rumble.settings.settingsPatch
import io.github.jordanwoodson.patches.rumble.shared.Constants.COMPATIBILITY_RUMBLE
import io.github.jordanwoodson.patches.rumble.shared.enumConstant
import io.github.jordanwoodson.patches.rumble.shared.enumWith
import io.github.jordanwoodson.patches.rumble.shared.kotlinObject
import io.github.jordanwoodson.patches.rumble.shared.single
import io.github.jordanwoodson.patches.shared.settings.Section
import io.github.jordanwoodson.patches.shared.settings.SwitchSetting
import io.github.jordanwoodson.patches.shared.settings.addInstructionsIfEnabled
import io.github.jordanwoodson.patches.shared.settings.isEnabled

private const val KEY = "hide_shorts"

@Suppress("unused")
val hideShortsPatch = bytecodePatch(
    name = "Hide Shorts",
    description = "Removes Shorts from the bottom bar, Home, My Feed, channel pages, categories and search. " +
        "Shorts links still open.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_RUMBLE)
    dependsOn(settingsPatch)

    execute {
        hideShortsTab()

        // The Shorts row, wherever it appears.
        ShortsListViewFingerprint.single("ShortsListView composable").method.addInstructionsIfEnabled(KEY, "return-void")

        // Home: treat a "shorts" section like one the server hides, so it is neither loaded nor drawn.
        HomeSectionKindFingerprint.single("HomeSectionKind parser").method.addInstructionsIfEnabled(
            KEY,
            """
                const-string v0, "shorts"
                invoke-virtual { v0, p1 }, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v0
                if-eqz v0, :lurk_off
                sget-object v0, Lcom/rumble/network/dto/home/HomeSectionKind;->HIDE:Lcom/rumble/network/dto/home/HomeSectionKind;
                return-object v0
            """,
        )

        // Channel pages: drop the "shorts" tab, unless it is the channel's only one. The getter has
        // two registers (the list and this), so this doubles as the size and the copy.
        CreatorTabsFingerprint.single("Creator.tabs getter").method.apply {
            val list = getInstruction<OneRegisterInstruction>(0).registerA
            addInstructionsWithLabels(
                1,
                """
                    if-eqz v$list, :done
                    ${isEnabled(KEY, "p0")}
                    if-eqz p0, :done
                    invoke-interface { v$list }, Ljava/util/List;->size()I
                    move-result p0
                    add-int/lit8 p0, p0, -0x1
                    if-lez p0, :done
                    new-instance p0, Ljava/util/ArrayList;
                    invoke-direct { p0, v$list }, Ljava/util/ArrayList;-><init>(Ljava/util/Collection;)V
                    const-string v$list, "shorts"
                    invoke-virtual { p0, v$list }, Ljava/util/ArrayList;->remove(Ljava/lang/Object;)Z
                    move-object v$list, p0
                """,
                ExternalLabel("done", getInstruction(1)),
            )
        }

        hideCategoryShortsTab()
        hideSearchShortsFilter()

        rumbleSettings.add(
            SwitchSetting(
                KEY,
                "Hide Shorts",
                "The Shorts tab and rows, and Shorts on channels, categories and search.",
                Section.INTERFACE,
                restart = true,
            ),
        )
    }
}

// The bottom bar draws every tab in a fixed list, and other code refers to tabs by their position in it.
// So the Shorts entry stays in the list; the loop skips drawing it but still counts it.
private fun BytecodePatchContext.hideShortsTab() {
    val shortsRoute = kotlinObject("ShortsScreen").substringBefore("->")
    val routeType = classDefBy(shortsRoute).superclass
        ?: throw PatchException("Rumble: ShortsScreen route has no superclass")

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
                ${isEnabled(KEY, "v$scratch")}
                if-eqz v$scratch, :keep
                invoke-virtual { v$tab }, $tabType->${routeGetter.name}()$routeType
                move-result-object v$scratch
                instance-of v$scratch, v$scratch, $shortsRoute
                if-eqz v$scratch, :keep
                move/from16 v$index, v$nextIndex
                goto/16 :loop
            """,
            ExternalLabel("keep", getInstruction(tabCast + 1)),
            ExternalLabel("loop", getInstruction(loop)),
        )
    }
}

// Category pages build their tabs (live, videos, ..., shorts) into a mutable list.
private fun BytecodePatchContext.hideCategoryShortsTab() {
    val contentType = enumWith("SHORTS", "REPOSTS", "CATEGORIES", "LIVE_STREAM")
    val shorts = contentType.enumConstant("SHORTS")

    val builders = mutableListOf<Pair<ClassDef, String>>()
    classDefForEach { classDef ->
        classDef.methods.filter { method ->
            method.parameterTypes.map { it.toString() } == listOf("Z") && method.returnType == "Ljava/util/List;" &&
                method.implementation?.instructions?.any {
                    it.opcode == Opcode.FILLED_NEW_ARRAY &&
                        ((it as ReferenceInstruction).reference as TypeReference).type == "[${contentType.type}"
                } == true
        }.forEach { builders += classDef to it.name }
    }
    val (builderClass, builderName) = builders.singleOrNull()
        ?: throw PatchException("Rumble: expected one category tab builder, found ${builders.size}")

    mutableClassDefBy(builderClass).methods.single { it.name == builderName && it.parameterTypes.size == 1 }.apply {
        val instructions = implementation!!.instructions.toList()
        val array = instructions.indexOfFirst { it.opcode == Opcode.FILLED_NEW_ARRAY }
        val arrayRegisters = (instructions[array] as FiveRegisterInstruction).let {
            listOf(it.registerC, it.registerD, it.registerE, it.registerF, it.registerG).take(it.registerCount)
        }
        val toList = (array + 1 until instructions.size).first { instructions[it].opcode == Opcode.INVOKE_STATIC }
        val list = (instructions[toList + 1] as OneRegisterInstruction).registerA
        // The array's other registers are free once it is built.
        val scratch = arrayRegisters.first { it != list }
        addInstructionsWithLabels(
            toList + 2,
            """
                ${isEnabled(KEY, "v$scratch")}
                if-eqz v$scratch, :keep
                sget-object v$scratch, $shorts
                invoke-interface { v$list, v$scratch }, Ljava/util/List;->remove(Ljava/lang/Object;)Z
            """,
            ExternalLabel("keep", getInstruction(toList + 2)),
        )
    }
}

// Search's filter chips (All, Videos, Shorts, Channels) are the filter enum's entries.
private fun BytecodePatchContext.hideSearchShortsFilter() {
    val filter = enumWith("ALL", "VIDEOS", "SHORTS", "CHANNELS")
    val shorts = filter.enumConstant("SHORTS")
    val entries = filter.methods.single {
        AccessFlags.STATIC.isSet(it.accessFlags) && it.parameterTypes.isEmpty() &&
            it.returnType != filter.type && it.returnType != "[${filter.type}" && it.returnType.startsWith("L")
    }
    val entriesCall = "${filter.type}->${entries.name}()${entries.returnType}"

    Fingerprint(filters = listOf(methodCall(smali = entriesCall))).single("search filter chips").method.apply {
        val instructions = implementation!!.instructions.toList()
        val call = instructions.indexOfFirst { it is ReferenceInstruction && it.reference.toString() == entriesCall }
        val list = (instructions[call + 1] as OneRegisterInstruction).registerA
        // The register the next instruction overwrites is free here.
        val following: Instruction = instructions[call + 2]
        if (following.opcode != Opcode.SGET_OBJECT) throw PatchException("Rumble: unexpected search chip code")
        val scratch = (following as OneRegisterInstruction).registerA
        if (list > 15 || scratch > 15) throw PatchException("Rumble: unexpected registers in the search chips")
        addInstructionsWithLabels(
            call + 2,
            """
                ${isEnabled(KEY, "v$scratch")}
                if-eqz v$scratch, :keep
                new-instance v$scratch, Ljava/util/ArrayList;
                invoke-direct { v$scratch, v$list }, Ljava/util/ArrayList;-><init>(Ljava/util/Collection;)V
                sget-object v$list, $shorts
                invoke-virtual { v$scratch, v$list }, Ljava/util/ArrayList;->remove(Ljava/lang/Object;)Z
                move-object v$list, v$scratch
            """,
            ExternalLabel("keep", getInstruction(call + 2)),
        )
    }
}
