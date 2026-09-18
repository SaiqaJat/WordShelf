package com.saiqa.dictionary;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DictionaryDatabaseHelper extends SQLiteOpenHelper {

    private final Context context;
    private static final String DB_NAME = "Dictionary.db";
    private static final int DB_VERSION = 6;

    private static final String TABLE_NAME = "Book_Directory";
    private static final String BOOK_ID = "_id";
    private static final String COLUMN_TITLE = "book_title";
    private static final String COLUMN_AUTHOR = "book_author";
    private static final String COLUMN_CREATED_AT = "created_at";

    // Dictionary saved words DB
    private static final String TABLE_NAME_1 = "SavedWords";
    private static final String COLUMN_ID1 = "_id";
    private static final String COLUMN_WORDS = "words";
    private static final String COLUMN_PHONETIC = "phonetic";
    private static final String COLUMN_MEANINGS_JSON = "meanings_json";
    private static final String COLUMN_CHAPTER = "chapter";
    private static final String COLUMN_PAGE = "page";
    private static final String COLUMN_HIGHLIGHT = "highlight";
    private static final String COLUMN_NOTE = "note";

    // Search history DB
    private static final String TABLE_SEARCH_HISTORY = "SearchHistory";
    private static final String HISTORY_ID = "_id";
    private static final String HISTORY_WORD = "history_word";
    private static final String HISTORY_PHONETIC = "history_phonetic";
    private static final String HISTORY_TIMESTAMP = "history_timestamp";

    // Definition Cache table (v6)
    private static final String TABLE_DEF_CACHE = "DefinitionCache";
    private static final String CACHE_WORD = "word";
    private static final String CACHE_RESULT_JSON = "result_json";
    private static final String CACHE_CACHED_AT = "cached_at";
    private static final int CACHE_MAX_ENTRIES = 200;

    // Legacy columns for migration
    private static final String COLUMN_PARTS_OF_SPEECH = "partsOfSpeech";
    private static final String COLUMN_DEFINITIONS = "definitions";
    private static final String COLUMN_SYNONYMS = "synonyms";
    private static final String COLUMN_ANTONYMS = "antonyms";

    private final Gson gson;

    DictionaryDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
        this.gson = new Gson();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE " + TABLE_NAME + " (" +
                BOOK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_AUTHOR + " TEXT, " +
                COLUMN_CREATED_AT + " INTEGER);";
        db.execSQL(query);

        String query1 = "CREATE TABLE " + TABLE_NAME_1 + " (" +
                COLUMN_ID1 + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_WORDS + " TEXT, " +
                COLUMN_PHONETIC + " TEXT, " +
                COLUMN_MEANINGS_JSON + " TEXT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_CHAPTER + " TEXT, " +
                COLUMN_PAGE + " TEXT, " +
                COLUMN_HIGHLIGHT + " TEXT, " +
                COLUMN_NOTE + " TEXT);";
        db.execSQL(query1);

        String queryHistory = "CREATE TABLE " + TABLE_SEARCH_HISTORY + " (" +
                HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                HISTORY_WORD + " TEXT UNIQUE, " +
                HISTORY_PHONETIC + " TEXT, " +
                HISTORY_TIMESTAMP + " INTEGER);";
        db.execSQL(queryHistory);

        String queryDefCache = "CREATE TABLE " + TABLE_DEF_CACHE + " (" +
                CACHE_WORD + " TEXT PRIMARY KEY, " +
                CACHE_RESULT_JSON + " TEXT, " +
                CACHE_CACHED_AT + " INTEGER);";
        db.execSQL(queryDefCache);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            // Migrate from version 2 to 3 (grouping meanings)
            // 1. Create a new table
            String createNewTable = "CREATE TABLE SavedWords_new (" +
                    COLUMN_ID1 + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_WORDS + " TEXT, " +
                    COLUMN_PHONETIC + " TEXT, " +
                    COLUMN_MEANINGS_JSON + " TEXT, " +
                    COLUMN_TITLE + " TEXT);";
            db.execSQL(createNewTable);

            // 2. Read old data
            Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME_1, null);
            Map<String, Map<String, DictionaryModel>> migratedData = new HashMap<>(); // Book -> Word -> Model
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String bookTitle = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                    String word = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WORDS));
                    String partOfSpeech = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PARTS_OF_SPEECH));
                    String definitionsStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DEFINITIONS));
                    String synonymsStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SYNONYMS));
                    String antonymsStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ANTONYMS));
                    
                    // Reconstruct Meaning
                    Meaning meaning = new Meaning();
                    meaning.setPartOfSpeech(partOfSpeech);
                    
                    List<Definition> defsList = new ArrayList<>();
                    if (definitionsStr != null) {
                        for (String d : definitionsStr.split(";;")) {
                            if (!d.trim().isEmpty()) {
                                Definition def = new Definition(d.trim());
                                defsList.add(def);
                            }
                        }
                    }
                    meaning.setDefinitions(defsList);
                    
                    if (synonymsStr != null && !synonymsStr.isEmpty()) {
                        meaning.setSynonyms(Arrays.asList(synonymsStr.split(", ")));
                    }
                    if (antonymsStr != null && !antonymsStr.isEmpty()) {
                        meaning.setAntonyms(Arrays.asList(antonymsStr.split(", ")));
                    }

                    if (!migratedData.containsKey(bookTitle)) {
                        migratedData.put(bookTitle, new HashMap<>());
                    }
                    
                    Map<String, DictionaryModel> bookWords = migratedData.get(bookTitle);
                    String wordLower = word.toLowerCase();
                    
                    if (!bookWords.containsKey(wordLower)) {
                        List<Meaning> newMeaningsList = new ArrayList<>();
                        newMeaningsList.add(meaning);
                        DictionaryModel model = new DictionaryModel(word, "", newMeaningsList);
                        bookWords.put(wordLower, model);
                    } else {
                        bookWords.get(wordLower).getMeanings().add(meaning);
                    }

                } while (cursor.moveToNext());
                cursor.close();
            }

            // 3. Insert into new table
            for (Map.Entry<String, Map<String, DictionaryModel>> bookEntry : migratedData.entrySet()) {
                String bookTitle = bookEntry.getKey();
                for (DictionaryModel model : bookEntry.getValue().values()) {
                    ContentValues cv = new ContentValues();
                    cv.put(COLUMN_WORDS, model.getWord());
                    cv.put(COLUMN_PHONETIC, model.getPhonetic());
                    cv.put(COLUMN_MEANINGS_JSON, new Gson().toJson(model.getMeanings()));
                    cv.put(COLUMN_TITLE, bookTitle);
                    db.insert("SavedWords_new", null, cv);
                }
            }

            // 4. Drop old table and rename new one
            db.execSQL("DROP TABLE " + TABLE_NAME_1);
            db.execSQL("ALTER TABLE SavedWords_new RENAME TO " + TABLE_NAME_1);
        }

        if (oldVersion < 4) {
            String queryHistory = "CREATE TABLE IF NOT EXISTS " + TABLE_SEARCH_HISTORY + " (" +
                    HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    HISTORY_WORD + " TEXT UNIQUE, " +
                    HISTORY_PHONETIC + " TEXT, " +
                    HISTORY_TIMESTAMP + " INTEGER);";
            db.execSQL(queryHistory);
        }

        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_AUTHOR + " TEXT;");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_CREATED_AT + " INTEGER;");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE " + TABLE_NAME_1 + " ADD COLUMN " + COLUMN_CHAPTER + " TEXT;");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE " + TABLE_NAME_1 + " ADD COLUMN " + COLUMN_PAGE + " TEXT;");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE " + TABLE_NAME_1 + " ADD COLUMN " + COLUMN_HIGHLIGHT + " TEXT;");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE " + TABLE_NAME_1 + " ADD COLUMN " + COLUMN_NOTE + " TEXT;");
            } catch (Exception ignored) {}
        }

        if (oldVersion < 6) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_DEF_CACHE + " (" +
                    CACHE_WORD + " TEXT PRIMARY KEY, " +
                    CACHE_RESULT_JSON + " TEXT, " +
                    CACHE_CACHED_AT + " INTEGER);");
        }
    }

    // ── Definition Cache Methods (v6) ──────────────────────────────────────────

    /**
     * Saves (upserts) a word's definition result list to the persistent cache.
     * Automatically prunes oldest entries if the table exceeds CACHE_MAX_ENTRIES.
     */
    public void saveDefinitionCache(String word, List<WordResultData> data) {
        if (word == null || data == null || data.isEmpty()) return;
        String key = word.trim().toLowerCase();
        String json = gson.toJson(data);
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put(CACHE_WORD, key);
            cv.put(CACHE_RESULT_JSON, json);
            cv.put(CACHE_CACHED_AT, System.currentTimeMillis());
            db.insertWithOnConflict(TABLE_DEF_CACHE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            pruneDefinitionCacheInternal(db);
        } catch (Exception e) {
            Log.e("DB_CACHE", "Failed to save definition cache for: " + key, e);
        } finally {
            db.close();
        }
    }

    /**
     * Returns the cached definition list for {@code word}, or {@code null} if not cached.
     */
    public List<WordResultData> getDefinitionCache(String word) {
        if (word == null) return null;
        String key = word.trim().toLowerCase();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT " + CACHE_RESULT_JSON + " FROM " + TABLE_DEF_CACHE +
                    " WHERE " + CACHE_WORD + " = ? LIMIT 1",
                    new String[]{key});
            if (cursor != null && cursor.moveToFirst()) {
                String json = cursor.getString(0);
                if (json != null && !json.isEmpty()) {
                    Type type = new TypeToken<List<WordResultData>>(){}.getType();
                    return gson.fromJson(json, type);
                }
            }
        } catch (Exception e) {
            Log.e("DB_CACHE", "Failed to read definition cache for: " + key, e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return null;
    }

    /**
     * Deletes oldest cache entries beyond {@code maxEntries}.
     * Call this after a successful cache write to bound table size.
     */
    public void pruneDefinitionCache(int maxEntries) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            pruneDefinitionCacheInternal(db);
        } finally {
            db.close();
        }
    }

    private void pruneDefinitionCacheInternal(SQLiteDatabase db) {
        try {
            db.execSQL(
                "DELETE FROM " + TABLE_DEF_CACHE +
                " WHERE " + CACHE_WORD + " NOT IN (" +
                "  SELECT " + CACHE_WORD + " FROM " + TABLE_DEF_CACHE +
                "  ORDER BY " + CACHE_CACHED_AT + " DESC" +
                "  LIMIT " + CACHE_MAX_ENTRIES + ")"
            );
        } catch (Exception e) {
            Log.e("DB_CACHE", "Failed to prune definition cache", e);
        }
    }

    public boolean addBook(String title) {
        return addBook(title, null);
    }

    public boolean addBook(String title, String author) {
        if (title == null || title.trim().isEmpty()) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_TITLE, title.trim());
        cv.put(COLUMN_AUTHOR, author != null && !author.trim().isEmpty() ? author.trim() : null);
        cv.put(COLUMN_CREATED_AT, System.currentTimeMillis());

        long result = db.insert(TABLE_NAME, null, cv);
        db.close();
        return result != -1;
    }

    public String getBookAuthor(String title) {
        if (title == null) return null;
        SQLiteDatabase db = this.getReadableDatabase();
        String author = null;
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_AUTHOR + " FROM " + TABLE_NAME + " WHERE " + COLUMN_TITLE + " = ? LIMIT 1", new String[]{title.trim()});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                author = cursor.getString(0);
            }
            cursor.close();
        }
        db.close();
        return author;
    }

    public Long getBookCreatedAt(String title) {
        if (title == null) return null;
        SQLiteDatabase db = this.getReadableDatabase();
        Long createdAt = null;
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_CREATED_AT + " FROM " + TABLE_NAME + " WHERE " + COLUMN_TITLE + " = ? LIMIT 1", new String[]{title.trim()});
        if (cursor != null) {
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                createdAt = cursor.getLong(0);
            }
            cursor.close();
        }
        db.close();
        return createdAt;
    }
    
    Cursor readAllData() {
        String query = "SELECT * FROM " + TABLE_NAME;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        if(db != null){
            cursor = db.rawQuery(query,null);
        }
        return cursor;
    }

    public List<String> getAllBookNames() {
        List<String> bookList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);

        if (cursor.moveToFirst()) {
            do {
                String bookTitle = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                bookList.add(bookTitle);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return bookList;
    }

    void updateBooks(String old_title, String new_title) {
        updateBooks(old_title, new_title, null);
    }

    void updateBooks(String old_title, String new_title, String new_author) {
        if (old_title == null || new_title == null || new_title.trim().isEmpty()) return;
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_TITLE, new_title.trim());
            if (new_author != null) {
                values.put(COLUMN_AUTHOR, new_author.trim().isEmpty() ? null : new_author.trim());
            }

            db.update(TABLE_NAME, values, COLUMN_TITLE + " = ?", new String[]{old_title});

            ContentValues wordValues = new ContentValues();
            wordValues.put(COLUMN_TITLE, new_title.trim());
            db.update(TABLE_NAME_1, wordValues, COLUMN_TITLE + " = ?", new String[]{old_title});

            db.setTransactionSuccessful();
            Toast.makeText(context, "Updated Successfully!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, "Failed to update", Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    void deleteRow(String title) {
        if (title == null) return;
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_NAME, COLUMN_TITLE + " = ?", new String[]{title});
            db.delete(TABLE_NAME_1, COLUMN_TITLE + " = ?", new String[]{title});
            db.setTransactionSuccessful();
            Toast.makeText(context, "Deleted Successfully!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, "Failed to Delete", Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public Map<String, Integer> getBookWordCounts() {
        Map<String, Integer> countMap = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COLUMN_TITLE + ", COUNT(*) FROM " + TABLE_NAME_1 + " GROUP BY " + COLUMN_TITLE,
                null
        );
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String title = cursor.getString(0);
                    int count = cursor.getInt(1);
                    if (title != null) {
                        countMap.put(title, count);
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        db.close();
        return countMap;
    }

    public boolean addWords(String book_title, DictionaryModel model) {
        if (book_title == null || book_title.trim().isEmpty() || model == null || model.getWord() == null) {
            return false;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        boolean success = false;
        try {
            // First check if word already exists in this book
            Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME_1 + " WHERE LOWER(" + COLUMN_WORDS + ") = LOWER(?) AND " + COLUMN_TITLE + " = ?", new String[]{model.getWord().trim(), book_title.trim()});
            
            if (cursor != null && cursor.moveToFirst()) {
                // Update existing word
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID1));
                ContentValues values = new ContentValues();
                values.put(COLUMN_PHONETIC, model.getPhonetic());
                values.put(COLUMN_MEANINGS_JSON, gson.toJson(model.getMeanings()));
                if (model.getChapter() != null) values.put(COLUMN_CHAPTER, model.getChapter().trim());
                if (model.getPage() != null) values.put(COLUMN_PAGE, model.getPage().trim());
                if (model.getHighlight() != null) values.put(COLUMN_HIGHLIGHT, model.getHighlight().trim());
                if (model.getNote() != null) values.put(COLUMN_NOTE, model.getNote().trim());
                
                int rows = db.update(TABLE_NAME_1, values, COLUMN_ID1 + " = ?", new String[]{String.valueOf(id)});
                cursor.close();
                success = (rows > 0);
            } else {
                // Insert new word
                if (cursor != null) cursor.close();
                ContentValues values = new ContentValues();
                values.put(COLUMN_WORDS, model.getWord().trim());
                values.put(COLUMN_PHONETIC, model.getPhonetic());
                values.put(COLUMN_MEANINGS_JSON, gson.toJson(model.getMeanings()));
                values.put(COLUMN_TITLE, book_title.trim());
                values.put(COLUMN_CHAPTER, model.getChapter() != null ? model.getChapter().trim() : null);
                values.put(COLUMN_PAGE, model.getPage() != null ? model.getPage().trim() : null);
                values.put(COLUMN_HIGHLIGHT, model.getHighlight() != null ? model.getHighlight().trim() : null);
                values.put(COLUMN_NOTE, model.getNote() != null ? model.getNote().trim() : null);

                long result = db.insert(TABLE_NAME_1, null, values);
                success = (result != -1);
            }
        } catch (Exception e) {
            Log.e("DB_ERROR", "Error adding word to book", e);
            success = false;
        } finally {
            db.close();
        }
        return success;
    }

    public boolean updateWordReaderDetails(int id, String chapter, String page, String highlight, String note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CHAPTER, (chapter != null && !chapter.trim().isEmpty()) ? chapter.trim() : null);
        values.put(COLUMN_PAGE, (page != null && !page.trim().isEmpty()) ? page.trim() : null);
        values.put(COLUMN_HIGHLIGHT, (highlight != null && !highlight.trim().isEmpty()) ? highlight.trim() : null);
        values.put(COLUMN_NOTE, (note != null && !note.trim().isEmpty()) ? note.trim() : null);

        int rows = db.update(TABLE_NAME_1, values, COLUMN_ID1 + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return rows > 0;
    }

    public List<DictionaryModel> getSavedWords(String bookTitle) {
        List<DictionaryModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_NAME_1 + " WHERE " + COLUMN_TITLE + " = ?",
                new String[]{bookTitle}
        );

        if (cursor != null && cursor.moveToFirst()) {
            int chapterIdx = cursor.getColumnIndex(COLUMN_CHAPTER);
            int pageIdx = cursor.getColumnIndex(COLUMN_PAGE);
            int highlightIdx = cursor.getColumnIndex(COLUMN_HIGHLIGHT);
            int noteIdx = cursor.getColumnIndex(COLUMN_NOTE);

            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID1));
                String word = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WORDS));
                String phonetic = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONETIC));
                String meaningsJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEANINGS_JSON));
                
                String chapter = (chapterIdx >= 0 && !cursor.isNull(chapterIdx)) ? cursor.getString(chapterIdx) : null;
                String page = (pageIdx >= 0 && !cursor.isNull(pageIdx)) ? cursor.getString(pageIdx) : null;
                String highlight = (highlightIdx >= 0 && !cursor.isNull(highlightIdx)) ? cursor.getString(highlightIdx) : null;
                String note = (noteIdx >= 0 && !cursor.isNull(noteIdx)) ? cursor.getString(noteIdx) : null;

                Type type = new TypeToken<List<Meaning>>(){}.getType();
                List<Meaning> meanings = gson.fromJson(meaningsJson, type);

                list.add(new DictionaryModel(id, word, phonetic, meanings, chapter, page, highlight, note));
            } while (cursor.moveToNext());
            cursor.close();
        }

        db.close();
        return list;
    }

    void deleteWordSavedInBook(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        long result = db.delete(TABLE_NAME_1, COLUMN_ID1 + " = ?", new String[]{String.valueOf(id)});

        if (result == 0) {
            Toast.makeText(context, "Failed to Delete", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Deleted Successfully!", Toast.LENGTH_SHORT).show();
        }

        db.close();
    }

    public void addSearchHistory(String word, String phonetic) {
        if (word == null || word.trim().isEmpty()) return;
        String cleanWord = word.trim();
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(HISTORY_WORD, cleanWord);
        values.put(HISTORY_PHONETIC, phonetic != null ? phonetic : "");
        values.put(HISTORY_TIMESTAMP, System.currentTimeMillis());

        // Replace will insert or update if word already exists due to UNIQUE constraint on history_word
        db.insertWithOnConflict(TABLE_SEARCH_HISTORY, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public List<SearchHistoryItem> getRecentSearches(int limit) {
        List<SearchHistoryItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_SEARCH_HISTORY + " ORDER BY " + HISTORY_TIMESTAMP + " DESC LIMIT ?",
                new String[]{String.valueOf(limit)}
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String word = cursor.getString(cursor.getColumnIndexOrThrow(HISTORY_WORD));
                String phonetic = cursor.getString(cursor.getColumnIndexOrThrow(HISTORY_PHONETIC));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(HISTORY_TIMESTAMP));
                list.add(new SearchHistoryItem(word, phonetic, timestamp));
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return list;
    }

    public void deleteSearchHistoryItem(String word) {
        if (word == null) return;
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SEARCH_HISTORY, HISTORY_WORD + " = ?", new String[]{word.trim()});
        db.close();
    }

    public void clearSearchHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SEARCH_HISTORY, null, null);
        db.close();
    }
}
