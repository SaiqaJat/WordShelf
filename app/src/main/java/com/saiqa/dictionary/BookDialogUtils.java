package com.saiqa.dictionary;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class BookDialogUtils {

    public interface OnBookCreatedListener {
        void onBookCreated(String bookTitle, boolean success);
    }

    public static void showCreateBookDialog(Context context, DictionaryDatabaseHelper db, OnBookCreatedListener listener) {
        Dialog dialog = new Dialog(context, R.style.Theme_WordShelf_Dialog);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.add_update_books_layout, null);
        dialog.setContentView(dialogView);
        dialog.setCancelable(true);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText edt_book_name = dialogView.findViewById(R.id.edt_book_name);
        EditText edt_book_author = dialogView.findViewById(R.id.edt_book_author);
        Button add_book_btn = dialogView.findViewById(R.id.add_book_btn);
        Button cancel_button = dialogView.findViewById(R.id.cancel_button);

        add_book_btn.setOnClickListener(v -> {
            String bookName = edt_book_name.getText() != null ? edt_book_name.getText().toString().trim() : "";
            String author = edt_book_author != null && edt_book_author.getText() != null 
                    ? edt_book_author.getText().toString().trim() : "";
            if (bookName.isEmpty()) {
                Toast.makeText(context, "Please enter a book name", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean created = db.addBook(bookName, author);
            if (created) {
                dialog.dismiss();
                if (listener != null) {
                    listener.onBookCreated(bookName, true);
                }
            } else {
                Toast.makeText(context, "Failed to create book. Please try again.", Toast.LENGTH_SHORT).show();
                if (listener != null) {
                    listener.onBookCreated(bookName, false);
                }
            }
        });

        cancel_button.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
