package com.saiqa.dictionary;

import android.content.Context;
import android.os.AsyncTask;
import android.util.LruCache;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Two-layer word definition cache:
 * <ul>
 *   <li><b>L1 (in-memory)</b> — {@link LruCache} of 100 entries. Instant O(1) lookup.
 *       Lives for the duration of the process.</li>
 *   <li><b>L2 (persistent)</b> — SQLite {@code DefinitionCache} table via
 *       {@link DictionaryDatabaseHelper}. Survives app restarts. Up to 200 entries,
 *       automatically pruned by LRU timestamp.</li>
 * </ul>
 *
 * <p>Initialization: call {@link #init(Context)} once in {@code MainActivity.onCreate()}.
 * The context is held as a {@link WeakReference} to avoid leaking the Activity/Application.</p>
 *
 * <p>Thread-safety: {@code get()} and {@code put()} are called on the main thread;
 * disk writes are offloaded to a background thread to avoid blocking the UI.</p>
 */
public class SearchCacheManager {

    private static SearchCacheManager instance;

    /** L1: in-memory LRU cache — max 100 word definitions. */
    private final LruCache<String, List<WordResultData>> memoryCache;

    /** Weak reference to application context for L2 SQLite access. */
    private WeakReference<Context> contextRef;

    private SearchCacheManager() {
        memoryCache = new LruCache<>(100);
    }

    public static synchronized SearchCacheManager getInstance() {
        if (instance == null) {
            instance = new SearchCacheManager();
        }
        return instance;
    }

    /**
     * Initializes the persistent (L2) backend. Must be called once before any
     * {@code get()} call that should benefit from cross-session caching.
     *
     * @param context Application context (or any context; stored as a weak reference).
     */
    public static synchronized void init(Context context) {
        SearchCacheManager mgr = getInstance();
        mgr.contextRef = new WeakReference<>(context.getApplicationContext());
    }

    /**
     * Looks up a word definition.
     * <ol>
     *   <li>Checks L1 (memory) — returns immediately on hit.</li>
     *   <li>Checks L2 (SQLite disk) — on hit, populates L1 and returns.</li>
     *   <li>Returns {@code null} on complete miss (caller should fetch from API).</li>
     * </ol>
     *
     * @param word The searched word (case-insensitive; trimmed internally).
     * @return Cached result list, or {@code null} if not cached.
     */
    public List<WordResultData> get(String word) {
        if (word == null) return null;
        String key = word.trim().toLowerCase();

        // L1 hit — instant
        List<WordResultData> memResult = memoryCache.get(key);
        if (memResult != null) return memResult;

        // L2 hit — disk read
        Context ctx = contextRef != null ? contextRef.get() : null;
        if (ctx != null) {
            try {
                DictionaryDatabaseHelper db = new DictionaryDatabaseHelper(ctx);
                List<WordResultData> diskResult = db.getDefinitionCache(key);
                db.close();
                if (diskResult != null && !diskResult.isEmpty()) {
                    // Warm up L1 from L2
                    memoryCache.put(key, diskResult);
                    return diskResult;
                }
            } catch (Exception ignored) {
                // Disk read failure is non-fatal; will fall through to API call
            }
        }

        return null;
    }

    /**
     * Stores a definition result in both L1 (memory) and L2 (disk) caches.
     * The disk write is performed asynchronously so as not to block the UI thread.
     *
     * @param word The word key (normalized to lowercase internally).
     * @param data The result list to cache.
     */
    public void put(String word, List<WordResultData> data) {
        if (word == null || data == null || data.isEmpty()) return;
        String key = word.trim().toLowerCase();

        // L1 — synchronous memory write
        memoryCache.put(key, data);

        // L2 — async disk write
        Context ctx = contextRef != null ? contextRef.get() : null;
        if (ctx != null) {
            new DiskWriteTask(ctx, key, data).execute();
        }
    }

    /**
     * Evicts all entries from the in-memory L1 cache.
     * Disk cache is unaffected (use database methods to clear it if needed).
     */
    public void clearMemory() {
        memoryCache.evictAll();
    }

    // ── Internal AsyncTask for non-blocking disk write ────────────────────────

    @SuppressWarnings("deprecation") // AsyncTask acceptable for simple fire-and-forget disk I/O
    private static class DiskWriteTask extends AsyncTask<Void, Void, Void> {
        private final WeakReference<Context> ctxRef;
        private final String key;
        private final List<WordResultData> data;

        DiskWriteTask(Context ctx, String key, List<WordResultData> data) {
            this.ctxRef = new WeakReference<>(ctx);
            this.key = key;
            this.data = data;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Context ctx = ctxRef.get();
            if (ctx == null) return null;
            try {
                DictionaryDatabaseHelper db = new DictionaryDatabaseHelper(ctx);
                db.saveDefinitionCache(key, data);
                db.close();
            } catch (Exception ignored) {
                // Non-fatal: disk write failure means next session won't have this cache hit,
                // but in-memory L1 still works for the current session.
            }
            return null;
        }
    }
}
