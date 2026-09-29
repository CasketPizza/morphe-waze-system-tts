package app.waze.systemtts.patches.waze

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.waze.systemtts.patches.shared.Constants.COMPATIBILITY_WAZE
import com.android.tools.smali.dexlib2.Opcode

private const val BRIDGE = "Lapp/waze/systemtts/extension/SystemTtsBridge;"

@Suppress("unused")
val systemTtsPatch = bytecodePatch(
    name = "Use Android system TTS for navigation",
    description = "Adds a system TTS control to Settings and replaces online navigation chunks using Android's default engine.",
    default = true
) {
    compatibleWith(COMPATIBILITY_WAZE)
    extendWith("extensions/extension.mpe")
    execute {
        listOf(
            ChunkConstructorFingerprint.method to "invoke-static {p1, p2, p3}, $BRIDGE->remember(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"
        ).forEach { (method, code) ->
            method.implementation!!.instructions.toList().indices.reversed().forEach { index ->
                if (method.implementation!!.instructions[index].opcode == Opcode.RETURN_VOID) {
                    method.addInstructions(index, code)
                }
            }
        }
        // These parameters are reused as other types later in the original methods.
        PlayerConstructorFingerprint.method.addInstructions(0,
            "invoke-static/range {p1 .. p1}, $BRIDGE->initialize(Landroid/content/Context;)V")
        SettingsCreateFingerprint.method.addInstructions(0,
            "invoke-static/range {p0 .. p0}, $BRIDGE->addSettings(Landroid/app/Activity;)V")
        UrlPlayFingerprint.method.addInstructions(0, """
            invoke-static {p0, p1, p2, p3}, $BRIDGE->play(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)Z
            move-result v0
            if-eqz v0, :original
            return-void
            :original
            nop
        """)
        StopPlayerFingerprint.method.addInstructions(0, "invoke-static {}, $BRIDGE->cancel()V")
    }
}
