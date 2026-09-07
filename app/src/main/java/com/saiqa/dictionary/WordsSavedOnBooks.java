package com.saiqa.dictionary;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class WordsSavedOnBooks extends AppCompatActivity implements TextToSpeech.OnInitListener {
    RecyclerView saved_words_recyclerview;
    View empty_saved_words_container;
    ImageButton btnExportPdfAll, btnShareBookAll;

    SavedWordsAdapter savedWordsAdapter;
    DictionaryDatabaseHelper dictionaryDB;
    List<DictionaryModel> savedWordsList;
    String bookTitle = "Saved Words";

    private TextToSpeech tts;
    private boolean isTtsReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_word_saved_on_books);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_saved_words_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tts = new TextToSpeech(this, this);

        Toolbar toolbar = findViewById(R.id.customToolbar);
        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        btnExportPdfAll = findViewById(R.id.btn_export_pdf_all);
        btnShareBookAll = findViewById(R.id.btn_share_book_all);

        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        Intent fromAct = getIntent();
        if (fromAct.hasExtra("book_title")) {
            String titleExtra = fromAct.getStringExtra("book_title");
            if (titleExtra != null && !titleExtra.isEmpty()) {
                bookTitle = titleExtra;
            }
        }
        if (toolbarTitle != null) {
            toolbarTitle.setText(bookTitle);
        }


        saved_words_recyclerview = findViewById(R.id.saved_words_recyclerview);
        empty_saved_words_container = findViewById(R.id.empty_saved_words_container);
        saved_words_recyclerview.setLayoutManager(new LinearLayoutManager(this));

        dictionaryDB = new DictionaryDatabaseHelper(this);
        savedWordsList = dictionaryDB.getSavedWords(bookTitle);

        checkEmptyState();

        if (btnExportPdfAll != null) {
            btnExportPdfAll.setOnClickListener(v -> {
                if (savedWordsList != null && !savedWordsList.isEmpty()) {
                    PdfExporter.exportBookCollection(this, bookTitle, savedWordsList);
                } else {
                    Toast.makeText(this, "No words in this book to export", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        if (btnShareBookAll != null) {
            btnShareBookAll.setOnClickListener(v -> {
                if (savedWordsList != null && !savedWordsList.isEmpty()) {
                    ShareUtils.shareBook(this, bookTitle, savedWordsList);
                } else {
                    Toast.makeText(this, "No words in this book to share", Toast.LENGTH_SHORT).show();
                }
            });
        }

        savedWordsAdapter = new SavedWordsAdapter(savedWordsList, word -> {
            if (!word.isEmpty() && isTtsReady && tts != null) {
                tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, "SavedWordTTS");
            } else if (!isTtsReady) {
                Toast.makeText(this, "Text-to-speech initializing...", Toast.LENGTH_SHORT).show();
            }
        });

        savedWordsAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                checkEmptyState();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                checkEmptyState();
            }
        });

        saved_words_recyclerview.setAdapter(savedWordsAdapter);
    }

    private void checkEmptyState() {
        boolean isEmpty = (savedWordsList == null || savedWordsList.isEmpty());
        if (empty_saved_words_container != null) {
            empty_saved_words_container.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        if (saved_words_recyclerview != null) {
            saved_words_recyclerview.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
        if (btnExportPdfAll != null) {
            btnExportPdfAll.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
        if (btnShareBookAll != null) {
            btnShareBookAll.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }


    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsReady = true;
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}