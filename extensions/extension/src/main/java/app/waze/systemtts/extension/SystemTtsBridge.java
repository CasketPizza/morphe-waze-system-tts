package app.waze.systemtts.extension;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import java.io.File;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Waze 5.24.5: Chunk -> SoundPlayer.g -> local audio -> SoundPlayer.c. */
public final class SystemTtsBridge {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Map<String, String> TEXT = new LinkedHashMap<String, String>() {
        protected boolean removeEldestEntry(Map.Entry<String, String> e) { return size() > 2048; }
    };
    private static final Map<String, Pending> JOBS = new LinkedHashMap<>();
    private static final AtomicInteger IDS = new AtomicInteger();
    private static final ThreadLocal<Boolean> BYPASS = new ThreadLocal<>();
    private static volatile Context context;
    private static TextToSpeech engine;
    private static boolean ready;
    private static int generation;
    private static volatile String status = "No navigation chunks received yet";

    private SystemTtsBridge() {}

    public static void initialize(Context value) { context = value.getApplicationContext(); }

    private static boolean enabled() {
        return context != null && context.getSharedPreferences("system_tts", 0).getBoolean("enabled", false);
    }

    public static void remember(String text, String url, String key) {
        if (text == null || text.trim().isEmpty()) return;
        synchronized (TEXT) { TEXT.put(url, text); TEXT.put(key, text); }
    }

    public static boolean play(Object player, String url, String key, Object callback) {
        if (!enabled() || Boolean.TRUE.equals(BYPASS.get())) return false;
        String text;
        synchronized (TEXT) { text = TEXT.get(url); if (text == null) text = TEXT.get(key); }
        if (text == null) { status = "Unmatched audio: using Waze voice"; return false; }
        final String spokenText = text;
        MAIN.post(() -> {
            Pending pending = new Pending(player, url, key, callback, spokenText, generation);
            JOBS.put(pending.id, pending);
            ensureEngine();
            if (ready) synthesize(pending);
            MAIN.postDelayed(() -> finish(pending.id, false), 10000);
        });
        return true;
    }

    private static void ensureEngine() {
        if (engine != null || context == null) return;
        engine = new TextToSpeech(context, result -> MAIN.post(() -> {
            ready = result == TextToSpeech.SUCCESS;
            if (!ready) {
                status = "System TTS initialization failed; using Waze voice";
                for (String id : JOBS.keySet().toArray(new String[0])) finish(id, false);
                engine.shutdown(); engine = null;
                return;
            }
            engine.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                public void onStart(String id) {}
                public void onDone(String id) { MAIN.post(() -> finish(id, true)); }
                public void onError(String id) { MAIN.post(() -> finish(id, false)); }
            });
            status = "Ready: " + engine.getDefaultEngine();
            for (Pending job : JOBS.values().toArray(new Pending[0])) synthesize(job);
        }));
    }

    private static void synthesize(Pending pending) {
        if (pending.started) return;
        pending.started = true;
        if (engine.synthesizeToFile(pending.text, null, pending.file, pending.id) == TextToSpeech.ERROR) finish(pending.id, false);
    }

    private static void finish(String id, boolean success) {
        Pending pending = JOBS.remove(id);
        if (pending == null) return;
        if (pending.generation != generation) { pending.file.delete(); return; }
        try {
            Class<?> callbackType = Class.forName("h.g.a.a");
            if (success && enabled() && pending.file.length() > 44) {
                Method method = pending.player.getClass().getMethod("c", String.class, String.class,
                        boolean.class, boolean.class, callbackType);
                method.invoke(pending.player, pending.file.getAbsolutePath(), null, false, false, pending.callback);
                status = "Navigation audio replaced using " + engine.getDefaultEngine();
                MAIN.postDelayed(() -> pending.file.delete(), 300000);
            } else {
                status = "System synthesis failed or timed out; using Waze voice";
                original(pending, callbackType);
            }
        } catch (Exception error) {
            status = "Playback integration error: " + error.getClass().getSimpleName();
            Log.e("WazeSystemTTS", status, error);
            try { original(pending, Class.forName("h.g.a.a")); }
            catch (Exception failure) { Log.e("WazeSystemTTS", "Original playback also failed", failure); }
        }
    }

    private static void original(Pending pending, Class<?> callbackType) throws Exception {
        BYPASS.set(true);
        try {
            pending.player.getClass().getMethod("g", String.class, String.class, callbackType)
                    .invoke(pending.player, pending.url, pending.key, pending.callback);
        } finally { BYPASS.remove(); pending.file.delete(); }
    }

    public static void cancel() {
        MAIN.post(() -> {
            generation++;
            if (engine != null) engine.stop();
            for (Pending pending : JOBS.values()) pending.file.delete();
            JOBS.clear();
        });
    }

    public static void addSettings(Activity activity) {
        // Posted from onCreate entry: wait for Waze to install its content view.
        MAIN.post(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            initialize(activity);
            FrameLayout content = activity.findViewById(android.R.id.content);
            if (content == null || content.findViewWithTag("system_tts_button") != null) return;
            Button button = new Button(activity);
            button.setTag("system_tts_button"); button.setText("Android system TTS");
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM | Gravity.END);
            params.bottomMargin = (int) (32 * activity.getResources().getDisplayMetrics().density);
            content.addView(button, params);
            button.setOnClickListener(v -> new AlertDialog.Builder(activity)
                    .setTitle("Android system TTS")
                    .setMultiChoiceItems(new String[]{"Use system voice for navigation"}, new boolean[]{enabled()},
                            (dialog, which, checked) -> {
                                context.getSharedPreferences("system_tts", 0).edit().putBoolean("enabled", checked).apply();
                        cancel();
                            })
                    .setPositiveButton("Test / status", (dialog, which) -> {
                        ensureEngine();
                        MAIN.postDelayed(() -> {
                            if (ready) engine.speak("In two hundred metres, turn left onto George Street.",
                                    TextToSpeech.QUEUE_FLUSH, null, "test");
                            new AlertDialog.Builder(activity).setTitle("System TTS status").setMessage(status)
                                    .setPositiveButton("OK", null).show();
                        }, 1500);
                    })
                    .setNeutralButton("Android TTS settings", (dialog, which) -> {
                        try { activity.startActivity(new Intent("com.android.settings.TTS_SETTINGS")); }
                        catch (Exception error) { status = "Open Text-to-speech in Android settings manually"; }
                    })
                    .setNegativeButton("Close", null).show());
        });
    }

    private static final class Pending {
        final String id = "waze-system-" + IDS.incrementAndGet();
        final Object player, callback;
        final String url, key, text;
        final int generation;
        final File file;
        boolean started;
        Pending(Object player, String url, String key, Object callback, String text, int generation) {
            this.player = player; this.url = url; this.key = key; this.callback = callback;
            this.text = text; this.generation = generation;
            file = new File(context.getCacheDir(), id + ".wav");
        }
    }
}
