import app.waze.systemtts.extension.PromptTextCatalog;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipFile;

/** Run against the user's original base.apk; no Waze assets are redistributed. */
public final class PromptTextCatalogTest {
    public static void main(String[] args) throws Exception {
        try (ZipFile apk = new ZipFile(args[0]);
             InputStreamReader reader = new InputStreamReader(
                     apk.getInputStream(apk.getEntry("assets/res/key_value_tts_strings.txt")), StandardCharsets.UTF_8)) {
            PromptTextCatalog catalog = new PromptTextCatalog(reader);
            check(catalog.resolve("/sound/eng/ApproachPermanentHazardSpeedBump.mp3"), "Speed bumps ahead");
            check(catalog.resolve("TTS_APPTEXT_APPROACH_PERMANENT_HAZARD_SPEED_BUMP"), "Speed bumps ahead");
            check(catalog.resolve("/sound/eng/ApproachPermanentHazardSchoolZone.mp3"), "School zone ahead");
            check(catalog.resolve("/sound/eng/Police.mp3"), "police reported ahead");
            check(catalog.resolve("/sound/eng/ApproachRailroadCrossing.mp3"), "Approaching a railroad crossing");
            check(catalog.resolve("/sound/eng/TurnLeft.mp3"), "turn left");
            check(catalog.resolve("/sound/eng/200meters.mp3"), "in two hundred meters");
            for (String sound : new String[]{"ping.mp3", "beepbeep.mp3", "speed_limit.mp3", "click.mp3",
                    "TTS_APPTEXT_ESTIMATED_TIME_IN_TRAFFIC", "TTS_APPTEXT_INSIGHTS_APPROACH_SEGMENT_PS_PS", "unknown.mp3"}) {
                check(catalog.resolve(sound), null);
            }
            System.out.println("PASS: 14 prompt, template and non-speech checks against original APK");
        }
    }

    private static void check(String actual, String expected) {
        if (!java.util.Objects.equals(actual, expected)) throw new AssertionError("Expected " + expected + "; got " + actual);
    }
}
