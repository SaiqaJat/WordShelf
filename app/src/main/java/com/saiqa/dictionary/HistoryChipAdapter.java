package com.saiqa.dictionary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class HistoryChipAdapter extends RecyclerView.Adapter<HistoryChipAdapter.ViewHolder> {

    private final List<SearchHistoryItem> items = new ArrayList<>();
    private final OnHistoryInteractionListener listener;

    public interface OnHistoryInteractionListener {
        void onHistoryClick(String word);
        void onHistoryDelete(String word);
    }

    public HistoryChipAdapter(OnHistoryInteractionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<SearchHistoryItem> list) {
        this.items.clear();
        if (list != null) {
            this.items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_chip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchHistoryItem item = items.get(position);
        holder.chip.setText(item.getWord());

        holder.chip.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHistoryClick(item.getWord());
            }
        });

        holder.chip.setOnCloseIconClickListener(v -> {
            if (listener != null) {
                listener.onHistoryDelete(item.getWord());
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        Chip chip;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            chip = (Chip) itemView;
        }
    }
}
