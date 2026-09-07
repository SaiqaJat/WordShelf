package com.saiqa.dictionary;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment implements TextToSpeech.OnInitListener {

    private EditText search_input;
    private View search_icon;
    private ImageButton btn_clear_search, btn_audio_pronounce;
    private TextView word_textView, phonetic_textView;
    private ProgressBar progressBar;
    private Button save_to_book_btn;

    private View ll_word, card_word_header, empty_state_container, error_state_container, container_recent_searches, card_autocomplete;
    private TextView error_title, error_message, btn_clear_history;
    private ImageView error_icon;
    private Button btn_retry_search;

    private RecyclerView recyclerview_word_result, rv_recent_searches, rv_autocomplete;
    private WordResultAdapter adapter;
    private HistoryChipAdapter historyAdapter;
    private AutocompleteAdapter autocompleteAdapter;

    private DictionaryDatabaseHelper dictionaryDB;
    private List<Meaning> MeaningList = new ArrayList<>();

    private TextToSpeech tts;
    private boolean isTtsReady = false;

    private Call<List<WordResultData>> activeSearchCall;
    private Call<List<DatamuseSearchResult>> activeAutocompleteCall;

    private final Handler autocompleteHandler = new Handler(Looper.getMainLooper());
    private Runnable autocompleteRunnable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tts = new TextToSpeech(requireContext(), this);
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

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        dictionaryDB = new DictionaryDatabaseHelper(requireContext());

        search_input = view.findViewById(R.id.search_input);
        search_icon = view.findViewById(R.id.search_icon);
        btn_clear_search = view.findViewById(R.id.btn_clear_search);
        btn_audio_pronounce = view.findViewById(R.id.btn_audio_pronounce);
        ImageButton btn_share_searched_word = view.findViewById(R.id.btn_share_searched_word);
        word_textView = view.findViewById(R.id.word_textView);
        phonetic_textView = view.findViewById(R.id.phonetic_textView);
        progressBar = view.findViewById(R.id.progressBar);
        save_to_book_btn = view.findViewById(R.id.save_to_book_btn);
        
        recyclerview_word_result = view.findViewById(R.id.recyclerview_word_result);
        ll_word = view.findViewById(R.id.ll_word);
        card_word_header = view.findViewById(R.id.card_word_header);
        empty_state_container = view.findViewById(R.id.empty_state_container);
        
        error_state_container = view.findViewById(R.id.error_state_container);
        error_title = view.findViewById(R.id.error_title);
        error_message = view.findViewById(R.id.error_message);
        error_icon = view.findViewById(R.id.error_icon);
        btn_retry_search = view.findViewById(R.id.btn_retry_search);

        container_recent_searches = view.findViewById(R.id.container_recent_searches);
        btn_clear_history = view.findViewById(R.id.btn_clear_history);
        rv_recent_searches = view.findViewById(R.id.rv_recent_searches);

        card_autocomplete = view.findViewById(R.id.card_autocomplete);
        rv_autocomplete = view.findViewById(R.id.rv_autocomplete);

        recyclerview_word_result.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerview_word_result.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_animation_slide_up));

        // Setup Recent Searches
        rv_recent_searches.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        historyAdapter = new HistoryChipAdapter(new HistoryChipAdapter.OnHistoryInteractionListener() {
            @Override
            public void onHistoryClick(String word) {
                search_input.setText(word);
                performSearch();
                hideKeyboard();
            }

            @Override
            public void onHistoryDelete(String word) {
                dictionaryDB.deleteSearchHistoryItem(word);
                loadRecentSearches();
            }
        });
        rv_recent_searches.setAdapter(historyAdapter);

        if (btn_clear_history != null) {
            btn_clear_history.setOnClickListener(v -> {
                dictionaryDB.clearSearchHistory();
                loadRecentSearches();
            });
        }

        // Setup Autocomplete Adapter
        rv_autocomplete.setLayoutManager(new LinearLayoutManager(requireContext()));
        autocompleteAdapter = new AutocompleteAdapter(word -> {
            search_input.setText(word);
            hideAutocomplete();
            performSearch();
            hideKeyboard();
        });
        rv_autocomplete.setAdapter(autocompleteAdapter);

        loadRecentSearches();

        if (btn_share_searched_word != null) {
            btn_share_searched_word.setOnClickListener(v -> {
                String wordToShare = word_textView.getText().toString().trim();
                String phoneticToShare = phonetic_textView != null ? phonetic_textView.getText().toString().trim() : "";
                if (!wordToShare.isEmpty() && MeaningList != null && !MeaningList.isEmpty()) {
                    ShareUtils.shareMeanings(requireContext(), wordToShare, phoneticToShare, MeaningList);
                } else {
                    Toast.makeText(requireContext(), "Search for a word first to share", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Search Input TextWatcher for Autocomplete & Clear Button
        search_input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (btn_clear_search != null) {
                    btn_clear_search.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                }
                
                String query = s.toString().trim();
                if (query.length() >= 2) {
                    scheduleAutocomplete(query);
                } else {
                    hideAutocomplete();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (btn_clear_search != null) {
            btn_clear_search.setOnClickListener(v -> {
                search_input.setText("");
                hideAutocomplete();
                showIdleState();
            });
        }

        search_input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideAutocomplete();
                performSearch();
                hideKeyboard();
                return true;
            }
            return false;
        });

        search_icon.setOnClickListener(v -> {
            hideAutocomplete();
            performSearch();
            hideKeyboard();
        });

        btn_retry_search.setOnClickListener(v -> performSearch());

        if (btn_audio_pronounce != null) {
            btn_audio_pronounce.setOnClickListener(v -> {
                String wordToSpeak = word_textView.getText().toString().trim();
                if (!wordToSpeak.isEmpty() && isTtsReady && tts != null) {
                    tts.speak(wordToSpeak, TextToSpeech.QUEUE_FLUSH, null, "WordTTS");
                } else if (!isTtsReady) {
                    Toast.makeText(requireContext(), "Text-to-speech engine initializing...", Toast.LENGTH_SHORT).show();
                }
            });
        }

        save_to_book_btn.setOnClickListener(v -> {
            if (MeaningList != null && !MeaningList.isEmpty()) {
                String word = word_textView.getText().toString();
                String phonetic = phonetic_textView != null ? phonetic_textView.getText().toString() : "";
                DictionaryModel model = new DictionaryModel(word, phonetic, MeaningList);
                bottomBookSelectionSheet(requireContext(), model);
            } else {
                Toast.makeText(requireContext(), "Nothing to Save", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void loadRecentSearches() {
        List<SearchHistoryItem> recent = dictionaryDB.getRecentSearches(10);
        if (recent != null && !recent.isEmpty()) {
            historyAdapter.setItems(recent);
            container_recent_searches.setVisibility(View.VISIBLE);
        } else {
            container_recent_searches.setVisibility(View.GONE);
        }
    }

    private void scheduleAutocomplete(String query) {
        if (autocompleteRunnable != null) {
            autocompleteHandler.removeCallbacks(autocompleteRunnable);
        }
        autocompleteRunnable = () -> fetchAutocomplete(query);
        autocompleteHandler.postDelayed(autocompleteRunnable, 300);
    }

    private void fetchAutocomplete(String query) {
        if (activeAutocompleteCall != null) {
            activeAutocompleteCall.cancel();
        }

        activeAutocompleteCall = RetrofitInstance.setInstance().datamuseApi.getSuggestions(query);
        activeAutocompleteCall.enqueue(new Callback<List<DatamuseSearchResult>>() {
            @Override
            public void onResponse(@NonNull Call<List<DatamuseSearchResult>> call, @NonNull Response<List<DatamuseSearchResult>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<DatamuseSearchResult> list = response.body();
                    if (list.size() > 6) {
                        list = list.subList(0, 6);
                    }
                    autocompleteAdapter.setSuggestions(list);
                    if (card_autocomplete != null) card_autocomplete.setVisibility(View.VISIBLE);
                } else {
                    hideAutocomplete();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<DatamuseSearchResult>> call, @NonNull Throwable t) {
                hideAutocomplete();
            }
        });
    }

    private void hideAutocomplete() {
        if (autocompleteRunnable != null) {
            autocompleteHandler.removeCallbacks(autocompleteRunnable);
        }
        if (activeAutocompleteCall != null) {
            activeAutocompleteCall.cancel();
        }
        if (card_autocomplete != null) {
            card_autocomplete.setVisibility(View.GONE);
        }
    }

    private Call<List<DatamuseWordDetail>> activeDatamuseFallbackCall;

    private void performSearch() {
        String rawInput = search_input.getText().toString();
        if (rawInput == null || rawInput.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a word to search!", Toast.LENGTH_SHORT).show();
            return;
        }

        final String cleanWord = rawInput.trim().toLowerCase();

        hideAutocomplete();

        // 1. Check Cache first
        List<WordResultData> cachedResult = SearchCacheManager.getInstance().get(cleanWord);
        if (cachedResult != null && !cachedResult.isEmpty()) {
            displaySearchResult(cachedResult);
            dictionaryDB.addSearchHistory(cachedResult.get(0).getWord(), cachedResult.get(0).getPhonetic());
            loadRecentSearches();
            return;
        }

        // 2. Fetch via API
        if (activeSearchCall != null) {
            activeSearchCall.cancel();
        }
        if (activeDatamuseFallbackCall != null) {
            activeDatamuseFallbackCall.cancel();
        }

        progressBarVisibility(true);

        activeSearchCall = RetrofitInstance.setInstance().apiResponse.getWordMeaning(cleanWord);
        activeSearchCall.enqueue(new Callback<List<WordResultData>>() {
            @Override
            public void onResponse(@NonNull Call<List<WordResultData>> call, @NonNull Response<List<WordResultData>> response) {
                if (call.isCanceled()) return;

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    progressBarVisibility(false);
                    List<WordResultData> resultList = response.body();
                    SearchCacheManager.getInstance().put(cleanWord, resultList);

                    displaySearchResult(resultList);

                    WordResultData first = resultList.get(0);
                    dictionaryDB.addSearchHistory(first.getWord(), first.getPhonetic());
                    loadRecentSearches();

                } else {
                    // Primary API returned non-200 or empty -> Attempt Stage 2 Datamuse fallback
                    performDatamuseFallbackSearch(cleanWord);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<WordResultData>> call, @NonNull Throwable t) {
                if (call.isCanceled()) return;
                Log.w("PRIMARY_API_FAIL", "Primary API failed for " + cleanWord + ", attempting Datamuse fallback.", t);
                performDatamuseFallbackSearch(cleanWord);
            }
        });
    }

    private void performDatamuseFallbackSearch(String cleanWord) {
        activeDatamuseFallbackCall = RetrofitInstance.setInstance().datamuseApi.getWordDetails(cleanWord, "d", 1);
        activeDatamuseFallbackCall.enqueue(new Callback<List<DatamuseWordDetail>>() {
            @Override
            public void onResponse(@NonNull Call<List<DatamuseWordDetail>> call, @NonNull Response<List<DatamuseWordDetail>> response) {
                if (call.isCanceled()) return;
                progressBarVisibility(false);

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    WordResultData fallbackResult = convertDatamuseToWordResultData(cleanWord, response.body());
                    if (fallbackResult != null && fallbackResult.getMeanings() != null && !fallbackResult.getMeanings().isEmpty()) {
                        List<WordResultData> resultList = new ArrayList<>();
                        resultList.add(fallbackResult);
                        SearchCacheManager.getInstance().put(cleanWord, resultList);

                        displaySearchResult(resultList);
                        dictionaryDB.addSearchHistory(fallbackResult.getWord(), fallbackResult.getPhonetic());
                        loadRecentSearches();
                        return;
                    }
                }

                showErrorState(
                        "No definition found",
                        "We couldn't find a definition for \"" + cleanWord + "\". Please check the spelling and try again."
                );
            }

            @Override
            public void onFailure(@NonNull Call<List<DatamuseWordDetail>> call, @NonNull Throwable t) {
                if (call.isCanceled()) return;
                progressBarVisibility(false);
                Log.e("FALLBACK_API_FAIL", "Datamuse fallback failed for " + cleanWord, t);
                showErrorState(
                        "Unable to fetch definition",
                        "Please check your internet connection and try searching again."
                );
            }
        });
    }

    private WordResultData convertDatamuseToWordResultData(String searchedWord, List<DatamuseWordDetail> datamuseList) {
        if (datamuseList == null || datamuseList.isEmpty()) return null;
        DatamuseWordDetail detail = datamuseList.get(0);
        if (detail.getDefs() == null || detail.getDefs().isEmpty()) return null;

        java.util.Map<String, List<Definition>> meaningsMap = new java.util.HashMap<>();

        for (String rawDef : detail.getDefs()) {
            int tabIndex = rawDef.indexOf('\t');
            String pos = "noun";
            String defText = rawDef;
            if (tabIndex != -1) {
                String prefix = rawDef.substring(0, tabIndex).trim();
                defText = rawDef.substring(tabIndex + 1).trim();
                switch (prefix.toLowerCase()) {
                    case "n": pos = "noun"; break;
                    case "v": pos = "verb"; break;
                    case "adj": pos = "adjective"; break;
                    case "adv": pos = "adverb"; break;
                    default: pos = "general"; break;
                }
            }

            if (!meaningsMap.containsKey(pos)) {
                meaningsMap.put(pos, new ArrayList<>());
            }
            meaningsMap.get(pos).add(new Definition(defText));
        }

        List<Meaning> meaningList = new ArrayList<>();
        for (java.util.Map.Entry<String, List<Definition>> entry : meaningsMap.entrySet()) {
            Meaning m = new Meaning();
            m.setPartOfSpeech(entry.getKey());
            m.setDefinitions(entry.getValue());
            meaningList.add(m);
        }

        WordResultData result = new WordResultData();
        String formattedWord = searchedWord.length() > 0 ? searchedWord.substring(0, 1).toUpperCase() + searchedWord.substring(1) : searchedWord;
        result.setWord(detail.getWord() != null ? detail.getWord() : formattedWord);
        result.setMeanings(meaningList);
        return result;
    }

    private void displaySearchResult(List<WordResultData> responseList) {
        showResultState();

        WordResultData result = responseList.get(0);
        String wordStr = result.getWord();
        String phoneticStr = result.getPhonetic();
        List<Meaning> meaningList = result.getMeanings();

        if (wordStr != null && !wordStr.trim().isEmpty()) {
            word_textView.setText(wordStr);
            if (phonetic_textView != null) {
                if (phoneticStr != null && !phoneticStr.trim().isEmpty()) {
                    phonetic_textView.setText(phoneticStr);
                    phonetic_textView.setVisibility(View.VISIBLE);
                } else {
                    phonetic_textView.setVisibility(View.GONE);
                }
            }
            if (card_word_header != null) card_word_header.setVisibility(View.VISIBLE);
            save_to_book_btn.setVisibility(View.VISIBLE);
            ll_word.setVisibility(View.VISIBLE);
            recyclerview_word_result.setVisibility(View.VISIBLE);
        } else {
            word_textView.setText("");
            if (card_word_header != null) card_word_header.setVisibility(View.GONE);
            save_to_book_btn.setVisibility(View.GONE);
            ll_word.setVisibility(View.GONE);
            recyclerview_word_result.setVisibility(View.VISIBLE);
        }

        MeaningList = meaningList;
        adapter = new WordResultAdapter(requireContext(), meaningList, wordStr);
        recyclerview_word_result.setAdapter(adapter);
    }

    private void showIdleState() {
        if (empty_state_container != null) empty_state_container.setVisibility(View.VISIBLE);
        if (error_state_container != null) error_state_container.setVisibility(View.GONE);
        if (card_word_header != null) card_word_header.setVisibility(View.GONE);
        if (recyclerview_word_result != null) recyclerview_word_result.setVisibility(View.GONE);
        loadRecentSearches();
    }

    private void showResultState() {
        if (empty_state_container != null) empty_state_container.setVisibility(View.GONE);
        if (error_state_container != null) error_state_container.setVisibility(View.GONE);
        if (card_word_header != null) card_word_header.setVisibility(View.VISIBLE);
        if (recyclerview_word_result != null) recyclerview_word_result.setVisibility(View.VISIBLE);
    }

    private void showErrorState(String title, String message) {
        if (empty_state_container != null) empty_state_container.setVisibility(View.GONE);
        if (card_word_header != null) card_word_header.setVisibility(View.GONE);
        if (recyclerview_word_result != null) recyclerview_word_result.setVisibility(View.GONE);

        if (error_state_container != null) {
            error_state_container.setVisibility(View.VISIBLE);
            if (error_title != null) error_title.setText(title);
            if (error_message != null) error_message.setText(message);
        }
    }

    private void hideKeyboard() {
        if (getActivity() != null && getActivity().getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
            }
        }
    }

    @SuppressLint("MissingInflatedId")
    void bottomBookSelectionSheet(Context context, DictionaryModel wordModel) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_book_selection_dialogbox, null);
        bottomSheetDialog.setContentView(view);

        RecyclerView recyclerView = view.findViewById(R.id.bookRecyclerView);
        TextView titleText = view.findViewById(R.id.titleText);
        ImageButton imageButton = view.findViewById(R.id.add_book);
        LinearLayout linearLayout = view.findViewById(R.id.creatingBook_ll);

        dictionaryDB = new DictionaryDatabaseHelper(context);
        List<String> books = dictionaryDB.getAllBookNames();

        if (books.isEmpty()) {
            titleText.setVisibility(View.GONE);
            linearLayout.setVisibility(View.VISIBLE);
        } else {
            titleText.setVisibility(View.VISIBLE);
            linearLayout.setVisibility(View.GONE);
        }

        imageButton.setOnClickListener(v -> {
            Dialog dialog = new Dialog(requireContext());
            View dialogView = LayoutInflater.from(context).inflate(R.layout.add_update_books_layout, null);
            dialog.setContentView(dialogView);
            dialog.setCancelable(true);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            dialog.show();

            EditText edt_book_name = dialogView.findViewById(R.id.edt_book_name);
            Button add_book_btn = dialogView.findViewById(R.id.add_book_btn);
            Button cancel_button = dialogView.findViewById(R.id.cancel_button);

            add_book_btn.setOnClickListener(v1 -> {
                String bookName = edt_book_name.getText().toString().trim();
                if (!bookName.isEmpty()) {
                    dictionaryDB.addBook(bookName);
                    dialog.dismiss();
                    bottomSheetDialog.dismiss();
                } else {
                    Toast.makeText(requireContext(), "Enter Book Name", Toast.LENGTH_SHORT).show();
                }
            });
            cancel_button.setOnClickListener(v12 -> dialog.dismiss());
        });

        BookSelectionAdapter bookSelectionAdapter = new BookSelectionAdapter(books, bookName -> {
            dictionaryDB.addWords(bookName, wordModel);
            Toast.makeText(context, "Saved in " + bookName, Toast.LENGTH_SHORT).show();
            bottomSheetDialog.dismiss();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(bookSelectionAdapter);
        bottomSheetDialog.show();
    }

    private void progressBarVisibility(Boolean inProgress) {
        if (inProgress) {
            search_icon.setVisibility(View.GONE);
            if (btn_clear_search != null) btn_clear_search.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            search_icon.setVisibility(View.VISIBLE);
            if (btn_clear_search != null && search_input != null && search_input.getText().length() > 0) {
                btn_clear_search.setVisibility(View.VISIBLE);
            }
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (activeSearchCall != null) {
            activeSearchCall.cancel();
        }
        if (activeAutocompleteCall != null) {
            activeAutocompleteCall.cancel();
        }
        if (activeDatamuseFallbackCall != null) {
            activeDatamuseFallbackCall.cancel();
        }
        super.onDestroy();
    }
}
