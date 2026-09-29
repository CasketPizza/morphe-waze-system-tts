package app.waze.systemtts.patches.waze

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

object TtsDownloadFingerprint : Fingerprint(
    definingClass = "Lcom/waze/sound/TtsNativeManager;",
    name = "downloadTtsFromVoiceServer",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "L", "Ljava/lang/String;")
)

object TtsPlayFingerprint : Fingerprint(
    definingClass = "Lcom/waze/sound/TtsNativeManager;",
    name = "play",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Z")
)

object LegacyTtsPlayFingerprint : Fingerprint(
    definingClass = "Lcom/waze/sound/WazeTtsPlayerNativeManager;",
    name = "play",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Z")
)
