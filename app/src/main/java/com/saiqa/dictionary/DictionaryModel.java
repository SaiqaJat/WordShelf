package com.saiqa.dictionary;

import java.util.List;

public class DictionaryModel {

    private int id;
    private String word;
    private String phonetic;
    private List<Meaning> meanings;

    // Constructors
    public DictionaryModel(int id, String word, String phonetic, List<Meaning> meanings) {
        this.id = id;
        this.word = word;
        this.phonetic = phonetic;
        this.meanings = meanings;
    }

    public DictionaryModel(String word, String phonetic, List<Meaning> meanings) {
        this.word = word;
        this.phonetic = phonetic;
        this.meanings = meanings;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getPhonetic() {
        return phonetic;
    }

    public void setPhonetic(String phonetic) {
        this.phonetic = phonetic;
    }

    public List<Meaning> getMeanings() {
        return meanings;
    }

    public void setMeanings(List<Meaning> meanings) {
        this.meanings = meanings;
    }
}
