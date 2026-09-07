package com.saiqa.dictionary;

import static androidx.core.content.ContextCompat.startActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class WordResultAdapter extends RecyclerView.Adapter<WordResultAdapter.ViewHolder> {

    Context context;
    List<Meaning> meaningList;
    String word;

    public WordResultAdapter(Context context, List<Meaning> meaningList, String word) {
        this.context = context;
        this.meaningList = meaningList;
        this.word = word;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.word_meaning_row, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Meaning meaning = meaningList.get(position);

        // Part of speech formatting & chip coloring
        String pos = meaning.getPartOfSpeech() != null ? meaning.getPartOfSpeech() : "";
        holder.parts_of_speech_textView.setText(pos.toUpperCase());

        if (pos.equalsIgnoreCase("noun")) {
            holder.parts_of_speech_textView.setBackgroundTintList(
                    androidx.core.content.ContextCompat.getColorStateList(context, R.color.chip_noun_bg));
            holder.parts_of_speech_textView.setTextColor(
                    androidx.core.content.ContextCompat.getColor(context, R.color.chip_noun_text));
        } else if (pos.equalsIgnoreCase("verb")) {
            holder.parts_of_speech_textView.setBackgroundTintList(
                    androidx.core.content.ContextCompat.getColorStateList(context, R.color.chip_verb_bg));
            holder.parts_of_speech_textView.setTextColor(
                    androidx.core.content.ContextCompat.getColor(context, R.color.chip_verb_text));
        } else if (pos.equalsIgnoreCase("adjective")) {
            holder.parts_of_speech_textView.setBackgroundTintList(
                    androidx.core.content.ContextCompat.getColorStateList(context, R.color.chip_adj_bg));
            holder.parts_of_speech_textView.setTextColor(
                    androidx.core.content.ContextCompat.getColor(context, R.color.chip_adj_text));
        } else if (pos.equalsIgnoreCase("adverb")) {
            holder.parts_of_speech_textView.setBackgroundTintList(
                    androidx.core.content.ContextCompat.getColorStateList(context, R.color.chip_adv_bg));
            holder.parts_of_speech_textView.setTextColor(
                    androidx.core.content.ContextCompat.getColor(context, R.color.chip_adv_text));
        } else {
            holder.parts_of_speech_textView.setBackgroundTintList(
                    androidx.core.content.ContextCompat.getColorStateList(context, R.color.chip_default_bg));
            holder.parts_of_speech_textView.setTextColor(
                    androidx.core.content.ContextCompat.getColor(context, R.color.chip_default_text));
        }

        // Definitions
        StringBuilder allDefinitions = new StringBuilder();
        if (meaning.getDefinitions() != null) {
            for (int i = 0; i < meaning.getDefinitions().size(); i++) {
                Definition definition = meaning.getDefinitions().get(i);
                if (definition != null && definition.getDefinition() != null) {
                    allDefinitions.append(i + 1).append(". ").append(definition.getDefinition().trim()).append("\n\n");
                }
            }
        }
        holder.definitions_textView.setText(allDefinitions.toString().trim());

        // Synonyms
        if (meaning.getSynonyms() == null || meaning.getSynonyms().isEmpty()) {
            if (holder.ll_synonyms != null) holder.ll_synonyms.setVisibility(View.GONE);
            else {
                holder.synonyms_title_textView.setVisibility(View.GONE);
                holder.synonyms_textView.setVisibility(View.GONE);
            }
        } else {
            if (holder.ll_synonyms != null) holder.ll_synonyms.setVisibility(View.VISIBLE);
            holder.synonyms_title_textView.setVisibility(View.VISIBLE);
            holder.synonyms_textView.setVisibility(View.VISIBLE);
            holder.synonyms_textView.setText(TextUtils.join(", ", meaning.getSynonyms()));
        }

        // Antonyms
        if (meaning.getAntonyms() == null || meaning.getAntonyms().isEmpty()) {
            if (holder.ll_antonyms != null) holder.ll_antonyms.setVisibility(View.GONE);
            else {
                holder.antonyms_title_textView.setVisibility(View.GONE);
                holder.antonyms_textView.setVisibility(View.GONE);
            }
        } else {
            if (holder.ll_antonyms != null) holder.ll_antonyms.setVisibility(View.VISIBLE);
            holder.antonyms_title_textView.setVisibility(View.VISIBLE);
            holder.antonyms_textView.setVisibility(View.VISIBLE);
            holder.antonyms_textView.setText(TextUtils.join(", ", meaning.getAntonyms()));
        }
    }

    @Override
    public int getItemCount() {
        return meaningList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView parts_of_speech_textView, definitions_textView,
                synonyms_textView, antonyms_textView,
                synonyms_title_textView, antonyms_title_textView;




        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            parts_of_speech_textView = itemView.findViewById(R.id.parts_of_speech_textView);
            definitions_textView = itemView.findViewById(R.id.definitions_textView);
            synonyms_textView = itemView.findViewById(R.id.synonyms_textView);
            antonyms_textView = itemView.findViewById(R.id.antonyms_textView);
            synonyms_title_textView = itemView.findViewById(R.id.synonyms_title_textView);
            antonyms_title_textView = itemView.findViewById(R.id.antonyms_title_textView);
            ll_synonyms = itemView.findViewById(R.id.ll_synonyms);
            ll_antonyms = itemView.findViewById(R.id.ll_antonyms);
        }
        View ll_synonyms, ll_antonyms;
    }
    
    @SuppressLint("NotifyDataSetChanged")
    public void update(List<Meaning> meanings) {
        this.meaningList = meanings;
        notifyDataSetChanged();
    }
}
