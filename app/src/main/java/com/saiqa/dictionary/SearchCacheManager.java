package com.saiqa.dictionary;

import android.util.LruCache;

import java.util.List;

public class SearchCacheManager {

    private static SearchCacheManager instance;
    private final LruCache<String, List<WordResultData>> cache;

    private SearchCacheManager() {
        // Cache up to 100 recent word definitions
        cache = new LruCache<>(100);
    }

    public static synchronized SearchCacheManager getInstance() {
        if (instance == null) {
            instance = new SearchCacheManager();
        }
        return instance;
    }

    public List<WordResultData> get(String word) {
        if (word == null) return null;
        return cache.get(word.trim().toLowerCase());
    }

    public void put(String word, List<WordResultData> data) {
        if (word == null || data == null || data.isEmpty()) return;
        cache.put(word.trim().toLowerCase(), data);
    }

    public void clear() {
        cache.evictAll();
    }
}
