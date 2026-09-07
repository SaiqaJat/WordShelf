package com.saiqa.dictionary;

public class SearchHistoryItem {
    private final String word;
    private final String phonetic;
    private final long timestamp;

    public SearchHistoryItem(String word, String phonetic, long timestamp) {
        this.word = word;
        this.phonetic = phonetic;
        this.timestamp = timestamp;
    }

    public String getWord() {
        return word;
    }

    public String getPhonetic() {
        return phonetic;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
