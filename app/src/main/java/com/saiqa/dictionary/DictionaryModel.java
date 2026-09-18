package com.saiqa.dictionary;

import java.util.List;

public class DictionaryModel {

    private int id;
    private String word;
    private String phonetic;
    private List<Meaning> meanings;

    // Reader metadata
    private String note;
    private String highlight;
    private String chapter;
    private String page;

    // Constructors
    public DictionaryModel(int id, String word, String phonetic, List<Meaning> meanings) {
        this(id, word, phonetic, meanings, null, null, null, null);
    }

    public DictionaryModel(String word, String phonetic, List<Meaning> meanings) {
        this(0, word, phonetic, meanings, null, null, null, null);
    }

    public DictionaryModel(String word, String phonetic, List<Meaning> meanings, String chapter, String page, String highlight, String note) {
        this(0, word, phonetic, meanings, chapter, page, highlight, note);
    }

    public DictionaryModel(int id, String word, String phonetic, List<Meaning> meanings, String chapter, String page, String highlight, String note) {
        this.id = id;
        this.word = word;
        this.phonetic = phonetic;
        this.meanings = meanings;
        this.chapter = chapter;
        this.page = page;
        this.highlight = highlight;
        this.note = note;
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getHighlight() {
        return highlight;
    }

    public void setHighlight(String highlight) {
        this.highlight = highlight;
    }

    public String getChapter() {
        return chapter;
    }

    public void setChapter(String chapter) {
        this.chapter = chapter;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }
}
