// Sibling Docs: ./README.md | Decisions: ./DECISIONS.md
package com.parse.livequery;

/**
 * Injectable time source for the LiveQuery client.
 *
 * <p>Production code uses {@link #SYSTEM}; tests inject a fake to make
 * reconnect timing deterministic. Satisfies the Clock Injection rule in
 * {@code docs/conventions.md} — no direct clock reads in business logic.
 * Scheduling also runs through the clock so fakes can drive backoff
 * without real waiting.
 */
public interface ParseLiveQueryClock {

    long currentTimeMillis();

    void postDelayed(Runnable runnable, long delayMs);

    void removeCallbacks(Runnable runnable);

    ParseLiveQueryClock SYSTEM = new ParseLiveQueryClock() {
        private android.os.Handler handler;

        private synchronized android.os.Handler handler() {
            if (handler == null) {
                handler = new android.os.Handler(android.os.Looper.getMainLooper());
            }
            return handler;
        }

        @Override
        public long currentTimeMillis() {
            return System.currentTimeMillis();
        }

        @Override
        public void postDelayed(Runnable runnable, long delayMs) {
            handler().postDelayed(runnable, delayMs);
        }

        @Override
        public void removeCallbacks(Runnable runnable) {
            synchronized (this) {
                if (handler != null && runnable != null) {
                    handler.removeCallbacks(runnable);
                }
            }
        }
    };
}
