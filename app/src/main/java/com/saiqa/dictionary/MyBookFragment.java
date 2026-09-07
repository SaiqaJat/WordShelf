package com.saiqa.dictionary;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;

public class MyBookFragment extends Fragment {

    View book_add_FAB;
    RecyclerView book_fragment_recyclerview;
    View empty_books_container;

    DictionaryDatabaseHelper dictionaryDB;

    ArrayList<String> bookTitles;
    ArrayList<String> bookIds;

    BooksRecyclerAdapter booksRecyclerAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_book, container, false);

        book_add_FAB = view.findViewById(R.id.book_add_FAB);
        book_fragment_recyclerview = view.findViewById(R.id.book_fragment_recyclerview);
        empty_books_container = view.findViewById(R.id.empty_books_container);

        dictionaryDB = new DictionaryDatabaseHelper(requireContext());
        bookTitles = new ArrayList<>();
        bookIds = new ArrayList<>();

        DisplayBooksData();

        int columns = getResources().getInteger(R.integer.book_grid_columns);
        booksRecyclerAdapter = new BooksRecyclerAdapter(requireContext(), bookTitles);
        book_fragment_recyclerview.setAdapter(booksRecyclerAdapter);
        book_fragment_recyclerview.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(requireContext(), columns));

        try {
            book_fragment_recyclerview.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_animation_slide_in));
            book_fragment_recyclerview.scheduleLayoutAnimation();
        } catch (Exception ignored) {}

        book_add_FAB.setOnClickListener(v -> {
            Dialog dialog = new Dialog(requireContext());
            dialog.setContentView(R.layout.add_update_books_layout);
            dialog.setCancelable(true);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            dialog.show();

            EditText edt_book_name = dialog.findViewById(R.id.edt_book_name);
            Button add_book_btn = dialog.findViewById(R.id.add_book_btn);
            Button cancel_button = dialog.findViewById(R.id.cancel_button);

            add_book_btn.setOnClickListener(view1 -> {
                String bookName = edt_book_name.getText().toString().trim();
                if (!bookName.isEmpty()) {
                    dictionaryDB.addBook(bookName);
                    dialog.dismiss();
                    reloadData();
                } else {
                    Toast.makeText(requireContext(), "Enter Book Name", Toast.LENGTH_SHORT).show();
                }
            });

            cancel_button.setOnClickListener(view12 -> dialog.dismiss());
        });

        EditText searchBooksInput = view.findViewById(R.id.search_books_input);
        if (searchBooksInput != null) {
            searchBooksInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterBooks(s.toString().trim());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        return view;
    }

    private void filterBooks(String query) {
        DisplayBooksData();
        if (!query.isEmpty()) {
            ArrayList<String> filtered = new ArrayList<>();
            for (String title : bookTitles) {
                if (title.toLowerCase().contains(query.toLowerCase())) {
                    filtered.add(title);
                }
            }
            bookTitles.clear();
            bookTitles.addAll(filtered);
        }
        if (booksRecyclerAdapter != null) {
            booksRecyclerAdapter.notifyDataSetChanged();
        }
        checkEmptyState();
    }

    private void DisplayBooksData() {
        Cursor cursor = dictionaryDB.readAllData();
        bookTitles.clear();
        bookIds.clear();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                bookIds.add(cursor.getString(0));
                bookTitles.add(cursor.getString(1));
            }
            cursor.close();
        }

        checkEmptyState();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void reloadData() {
        DisplayBooksData();
        if (booksRecyclerAdapter != null) {
            booksRecyclerAdapter.notifyDataSetChanged();
            if (!bookTitles.isEmpty()) {
                book_fragment_recyclerview.scrollToPosition(bookTitles.size() - 1);
            }
        }
    }

    private void checkEmptyState() {
        if (empty_books_container != null) {
            empty_books_container.setVisibility(bookTitles.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (book_fragment_recyclerview != null) {
            book_fragment_recyclerview.setVisibility(bookTitles.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadData();
    }
}
