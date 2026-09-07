package com.saiqa.dictionary;

import com.google.gson.annotations.SerializedName;

public class DatamuseSearchResult {
    @SerializedName("word")
    private String word;

    @SerializedName("score")
    private int score;

    public DatamuseSearchResult() {}

    public DatamuseSearchResult(String word) {
        this.word = word;
    }

    public String getWord() {
        return word;
    }

    public int getScore() {
        return score;
    }
}
