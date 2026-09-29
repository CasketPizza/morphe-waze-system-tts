package app.waze.systemtts.extension;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Routes Waze navigation TTS text to Android's currently configured TTS engine. */
public final class SystemTtsBridge {
    private static final Map<String, String> pendingText = new ConcurrentHashMap<>();
    private static final Object lock = new Object();
    private static volatile TextToSpeech engine;
    private static volatile boolean ready;
    private static volatile String queuedKey;

    private SystemTtsBridge() {}

    public static void remember(String text, String cacheKey) {
        if (text == null || cacheKey == null) return;
        pendingText.put(cacheKey, text);
        ensureEngine();
    }

    public static void speak(String cacheKey) {
        if (cacheKey == null) return;
        ensureEngine();
        if (!ready) {
            queuedKey = cacheKey;
            return;
        }
        String text = pendingText.remove(cacheKey);
        if (text == null || text.length() == 0) return;
        TextToSpeech current = engine;
        if (current != null) {
            current.speak(text, TextToSpeech.QUEUE_FLUSH, null, cacheKey);
        }
    }

    public static void speakText(String text) {
        if (text == null || text.length() == 0) return;
        ensureEngine();
        TextToSpeech current = engine;
        if (current != null && ready) {
            current.speak(text, TextToSpeech.QUEUE_FLUSH, null, "waze-direct-tts");
        }
    }

    private static void ensureEngine() {
        if (engine != null) return;
        synchronized (lock) {
            if (engine != null) return;
            Context context = applicationContext();
            if (context == null) return;
            engine = new TextToSpeech(context.getApplicationContext(), status -> {
                ready = status == TextToSpeech.SUCCESS;
                String key = queuedKey;
                queuedKey = null;
                if (ready && key != null) speak(key);
            });
        }
    }

    private static Context applicationContext() {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Object application = activityThread.getMethod("currentApplication").invoke(null);
            return application instanceof Context ? (Context) application : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
