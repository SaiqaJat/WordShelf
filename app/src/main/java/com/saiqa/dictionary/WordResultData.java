package com.saiqa.dictionary;

import java.util.List;

public class WordResultData {

    private String word;
    private String phonetic;
    private List<Phonetic> phonetics;
    private List<Meaning> meanings;

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getPhonetic() {
        if (phonetic != null && !phonetic.trim().isEmpty()) {
            return phonetic.trim();
        }
        if (phonetics != null && !phonetics.isEmpty()) {
            for (Phonetic p : phonetics) {
                if (p != null && p.getText() != null && !p.getText().trim().isEmpty()) {
                    return p.getText().trim();
                }
            }
        }
        return "";
    }

    public void setPhonetic(String phonetic) {
        this.phonetic = phonetic;
    }

    public List<Phonetic> getPhonetics() {
        return phonetics;
    }

    public void setPhonetics(List<Phonetic> phonetics) {
        this.phonetics = phonetics;
    }

    public List<Meaning> getMeanings() {
        return meanings;
    }

    public void setMeanings(List<Meaning> meanings) {
        this.meanings = meanings;
    }
}
