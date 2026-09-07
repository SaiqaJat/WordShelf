package com.saiqa.dictionary;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SavedWordsAdapter extends RecyclerView.Adapter<SavedWordsAdapter.WordViewHolder> {

    private final List<DictionaryModel> savedWordsList;
    private final OnWordSpeakListener speakListener;

    public interface OnWordSpeakListener {
        void onSpeakWord(String word);
    }

    public SavedWordsAdapter(List<DictionaryModel> savedWordsList) {
        this(savedWordsList, null);
    }

    public SavedWordsAdapter(List<DictionaryModel> savedWordsList, OnWordSpeakListener speakListener) {
        this.savedWordsList = savedWordsList;
        this.speakListener = speakListener;
    }

    @NonNull
    @Override
    public WordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.saved_words_row, parent, false);
        return new WordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WordViewHolder holder, @SuppressLint("RecyclerView") int position) {
        DictionaryModel model = savedWordsList.get(position);
        Context context = holder.itemView.getContext();

        String wordStr = model.getWord() != null ? model.getWord().trim() : "";
        holder.saved_words_textView.setText(wordStr);

        String phoneticStr = model.getPhonetic() != null ? model.getPhonetic().trim() : "";
        if (!phoneticStr.isEmpty()) {
            holder.saved_words_phonetic_textView.setVisibility(View.VISIBLE);
            holder.saved_words_phonetic_textView.setText(phoneticStr);
        } else {
            holder.saved_words_phonetic_textView.setVisibility(View.GONE);
        }

        // Clear previous meanings
        holder.ll_meanings_container.removeAllViews();

        StringBuilder copyBuilder = new StringBuilder();
        copyBuilder.append(wordStr).append(phoneticStr.isEmpty() ? "" : " " + phoneticStr).append("\n\n");

        if (model.getMeanings() != null) {
            for (Meaning meaning : model.getMeanings()) {
                View meaningView = LayoutInflater.from(context).inflate(R.layout.meaning_item, holder.ll_meanings_container, false);
                
                TextView posTextView = meaningView.findViewById(R.id.meaning_parts_of_speech_textView);
                TextView defsTextView = meaningView.findViewById(R.id.meaning_definitions_textView);
                TextView synsTextView = meaningView.findViewById(R.id.meaning_synonyms_textView);
                TextView antsTextView = meaningView.findViewById(R.id.meaning_antonyms_textView);
                View ll_syns = meaningView.findViewById(R.id.ll_meaning_synonyms);
                View ll_ants = meaningView.findViewById(R.id.ll_meaning_antonyms);

                String pos = meaning.getPartOfSpeech() != null ? meaning.getPartOfSpeech().trim() : "";
                posTextView.setText(pos.toUpperCase());
                copyBuilder.append("[").append(pos.toUpperCase()).append("]\n");

                // Set Color
                if (pos.equalsIgnoreCase("noun")) {
                    posTextView.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.chip_noun_bg));
                    posTextView.setTextColor(ContextCompat.getColor(context, R.color.chip_noun_text));
                } else if (pos.equalsIgnoreCase("verb")) {
                    posTextView.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.chip_verb_bg));
                    posTextView.setTextColor(ContextCompat.getColor(context, R.color.chip_verb_text));
                } else if (pos.equalsIgnoreCase("adjective")) {
                    posTextView.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.chip_adj_bg));
                    posTextView.setTextColor(ContextCompat.getColor(context, R.color.chip_adj_text));
                } else if (pos.equalsIgnoreCase("adverb")) {
                    posTextView.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.chip_adv_bg));
                    posTextView.setTextColor(ContextCompat.getColor(context, R.color.chip_adv_text));
                } else {
                    posTextView.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.chip_default_bg));
                    posTextView.setTextColor(ContextCompat.getColor(context, R.color.chip_default_text));
                }

                // Definitions
                StringBuilder defBuilder = new StringBuilder();
                if (meaning.getDefinitions() != null) {
                    List<Definition> defs = meaning.getDefinitions();
                    for (int i = 0; i < defs.size(); i++) {
                        Definition def = defs.get(i);
                        if (def != null && def.getDefinition() != null) {
                            defBuilder.append(i + 1).append(". ").append(def.getDefinition().trim());
                            if (i != defs.size() - 1) defBuilder.append("\n\n");
                        }
                    }
                }
                defsTextView.setText(defBuilder.toString());
                copyBuilder.append(defBuilder.toString()).append("\n");

                // Synonyms
                if (meaning.getSynonyms() == null || meaning.getSynonyms().isEmpty()) {
                    ll_syns.setVisibility(View.GONE);
                } else {
                    ll_syns.setVisibility(View.VISIBLE);
                    String synStr = TextUtils.join(", ", meaning.getSynonyms());
                    synsTextView.setText(synStr);
                    copyBuilder.append("Synonyms: ").append(synStr).append("\n");
                }

                // Antonyms
                if (meaning.getAntonyms() == null || meaning.getAntonyms().isEmpty()) {
                    ll_ants.setVisibility(View.GONE);
                } else {
                    ll_ants.setVisibility(View.VISIBLE);
                    String antStr = TextUtils.join(", ", meaning.getAntonyms());
                    antsTextView.setText(antStr);
                    copyBuilder.append("Antonyms: ").append(antStr).append("\n");
                }
                
                copyBuilder.append("\n");
                holder.ll_meanings_container.addView(meaningView);
            }
        }

        // Audio pronounce button action
        if (holder.btn_audio_saved_word != null) {
            holder.btn_audio_saved_word.setOnClickListener(v -> {
                if (speakListener != null) {
                    speakListener.onSpeakWord(wordStr);
                }
            });
        }

        // Share button action
        if (holder.btn_share_saved_word != null) {
            holder.btn_share_saved_word.setOnClickListener(v -> {
                ShareUtils.shareSingleWord(context, model);
            });
        }

        // PDF Export button action
        if (holder.btn_pdf_saved_word != null) {
            holder.btn_pdf_saved_word.setOnClickListener(v -> {
                PdfExporter.exportSingleWord(context, model);
            });
        }

        // Copy button action
        if (holder.btn_copy_saved_word != null) {
            holder.btn_copy_saved_word.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Word Definition", copyBuilder.toString().trim());
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Long press delete dialog
        holder.itemView.setOnLongClickListener(v -> {
            Dialog dialog = new Dialog(v.getContext());
            dialog.setContentView(R.layout.delete_dialogbox_layout);
            dialog.setCancelable(true);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            dialog.show();

            Button cancel_delete_btn = dialog.findViewById(R.id.cancel_delete_btn);
            Button delete_row_btn = dialog.findViewById(R.id.delete_row_btn);

            delete_row_btn.setOnClickListener(v12 -> {
                int id = savedWordsList.get(position).getId();
                DictionaryDatabaseHelper DictionaryDB = new DictionaryDatabaseHelper(v.getContext());
                DictionaryDB.deleteWordSavedInBook(id);

                savedWordsList.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, savedWordsList.size());
                dialog.dismiss();
            });

            cancel_delete_btn.setOnClickListener(v1 -> dialog.dismiss());
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return savedWordsList.size();
    }

    public static class WordViewHolder extends RecyclerView.ViewHolder {

        TextView saved_words_textView, saved_words_phonetic_textView;
        LinearLayout ll_meanings_container;
        ImageButton btn_audio_saved_word, btn_share_saved_word, btn_pdf_saved_word, btn_copy_saved_word;

        public WordViewHolder(@NonNull View itemView) {
            super(itemView);
            saved_words_textView = itemView.findViewById(R.id.saved_words_textView);
            saved_words_phonetic_textView = itemView.findViewById(R.id.saved_words_phonetic_textView);
            ll_meanings_container = itemView.findViewById(R.id.ll_meanings_container);
            
            btn_audio_saved_word = itemView.findViewById(R.id.btn_audio_saved_word);
            btn_share_saved_word = itemView.findViewById(R.id.btn_share_saved_word);
            btn_pdf_saved_word = itemView.findViewById(R.id.btn_pdf_saved_word);
            btn_copy_saved_word = itemView.findViewById(R.id.btn_copy_saved_word);
        }
    }
}
