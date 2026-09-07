package com.saiqa.dictionary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BookSelectionAdapter extends RecyclerView.Adapter<BookSelectionAdapter.BooksViewHolder> {

    private final List<String> bookList;
    private final OnBookClickListener listener;

    // Interface to handle clicks
    public interface OnBookClickListener {
        void onBookClick(String bookName);
    }

    public BookSelectionAdapter(List<String> bookList,OnBookClickListener listener) {
        this.bookList = bookList;
        this.listener = listener;
    }


    @NonNull
    @Override
    public BookSelectionAdapter.BooksViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_book_name,parent,false);
        return new BooksViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookSelectionAdapter.BooksViewHolder holder, int position) {

        String title = bookList.get(position);
        holder.bookNameTextView.setText(title);
        holder.itemView.setOnClickListener(v -> listener.onBookClick(title));

    }

    @Override
    public int getItemCount() {
        return bookList.size();
    }

    public static class BooksViewHolder extends RecyclerView.ViewHolder {

        TextView bookNameTextView;

        public BooksViewHolder(@NonNull View itemView) {
            super(itemView);

            bookNameTextView = itemView.findViewById(R.id.book_name_text);
        }



    }
}
