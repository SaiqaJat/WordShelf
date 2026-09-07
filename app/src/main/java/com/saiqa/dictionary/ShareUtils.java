package com.saiqa.dictionary;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import java.util.List;

public class ShareUtils {

    public static void shareSingleWord(Context context, DictionaryModel model) {
        if (model == null) return;
        shareMeanings(context, model.getWord(), model.getPhonetic(), model.getMeanings());
    }
    
    public static void shareBook(Context context, String bookTitle, List<DictionaryModel> words) {
        StringBuilder sb = new StringBuilder();
        sb.append("📚 Book: ").append(bookTitle).append("\n");
        sb.append("Total Words: ").append(words.size()).append("\n\n");
        
        for (DictionaryModel model : words) {
            sb.append("📖 ").append(model.getWord() != null ? model.getWord().toUpperCase() : "").append("\n");
            if (model.getPhonetic() != null && !model.getPhonetic().isEmpty()) {
                sb.append("Pronunciation: ").append(model.getPhonetic()).append("\n");
            }
            sb.append("\n");

            if (model.getMeanings() != null) {
                for (Meaning meaning : model.getMeanings()) {
                    if (meaning.getPartOfSpeech() != null) {
                        sb.append("[").append(meaning.getPartOfSpeech().toUpperCase()).append("]\n");
                    }
                    if (meaning.getDefinitions() != null) {
                        for (int i = 0; i < meaning.getDefinitions().size(); i++) {
                            Definition def = meaning.getDefinitions().get(i);
                            if (def != null && def.getDefinition() != null) {
                                sb.append(" • ").append(def.getDefinition().trim()).append("\n");
                                if (def.getExample() != null && !def.getExample().trim().isEmpty()) {
                                    sb.append("   Example: \"").append(def.getExample().trim()).append("\"\n");
                                }
                            }
                        }
                    }
                    if (meaning.getSynonyms() != null && !meaning.getSynonyms().isEmpty()) {
                        sb.append(" Synonyms: ").append(TextUtils.join(", ", meaning.getSynonyms())).append("\n");
                    }
                    if (meaning.getAntonyms() != null && !meaning.getAntonyms().isEmpty()) {
                        sb.append(" Antonyms: ").append(TextUtils.join(", ", meaning.getAntonyms())).append("\n");
                    }
                    sb.append("\n");
                }
            }
            sb.append("------------------------\n\n");
        }
        
        sb.append("Shared via WordShelf");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "WordShelf Book: " + bookTitle);
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        context.startActivity(Intent.createChooser(shareIntent, "Share Book via"));
    }

    public static void shareMeanings(Context context, String word, String phonetic, List<Meaning> meaningList) {
        StringBuilder sb = new StringBuilder();
        sb.append("📖 ").append(word != null ? word.toUpperCase() : "").append("\n");
        if (phonetic != null && !phonetic.isEmpty()) {
            sb.append("Pronunciation: ").append(phonetic).append("\n");
        }
        sb.append("\n");

        if (meaningList != null) {
            for (Meaning meaning : meaningList) {
                if (meaning.getPartOfSpeech() != null) {
                    sb.append("[").append(meaning.getPartOfSpeech().toUpperCase()).append("]\n");
                }
                if (meaning.getDefinitions() != null) {
                    for (int i = 0; i < meaning.getDefinitions().size(); i++) {
                        Definition def = meaning.getDefinitions().get(i);
                        if (def != null && def.getDefinition() != null) {
                            sb.append(" • ").append(def.getDefinition().trim()).append("\n");
                            if (def.getExample() != null && !def.getExample().trim().isEmpty()) {
                                sb.append("   Example: \"").append(def.getExample().trim()).append("\"\n");
                            }
                        }
                    }
                }
                if (meaning.getSynonyms() != null && !meaning.getSynonyms().isEmpty()) {
                    sb.append(" Synonyms: ").append(TextUtils.join(", ", meaning.getSynonyms())).append("\n");
                }
                if (meaning.getAntonyms() != null && !meaning.getAntonyms().isEmpty()) {
                    sb.append(" Antonyms: ").append(TextUtils.join(", ", meaning.getAntonyms())).append("\n");
                }
                sb.append("\n");
            }
        }
        sb.append("Shared via WordShelf");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Definition: " + word);
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        context.startActivity(Intent.createChooser(shareIntent, "Share Word via"));
    }
}
