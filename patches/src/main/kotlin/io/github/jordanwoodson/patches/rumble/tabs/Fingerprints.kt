package io.github.jordanwoodson.patches.rumble.tabs

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

// The rumble_prefs helper's getter of the tab Rumble opens on (defaultTabIndexKey: 0 Home, 3 My Feed).
internal object StartupTabFingerprint : Fingerprint(
    returnType = "I",
    parameters = emptyList(),
    custom = { method, classDef ->
        method.implementation?.instructions?.any {
            it is ReferenceInstruction && it.reference.toString() == "Landroid/content/SharedPreferences;->getInt(Ljava/lang/String;I)I"
        } == true && classDef.methods.any { constructor ->
            constructor.name == "<init>" && constructor.implementation?.instructions?.mapNotNull {
                if (it.opcode == Opcode.CONST_STRING) ((it as ReferenceInstruction).reference as StringReference).string else null
            }?.containsAll(listOf("rumble_prefs", "defaultTabIndexKey")) == true
        }
    },
)
