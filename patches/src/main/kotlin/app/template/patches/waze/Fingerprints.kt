package app.waze.systemtts.patches.waze

import app.morphe.patcher.Fingerprint

object ChunkConstructorFingerprint : Fingerprint(
    definingClass = "Lcom/waze/sound/ai;", name = "<init>",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;")
)
object PlayerConstructorFingerprint : Fingerprint(
    definingClass = "Lcom/waze/sound/fb;", name = "<init>"
)
object UrlPlayFingerprint : Fingerprint(
    definingClass = "Lcom/waze/sound/fb;", name = "g", returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Lh/g/a/a;")
)
object StopPlayerFingerprint : Fingerprint(
    definingClass = "Lcom/waze/sound/fb;", name = "e", returnType = "V", parameters = emptyList()
)
object SettingsRowsFingerprint : Fingerprint(
    definingClass = "Lcom/waze/settings/dc;", name = "x",
    returnType = "V", parameters = emptyList()
)
