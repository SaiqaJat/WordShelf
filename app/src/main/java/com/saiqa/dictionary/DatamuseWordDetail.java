package com.saiqa.dictionary;

import java.util.List;

public class DatamuseWordDetail {
    private String word;
    private List<String> defs;

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public List<String> getDefs() {
        return defs;
    }

    public void setDefs(List<String> defs) {
        this.defs = defs;
    }
}
