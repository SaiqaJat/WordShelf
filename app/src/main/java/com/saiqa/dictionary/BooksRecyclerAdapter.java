package com.saiqa.dictionary;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BooksRecyclerAdapter extends RecyclerView.Adapter<BooksRecyclerAdapter.MyViewHolder> {
    Context context;
    ArrayList<String> allBooks;
    Map<String, Integer> wordCounts = new HashMap<>();

    public BooksRecyclerAdapter(Context context, ArrayList<String> allBooks) {
        this.context = context;
        this.allBooks = allBooks;
        updateWordCounts();
    }

    public void updateWordCounts() {
        try {
            DictionaryDatabaseHelper db = new DictionaryDatabaseHelper(context);
            this.wordCounts = db.getBookWordCounts();
        } catch (Exception e) {
            this.wordCounts = new HashMap<>();
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.book_row, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
        String bookTitle = allBooks.get(position);
        holder.book_name.setText(bookTitle);

        Integer count = wordCounts.get(bookTitle);
        int wordCount = (count != null) ? count : 0;
        holder.book_word_count.setText(wordCount == 1 ? "1 word saved" : wordCount + " words saved");

        holder.list_item.setOnLongClickListener(v -> {
            Dialog dialog = new Dialog(context);
            dialog.setContentView(R.layout.add_update_books_layout);
            dialog.setCancelable(true);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            dialog.show();

            EditText edt_book_name = dialog.findViewById(R.id.edt_book_name);
            Button add_book_btn = dialog.findViewById(R.id.add_book_btn);
            Button cancel_button = dialog.findViewById(R.id.cancel_button);
            TextView textTitle = dialog.findViewById(R.id.textTitle);

            add_book_btn.setText("Update");
            textTitle.setText("Update Book");
            edt_book_name.setText(allBooks.get(position));

            add_book_btn.setOnClickListener(v3 -> {
                String newBookName = edt_book_name.getText().toString().trim();
                if (!newBookName.isEmpty()) {
                    String oldBookTitle = allBooks.get(position);
                    DictionaryDatabaseHelper DictionaryDB = new DictionaryDatabaseHelper(context);
                    DictionaryDB.updateBooks(oldBookTitle, newBookName);
                    allBooks.set(position, newBookName);
                    updateWordCounts();
                    notifyItemChanged(position);
                    dialog.dismiss();
                } else {
                    Toast.makeText(context, "Enter Book Name", Toast.LENGTH_SHORT).show();
                }
            });
            cancel_button.setOnClickListener(view12 -> dialog.dismiss());
            return true;
        });

        holder.delete_row_icon.setOnClickListener(v -> {
            Dialog dialog = new Dialog(context);
            dialog.setContentView(R.layout.delete_dialogbox_layout);
            dialog.setCancelable(true);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            dialog.show();

            Button cancel_delete_btn = dialog.findViewById(R.id.cancel_delete_btn);
            Button delete_row_btn = dialog.findViewById(R.id.delete_row_btn);

            delete_row_btn.setOnClickListener(v2 -> {
                String bookName = allBooks.get(position);
                DictionaryDatabaseHelper DictionaryDB = new DictionaryDatabaseHelper(context);
                DictionaryDB.deleteRow(bookName);

                allBooks.remove(position);
                updateWordCounts();
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, allBooks.size());
                dialog.dismiss();
            });

            cancel_delete_btn.setOnClickListener(v1 -> dialog.dismiss());
        });

        holder.list_item.setOnClickListener(v -> {
            Intent intent = new Intent(context, WordsSavedOnBooks.class);
            intent.putExtra("book_title", allBooks.get(position));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return allBooks.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView book_name, book_word_count;
        View list_item;
        ImageView delete_row_icon;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            book_name = itemView.findViewById(R.id.book_name);
            book_word_count = itemView.findViewById(R.id.book_word_count);
            list_item = itemView.findViewById(R.id.list_item);
            delete_row_icon = itemView.findViewById(R.id.delete_row_icon);
        }
    }

}
