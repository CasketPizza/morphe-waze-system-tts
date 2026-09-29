package app.waze.systemtts.patches.waze

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.waze.systemtts.patches.shared.Constants.COMPATIBILITY_WAZE

private const val BRIDGE = "Lapp/waze/systemtts/extension/SystemTtsBridge;"

@Suppress("unused")
val systemTtsPatch = bytecodePatch(
    name = "Use Android system TTS for navigation",
    description = "Makes Waze navigation use the Android default Text-to-Speech engine, including street names, when TTS navigation is selected.",
    default = true
) {
    compatibleWith(COMPATIBILITY_WAZE)
    extendWith("extensions/extension.mpe")

    execute {
        TtsDownloadFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p1, p3}, $BRIDGE->remember(Ljava/lang/String;Ljava/lang/String;)V
                const/4 v0, 0x1
                invoke-virtual {p0, p3, v0}, Lcom/waze/sound/ia;->onTtsDownloadComplete(Ljava/lang/String;Z)V
                return-void
            """
        )
        TtsPlayFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p1}, $BRIDGE->speak(Ljava/lang/String;)V
                return-void
            """
        )
        LegacyTtsPlayFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p1}, $BRIDGE->speak(Ljava/lang/String;)V
                return-void
            """
        )
    }
}
