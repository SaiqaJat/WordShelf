package com.saiqa.dictionary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AutocompleteAdapter extends RecyclerView.Adapter<AutocompleteAdapter.ViewHolder> {

    private final List<DatamuseSearchResult> suggestions = new ArrayList<>();
    private final OnSuggestionClickListener listener;

    public interface OnSuggestionClickListener {
        void onSuggestionClick(String word);
    }

    public AutocompleteAdapter(OnSuggestionClickListener listener) {
        this.listener = listener;
    }

    public void setSuggestions(List<DatamuseSearchResult> list) {
        this.suggestions.clear();
        if (list != null) {
            this.suggestions.addAll(list);
        }
        notifyDataSetChanged();
    }

    public void clear() {
        this.suggestions.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_autocomplete_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DatamuseSearchResult item = suggestions.get(position);
        holder.suggestionText.setText(item.getWord());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSuggestionClick(item.getWord());
            }
        });
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView suggestionText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            suggestionText = itemView.findViewById(R.id.suggestion_text);
        }
    }
}
