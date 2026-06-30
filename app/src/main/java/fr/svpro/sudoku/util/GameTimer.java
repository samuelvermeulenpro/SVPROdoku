package fr.svpro.sudoku.util;

import android.os.Handler;
import android.os.Looper;

/**
 * Chronomètre de jeu avec callbacks sur le thread principal.
 */
public class GameTimer {

    public interface Listener {
        void onTick(long elapsedSeconds);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Listener listener;
    private long startTimeMs;
    private long accumulatedMs = 0;
    private boolean running = false;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (running && listener != null) {
                long elapsed = accumulatedMs + (System.currentTimeMillis() - startTimeMs);
                listener.onTick(elapsed / 1000);
                handler.postDelayed(this, 500);
            }
        }
    };

    public void setListener(Listener listener) { this.listener = listener; }

    public void start() {
        if (running) return;
        running = true;
        startTimeMs = System.currentTimeMillis();
        handler.post(tickRunnable);
    }

    public void pause() {
        if (!running) return;
        accumulatedMs += System.currentTimeMillis() - startTimeMs;
        running = false;
        handler.removeCallbacks(tickRunnable);
    }

    public void reset() {
        pause();
        accumulatedMs = 0;
    }

    /** Pré-charge un temps accumulé (utilisé pour la restauration d'une partie sauvegardée). */
    public void addAccumulatedMs(long ms) {
        accumulatedMs += ms;
    }

    public long getElapsedSeconds() {
        long total = accumulatedMs;
        if (running) total += System.currentTimeMillis() - startTimeMs;
        return total / 1000;
    }

    public static String format(long seconds) {
        long m = seconds / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    public boolean isRunning() { return running; }
}
