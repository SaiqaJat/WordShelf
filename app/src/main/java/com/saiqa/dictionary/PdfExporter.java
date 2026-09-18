package com.saiqa.dictionary;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PdfExporter {

    private static final String TAG = "PdfExporter";

    // Standard A4 dimensions in points (72 points per inch)
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 48;
    private static final int USABLE_WIDTH = PAGE_WIDTH - (MARGIN * 2);

    // Literary Print-Friendly Color Palette
    private static final int COLOR_INK_PRIMARY = Color.parseColor("#1E293B");      // Deep charcoal ink
    private static final int COLOR_INK_SECONDARY = Color.parseColor("#475569");    // Subtitle & secondary
    private static final int COLOR_INK_MUTED = Color.parseColor("#64748B");        // Meta & details
    private static final int COLOR_BRONZE_ACCENT = Color.parseColor("#8E6E53");    // Warm literary bronze
    private static final int COLOR_BORDER_HAIRLINE = Color.parseColor("#E2E8F0");  // Clean subtle rule
    private static final int COLOR_HIGHLIGHT_BG = Color.parseColor("#F8F6F2");     // Subtle warm quote tint
    private static final int COLOR_BADGE_BG = Color.parseColor("#F1F5F9");         // POS badge pill
    private static final int COLOR_BADGE_TEXT = Color.parseColor("#2C3E50");       // POS badge label

    public static void exportSingleWord(Context context, DictionaryModel model) {
        if (model == null) return;
        List<DictionaryModel> list = new ArrayList<>();
        list.add(model);
        String title = (model.getWord() != null && !model.getWord().trim().isEmpty())
                ? model.getWord().trim()
                : "Vocabulary Word";
        exportBookCollection(context, title, list);
    }

    public static void exportBookCollection(Context context, String collectionTitle, List<DictionaryModel> wordList) {
        if (wordList == null || wordList.isEmpty()) {
            Toast.makeText(context, "No saved words to export as PDF", Toast.LENGTH_SHORT).show();
            return;
        }

        // Query book metadata from database
        DictionaryDatabaseHelper db = new DictionaryDatabaseHelper(context);
        String bookAuthor = db.getBookAuthor(collectionTitle);
        Long bookCreatedAt = db.getBookCreatedAt(collectionTitle);

        // Sort collection alphabetically
        List<DictionaryModel> sortedList = new ArrayList<>(wordList);
        Collections.sort(sortedList, (a, b) -> {
            String w1 = a.getWord() != null ? a.getWord().trim() : "";
            String w2 = b.getWord() != null ? b.getWord().trim() : "";
            return w1.compareToIgnoreCase(w2);
        });

        // --- TYPOGRAPHY & PAINTS ---
        Typeface serifBold = Typeface.create(Typeface.SERIF, Typeface.BOLD);
        Typeface serifNormal = Typeface.create(Typeface.SERIF, Typeface.NORMAL);
        Typeface serifItalic = Typeface.create(Typeface.SERIF, Typeface.ITALIC);
        Typeface sansBold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
        Typeface sansNormal = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

        TextPaint brandPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        brandPaint.setColor(COLOR_BRONZE_ACCENT);
        brandPaint.setTypeface(sansBold);
        brandPaint.setTextSize(10f);
        brandPaint.setTextAlign(Paint.Align.CENTER);

        TextPaint coverTitlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        coverTitlePaint.setColor(COLOR_INK_PRIMARY);
        coverTitlePaint.setTypeface(serifBold);
        coverTitlePaint.setTextSize(28f);
        coverTitlePaint.setTextAlign(Paint.Align.CENTER);

        TextPaint coverSubPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        coverSubPaint.setColor(COLOR_INK_SECONDARY);
        coverSubPaint.setTypeface(serifItalic);
        coverSubPaint.setTextSize(14f);
        coverSubPaint.setTextAlign(Paint.Align.CENTER);

        TextPaint coverMetaPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        coverMetaPaint.setColor(COLOR_INK_PRIMARY);
        coverMetaPaint.setTypeface(sansNormal);
        coverMetaPaint.setTextSize(11.5f);
        coverMetaPaint.setTextAlign(Paint.Align.CENTER);

        TextPaint sectionTitlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        sectionTitlePaint.setColor(COLOR_INK_PRIMARY);
        sectionTitlePaint.setTypeface(serifBold);
        sectionTitlePaint.setTextSize(19f);

        TextPaint sectionSubPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        sectionSubPaint.setColor(COLOR_INK_MUTED);
        sectionSubPaint.setTypeface(serifItalic);
        sectionSubPaint.setTextSize(11f);

        TextPaint labelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(COLOR_BRONZE_ACCENT);
        labelPaint.setTypeface(sansBold);
        labelPaint.setTextSize(8.5f);

        TextPaint wordTitlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        wordTitlePaint.setColor(COLOR_INK_PRIMARY);
        wordTitlePaint.setTypeface(serifBold);
        wordTitlePaint.setTextSize(16f);

        TextPaint phoneticPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        phoneticPaint.setColor(COLOR_INK_MUTED);
        phoneticPaint.setTypeface(serifItalic);
        phoneticPaint.setTextSize(11.5f);

        TextPaint posBadgePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        posBadgePaint.setColor(COLOR_BADGE_TEXT);
        posBadgePaint.setTypeface(sansBold);
        posBadgePaint.setTextSize(8.5f);

        TextPaint bodyPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(COLOR_INK_PRIMARY);
        bodyPaint.setTypeface(serifNormal);
        bodyPaint.setTextSize(10.5f);

        TextPaint examplePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        examplePaint.setColor(COLOR_INK_SECONDARY);
        examplePaint.setTypeface(serifItalic);
        examplePaint.setTextSize(10f);

        TextPaint highlightPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setColor(COLOR_INK_PRIMARY);
        highlightPaint.setTypeface(serifItalic);
        highlightPaint.setTextSize(10.5f);

        TextPaint notePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        notePaint.setColor(COLOR_INK_PRIMARY);
        notePaint.setTypeface(serifNormal);
        notePaint.setTextSize(10f);

        TextPaint metaPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        metaPaint.setColor(COLOR_INK_MUTED);
        metaPaint.setTypeface(sansNormal);
        metaPaint.setTextSize(9.5f);

        TextPaint headerFooterPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        headerFooterPaint.setColor(COLOR_INK_MUTED);
        headerFooterPaint.setTypeface(sansNormal);
        headerFooterPaint.setTextSize(9f);

        Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dividerPaint.setColor(COLOR_BORDER_HAIRLINE);
        dividerPaint.setStrokeWidth(0.8f);

        Paint bronzeLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bronzeLinePaint.setColor(COLOR_BRONZE_ACCENT);
        bronzeLinePaint.setStrokeWidth(1.2f);

        Paint badgeBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgeBgPaint.setColor(COLOR_BADGE_BG);
        badgeBgPaint.setStyle(Paint.Style.FILL);

        Paint highlightBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightBgPaint.setColor(COLOR_HIGHLIGHT_BG);
        highlightBgPaint.setStyle(Paint.Style.FILL);

        // =========================================================================
        // PASS 1: PRE-PAGINATION & SMART PLANNING
        // =========================================================================

        // Page 1: Cover Page
        // Page 2: Book Information Page
        // Page 3..X: Word Index
        // Page (X+1)..Y: Detailed Vocabulary Entries
        // Page (Y+1)..Z: Words at a Glance
        // Page (Z+1): Final Closing Page

        // 1. Calculate Index Pagination
        // Group words by first letter
        Map<Character, List<DictionaryModel>> letterGroups = new LinkedHashMap<>();
        for (DictionaryModel model : sortedList) {
            String word = model.getWord() != null ? model.getWord().trim() : "";
            char firstLetter = word.isEmpty() ? '#' : Character.toUpperCase(word.charAt(0));
            if (!letterGroups.containsKey(firstLetter)) {
                letterGroups.put(firstLetter, new ArrayList<>());
            }
            letterGroups.get(firstLetter).add(model);
        }

        int indexUsableHeight = PAGE_HEIGHT - (MARGIN * 2) - 80;
        int estimatedIndexHeight = 0;
        for (Map.Entry<Character, List<DictionaryModel>> entry : letterGroups.entrySet()) {
            estimatedIndexHeight += 26; // Letter header
            estimatedIndexHeight += (entry.getValue().size() * 18); // Word row
            estimatedIndexHeight += 8; // Spacing
        }
        int numIndexPages = Math.max(1, (int) Math.ceil((double) estimatedIndexHeight / indexUsableHeight));

        int firstEntryPage = 2 + numIndexPages + 1;

        // 2. Simulate Detailed Vocabulary Entries to determine exact starting page for each word
        int simPage = firstEntryPage;
        int simY = MARGIN + 40;
        int contentMaxY = PAGE_HEIGHT - MARGIN - 40;

        Map<String, Integer> wordPageMap = new HashMap<>();

        for (int i = 0; i < sortedList.size(); i++) {
            DictionaryModel item = sortedList.get(i);
            int entryHeight = calculateEntryHeight(item, wordTitlePaint, bodyPaint, examplePaint, highlightPaint, notePaint, metaPaint, USABLE_WIDTH);

            if (simY + entryHeight > contentMaxY) {
                simPage++;
                simY = MARGIN + 40;
            }

            String wordKey = item.getWord() != null ? item.getWord().trim().toLowerCase(Locale.US) : ("word_" + i);
            wordPageMap.put(wordKey, simPage);
            simY += entryHeight + 18; // Spacing between entries
        }

        int lastEntryPage = simPage;

        // 3. Simulate Words at a Glance Pagination
        int glanceStartPage = lastEntryPage + 1;
        int glanceRowHeight = 22;
        int glanceUsableHeight = PAGE_HEIGHT - (MARGIN * 2) - 80;
        int totalGlanceRowsHeight = sortedList.size() * glanceRowHeight;
        int numGlancePages = Math.max(1, (int) Math.ceil((double) totalGlanceRowsHeight / glanceUsableHeight));
        int lastGlancePage = glanceStartPage + numGlancePages - 1;

        // 4. Final Closing Page
        int finalClosingPage = lastGlancePage + 1;
        int totalDocumentPages = finalClosingPage;

        // =========================================================================
        // PASS 2: RENDER COMPLETE PDF DOCUMENT
        // =========================================================================
        PdfDocument pdfDocument = new PdfDocument();

        // -------------------------------------------------------------------------
        // 1. ELEGANT COVER PAGE (Page 1)
        // -------------------------------------------------------------------------
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        canvas.drawColor(Color.WHITE);

        // Subtle Framing
        canvas.drawLine(MARGIN + 20, MARGIN + 20, PAGE_WIDTH - MARGIN - 20, MARGIN + 20, dividerPaint);
        canvas.drawLine(MARGIN + 20, PAGE_HEIGHT - MARGIN - 20, PAGE_WIDTH - MARGIN - 20, PAGE_HEIGHT - MARGIN - 20, dividerPaint);

        // Brand
        canvas.drawText("W O R D S H E L F", PAGE_WIDTH / 2f, PAGE_HEIGHT * 0.18f, brandPaint);

        // Title Block
        canvas.drawText("MY VOCABULARY", PAGE_WIDTH / 2f, PAGE_HEIGHT * 0.28f, coverSubPaint);
        canvas.drawText("A Personal Collection", PAGE_WIDTH / 2f, PAGE_HEIGHT * 0.31f, sectionSubPaint);

        canvas.drawLine(PAGE_WIDTH / 2f - 40, PAGE_HEIGHT * 0.34f, PAGE_WIDTH / 2f + 40, PAGE_HEIGHT * 0.34f, bronzeLinePaint);

        // Book / Collection Title
        canvas.drawText(collectionTitle, PAGE_WIDTH / 2f, PAGE_HEIGHT * 0.42f, coverTitlePaint);

        float currentCoverY = PAGE_HEIGHT * 0.46f;
        if (bookAuthor != null && !bookAuthor.trim().isEmpty()) {
            canvas.drawText("by " + bookAuthor.trim(), PAGE_WIDTH / 2f, currentCoverY, coverSubPaint);
            currentCoverY += 28;
        }

        // Word Count Badge
        String countStr = sortedList.size() == 1 ? "1 Word Collected" : sortedList.size() + " Words Collected";
        canvas.drawText(countStr, PAGE_WIDTH / 2f, currentCoverY + 10, coverMetaPaint);

        // Footer block on cover
        String exportDateStr = new SimpleDateFormat("MMMM d, yyyy", Locale.US).format(new Date());
        canvas.drawText("Created with WordShelf", PAGE_WIDTH / 2f, PAGE_HEIGHT - MARGIN - 50, brandPaint);
        canvas.drawText(exportDateStr, PAGE_WIDTH / 2f, PAGE_HEIGHT - MARGIN - 36, headerFooterPaint);

        pdfDocument.finishPage(page);

        // -------------------------------------------------------------------------
        // 2. BOOK INFORMATION PAGE (Page 2)
        // -------------------------------------------------------------------------
        pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create();
        page = pdfDocument.startPage(pageInfo);
        canvas = page.getCanvas();
        canvas.drawColor(Color.WHITE);

        drawHeader(canvas, collectionTitle, MARGIN, headerFooterPaint, dividerPaint);
        drawFooter(canvas, 2, totalDocumentPages, MARGIN, headerFooterPaint);

        float infoY = MARGIN + 40;
        canvas.drawText("BOOK INFORMATION", MARGIN, infoY, sectionTitlePaint);
        canvas.drawText("A snapshot of this personal reading journal", MARGIN, infoY + 16, sectionSubPaint);
        canvas.drawLine(MARGIN, infoY + 28, PAGE_WIDTH - MARGIN, infoY + 28, dividerPaint);

        infoY += 60;
        drawInfoRow(canvas, "TITLE", collectionTitle, MARGIN, infoY, labelPaint, bodyPaint, dividerPaint);
        infoY += 56;

        if (bookAuthor != null && !bookAuthor.trim().isEmpty()) {
            drawInfoRow(canvas, "AUTHOR", bookAuthor.trim(), MARGIN, infoY, labelPaint, bodyPaint, dividerPaint);
            infoY += 56;
        }

        drawInfoRow(canvas, "WORDS COLLECTED", String.valueOf(sortedList.size()), MARGIN, infoY, labelPaint, bodyPaint, dividerPaint);
        infoY += 56;

        if (bookCreatedAt != null && bookCreatedAt > 0) {
            String createdStr = new SimpleDateFormat("MMMM d, yyyy", Locale.US).format(new Date(bookCreatedAt));
            drawInfoRow(canvas, "CREATED", createdStr, MARGIN, infoY, labelPaint, bodyPaint, dividerPaint);
            infoY += 56;
        }

        drawInfoRow(canvas, "EXPORTED", exportDateStr, MARGIN, infoY, labelPaint, bodyPaint, dividerPaint);

        pdfDocument.finishPage(page);

        // -------------------------------------------------------------------------
        // 3. WORD INDEX (Page 3 to 2 + numIndexPages)
        // -------------------------------------------------------------------------
        int indexCurrentPage = 3;
        pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, indexCurrentPage).create();
        page = pdfDocument.startPage(pageInfo);
        canvas = page.getCanvas();
        canvas.drawColor(Color.WHITE);

        drawHeader(canvas, collectionTitle, MARGIN, headerFooterPaint, dividerPaint);
        drawFooter(canvas, indexCurrentPage, totalDocumentPages, MARGIN, headerFooterPaint);

        float indexY = MARGIN + 40;
        canvas.drawText("WORD INDEX", MARGIN, indexY, sectionTitlePaint);
        canvas.drawText("Alphabetical index of words and their location", MARGIN, indexY + 16, sectionSubPaint);
        canvas.drawLine(MARGIN, indexY + 28, PAGE_WIDTH - MARGIN, indexY + 28, dividerPaint);

        indexY += 50;

        TextPaint indexLetterPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        indexLetterPaint.setColor(COLOR_BRONZE_ACCENT);
        indexLetterPaint.setTypeface(serifBold);
        indexLetterPaint.setTextSize(14f);

        TextPaint dotPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(COLOR_BORDER_HAIRLINE);
        dotPaint.setTypeface(sansNormal);
        dotPaint.setTextSize(9.5f);

        for (Map.Entry<Character, List<DictionaryModel>> entry : letterGroups.entrySet()) {
            int groupHeight = 24 + (entry.getValue().size() * 18);
            if (indexY + groupHeight > PAGE_HEIGHT - MARGIN - 40) {
                pdfDocument.finishPage(page);
                indexCurrentPage++;
                pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, indexCurrentPage).create();
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                canvas.drawColor(Color.WHITE);

                drawHeader(canvas, collectionTitle, MARGIN, headerFooterPaint, dividerPaint);
                drawFooter(canvas, indexCurrentPage, totalDocumentPages, MARGIN, headerFooterPaint);

                indexY = MARGIN + 40;
            }

            // Letter Header
            canvas.drawText(String.valueOf(entry.getKey()), MARGIN, indexY, indexLetterPaint);
            canvas.drawLine(MARGIN + 18, indexY - 4, MARGIN + 60, indexY - 4, dividerPaint);
            indexY += 20;

            // Words under letter
            for (DictionaryModel model : entry.getValue()) {
                String word = model.getWord() != null ? model.getWord().trim() : "";
                String wordKey = word.toLowerCase(Locale.US);
                int targetPage = wordPageMap.containsKey(wordKey) ? wordPageMap.get(wordKey) : firstEntryPage;

                canvas.drawText(word, MARGIN + 12, indexY, bodyPaint);
                String pageNumStr = String.valueOf(targetPage);
                float pageStrWidth = bodyPaint.measureText(pageNumStr);
                canvas.drawText(pageNumStr, PAGE_WIDTH - MARGIN - pageStrWidth, indexY, bodyPaint);

                // Dot Leader line
                float wordWidth = bodyPaint.measureText(word);
                float dotStart = MARGIN + 12 + wordWidth + 12;
                float dotEnd = PAGE_WIDTH - MARGIN - pageStrWidth - 12;

                if (dotEnd > dotStart) {
                    StringBuilder dots = new StringBuilder();
                    while (dotPaint.measureText(dots.toString() + ". ") < (dotEnd - dotStart)) {
                        dots.append(". ");
                    }
                    canvas.drawText(dots.toString(), dotStart, indexY, dotPaint);
                }

                indexY += 18;
            }

            indexY += 10;
        }

        pdfDocument.finishPage(page);

        // -------------------------------------------------------------------------
        // 4. DETAILED VOCABULARY ENTRIES
        // -------------------------------------------------------------------------
        int currentContentPage = firstEntryPage;
        pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentContentPage).create();
        page = pdfDocument.startPage(pageInfo);
        canvas = page.getCanvas();
        canvas.drawColor(Color.WHITE);

        drawHeader(canvas, collectionTitle, MARGIN, headerFooterPaint, dividerPaint);
        drawFooter(canvas, currentContentPage, totalDocumentPages, MARGIN, headerFooterPaint);

        float currentY = MARGIN + 40;

        for (int i = 0; i < sortedList.size(); i++) {
            DictionaryModel item = sortedList.get(i);
            int entryHeight = calculateEntryHeight(item, wordTitlePaint, bodyPaint, examplePaint, highlightPaint, notePaint, metaPaint, USABLE_WIDTH);

            if (currentY + entryHeight > contentMaxY) {
                pdfDocument.finishPage(page);
                currentContentPage++;

                pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentContentPage).create();
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                canvas.drawColor(Color.WHITE);

                drawHeader(canvas, collectionTitle, MARGIN, headerFooterPaint, dividerPaint);
                drawFooter(canvas, currentContentPage, totalDocumentPages, MARGIN, headerFooterPaint);

                currentY = MARGIN + 40;
            }

            // Draw single vocabulary entry
            currentY = drawVocabularyEntry(canvas, i + 1, item, currentY, MARGIN, USABLE_WIDTH,
                    wordTitlePaint, phoneticPaint, posBadgePaint, bodyPaint, examplePaint,
                    highlightPaint, notePaint, metaPaint, labelPaint, dividerPaint, badgeBgPaint, highlightBgPaint, bronzeLinePaint);

            currentY += 18; // Space before next entry
        }

        pdfDocument.finishPage(page);

        // -------------------------------------------------------------------------
        // 5. WORDS AT A GLANCE
        // -------------------------------------------------------------------------
        int currentGlancePage = glanceStartPage;
        pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentGlancePage).create();
        page = pdfDocument.startPage(pageInfo);
        canvas = page.getCanvas();
        canvas.drawColor(Color.WHITE);

        drawHeader(canvas, collectionTitle, MARGIN, headerFooterPaint, dividerPaint);
        drawFooter(canvas, currentGlancePage, totalDocumentPages, MARGIN, headerFooterPaint);

        float glanceY = MARGIN + 40;
        canvas.drawText("WORDS AT A GLANCE", MARGIN, glanceY, sectionTitlePaint);
        canvas.drawText("A quick revision list of all saved vocabulary", MARGIN, glanceY + 16, sectionSubPaint);
        canvas.drawLine(MARGIN, glanceY + 28, PAGE_WIDTH - MARGIN, glanceY + 28, dividerPaint);

        glanceY += 46;

        for (int i = 0; i < sortedList.size(); i++) {
            if (glanceY + glanceRowHeight > PAGE_HEIGHT - MARGIN - 40) {
                pdfDocument.finishPage(page);
                currentGlancePage++;

                pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentGlancePage).create();
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                canvas.drawColor(Color.WHITE);

                drawHeader(canvas, collectionTitle, MARGIN, headerFooterPaint, dividerPaint);
                drawFooter(canvas, currentGlancePage, totalDocumentPages, MARGIN, headerFooterPaint);

                glanceY = MARGIN + 40;
            }

            DictionaryModel model = sortedList.get(i);
            String wordStr = model.getWord() != null ? model.getWord().trim() : "";
            String posStr = "";
            if (model.getMeanings() != null && !model.getMeanings().isEmpty()) {
                String rawPos = model.getMeanings().get(0).getPartOfSpeech();
                if (rawPos != null && !rawPos.trim().isEmpty()) {
                    posStr = rawPos.substring(0, 1).toUpperCase(Locale.US) + rawPos.substring(1).toLowerCase(Locale.US);
                }
            }

            canvas.drawText(wordStr, MARGIN + 8, glanceY, bodyPaint);
            if (!posStr.isEmpty()) {
                float posWidth = metaPaint.measureText(posStr);
                canvas.drawText(posStr, PAGE_WIDTH - MARGIN - 8 - posWidth, glanceY, metaPaint);
            }

            canvas.drawLine(MARGIN, glanceY + 6, PAGE_WIDTH - MARGIN, glanceY + 6, dividerPaint);
            glanceY += glanceRowHeight;
        }

        pdfDocument.finishPage(page);

        // -------------------------------------------------------------------------
        // 6. FINAL CLOSING PAGE
        // -------------------------------------------------------------------------
        pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, finalClosingPage).create();
        page = pdfDocument.startPage(pageInfo);
        canvas = page.getCanvas();
        canvas.drawColor(Color.WHITE);

        canvas.drawLine(MARGIN + 40, PAGE_HEIGHT * 0.40f, PAGE_WIDTH - MARGIN - 40, PAGE_HEIGHT * 0.40f, dividerPaint);

        TextPaint closingBigPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        closingBigPaint.setColor(COLOR_INK_PRIMARY);
        closingBigPaint.setTypeface(serifBold);
        closingBigPaint.setTextSize(16f);
        closingBigPaint.setTextAlign(Paint.Align.CENTER);

        TextPaint closingBrandPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        closingBrandPaint.setColor(COLOR_BRONZE_ACCENT);
        closingBrandPaint.setTypeface(serifItalic);
        closingBrandPaint.setTextSize(13f);
        closingBrandPaint.setTextAlign(Paint.Align.CENTER);

        canvas.drawText("WORDS DISCOVERED.", PAGE_WIDTH / 2f, PAGE_HEIGHT * 0.47f, closingBigPaint);
        canvas.drawText("KNOWLEDGE KEPT.", PAGE_WIDTH / 2f, PAGE_HEIGHT * 0.51f, closingBigPaint);

        canvas.drawLine(PAGE_WIDTH / 2f - 30, PAGE_HEIGHT * 0.55f, PAGE_WIDTH / 2f + 30, PAGE_HEIGHT * 0.55f, bronzeLinePaint);

        canvas.drawText("Created with WordShelf", PAGE_WIDTH / 2f, PAGE_HEIGHT * 0.60f, closingBrandPaint);

        pdfDocument.finishPage(page);

        // -------------------------------------------------------------------------
        // WRITE TO FILE & LAUNCH PREVIEW ACTIVITY
        // -------------------------------------------------------------------------
        try {
            String sanitizeName = collectionTitle.replaceAll("[^a-zA-Z0-9.-]", "_");
            File pdfFile = new File(context.getCacheDir(), sanitizeName + "_vocabulary.pdf");
            FileOutputStream fos = new FileOutputStream(pdfFile);
            pdfDocument.writeTo(fos);
            pdfDocument.close();
            fos.close();

            Intent previewIntent = new Intent(context, PdfPreviewActivity.class);
            previewIntent.putExtra(PdfPreviewActivity.EXTRA_PDF_PATH, pdfFile.getAbsolutePath());
            previewIntent.putExtra(PdfPreviewActivity.EXTRA_TITLE, collectionTitle);
            context.startActivity(previewIntent);

        } catch (Exception e) {
            Log.e(TAG, "Error generating PDF", e);
            pdfDocument.close();
            Toast.makeText(context, "Failed to generate PDF: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // HELPER RENDERING METHODS
    // =========================================================================

    private static void drawInfoRow(Canvas canvas, String label, String value, float x, float y,
                                    TextPaint labelPaint, TextPaint valuePaint, Paint dividerPaint) {
        canvas.drawText(label, x, y, labelPaint);
        canvas.drawText(value, x, y + 18, valuePaint);
        canvas.drawLine(x, y + 28, PAGE_WIDTH - MARGIN, y + 28, dividerPaint);
    }

    private static float drawVocabularyEntry(Canvas canvas, int index, DictionaryModel item, float startY, float x, int width,
                                             TextPaint wordTitlePaint, TextPaint phoneticPaint, TextPaint posBadgePaint,
                                             TextPaint bodyPaint, TextPaint examplePaint, TextPaint highlightPaint,
                                             TextPaint notePaint, TextPaint metaPaint, TextPaint labelPaint,
                                             Paint dividerPaint, Paint badgeBgPaint, Paint highlightBgPaint, Paint bronzeLinePaint) {

        float y = startY;

        // Entry Number Prefix e.g. "01", "02"
        String indexStr = String.format(Locale.US, "%02d", index);
        TextPaint indexNumPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        indexNumPaint.setColor(COLOR_BRONZE_ACCENT);
        indexNumPaint.setTypeface(Typeface.SERIF);
        indexNumPaint.setTextSize(11f);

        canvas.drawText(indexStr, x, y + 12, indexNumPaint);
        float indexWidth = indexNumPaint.measureText(indexStr) + 10;

        // Word (Strongest Visual Element)
        String wordStr = item.getWord() != null ? item.getWord().trim().toUpperCase(Locale.US) : "";
        canvas.drawText(wordStr, x + indexWidth, y + 14, wordTitlePaint);

        // Phonetic
        String phoneticStr = item.getPhonetic() != null ? item.getPhonetic().trim() : "";
        if (!phoneticStr.isEmpty()) {
            float wordWidth = wordTitlePaint.measureText(wordStr);
            canvas.drawText("  " + phoneticStr, x + indexWidth + wordWidth, y + 14, phoneticPaint);
        }

        y += 24;

        // Meanings, Definitions, Examples & Synonyms
        if (item.getMeanings() != null) {
            for (Meaning meaning : item.getMeanings()) {
                // Part of speech
                String pos = meaning.getPartOfSpeech();
                if (pos != null && !pos.trim().isEmpty()) {
                    String posText = pos.trim().toLowerCase(Locale.US);
                    float posWidth = posBadgePaint.measureText(posText);
                    RectF badgeRect = new RectF(x, y, x + posWidth + 12, y + 14);
                    canvas.drawRoundRect(badgeRect, 3, 3, badgeBgPaint);
                    canvas.drawText(posText, x + 6, y + 10, posBadgePaint);
                    y += 18;
                }

                canvas.drawLine(x, y, x + width, y, dividerPaint);
                y += 10;

                // Definitions & Examples
                if (meaning.getDefinitions() != null) {
                    for (int d = 0; d < meaning.getDefinitions().size(); d++) {
                        Definition def = meaning.getDefinitions().get(d);
                        if (def != null && def.getDefinition() != null && !def.getDefinition().trim().isEmpty()) {
                            // Section Label
                            canvas.drawText("DEFINITION", x, y + 8, labelPaint);
                            y += 12;

                            StaticLayout defLayout = createStaticLayout(def.getDefinition().trim(), bodyPaint, width - 8);
                            canvas.save();
                            canvas.translate(x, y);
                            defLayout.draw(canvas);
                            canvas.restore();
                            y += defLayout.getHeight() + 6;

                            // Dictionary Example
                            if (def.getExample() != null && !def.getExample().trim().isEmpty()) {
                                canvas.drawText("EXAMPLE", x, y + 8, labelPaint);
                                y += 12;

                                String exText = "\"" + def.getExample().trim() + "\"";
                                StaticLayout exLayout = createStaticLayout(exText, examplePaint, width - 16);
                                canvas.save();
                                canvas.translate(x + 8, y);
                                exLayout.draw(canvas);
                                canvas.restore();
                                y += exLayout.getHeight() + 6;
                            }
                        }
                    }
                }

                // Synonyms
                if (meaning.getSynonyms() != null && !meaning.getSynonyms().isEmpty()) {
                    canvas.drawText("SYNONYMS", x, y + 8, labelPaint);
                    y += 12;

                    String syns = TextUtils.join(" · ", meaning.getSynonyms());
                    StaticLayout synLayout = createStaticLayout(syns, metaPaint, width - 8);
                    canvas.save();
                    canvas.translate(x, y);
                    synLayout.draw(canvas);
                    canvas.restore();
                    y += synLayout.getHeight() + 6;
                }
            }
        }

        // --- READER METADATA ---

        // Found While Reading: Chapter & Page
        String chapter = item.getChapter() != null ? item.getChapter().trim() : "";
        String page = item.getPage() != null ? item.getPage().trim() : "";
        if (!chapter.isEmpty() || !page.isEmpty()) {
            canvas.drawLine(x, y, x + width, y, dividerPaint);
            y += 10;

            canvas.drawText("FOUND WHILE READING", x, y + 8, labelPaint);
            y += 12;

            String foundLocation;
            if (!chapter.isEmpty() && !page.isEmpty()) {
                String pageDisplay = page.toLowerCase(Locale.US).startsWith("p") ? page : "Page " + page;
                foundLocation = chapter + " · " + pageDisplay;
            } else if (!chapter.isEmpty()) {
                foundLocation = chapter;
            } else {
                foundLocation = page.toLowerCase(Locale.US).startsWith("p") ? page : "Page " + page;
            }

            canvas.drawText(foundLocation, x, y + 10, bodyPaint);
            y += 18;
        }

        // Reader Highlight
        String highlight = item.getHighlight() != null ? item.getHighlight().trim() : "";
        if (!highlight.isEmpty()) {
            canvas.drawLine(x, y, x + width, y, dividerPaint);
            y += 10;

            canvas.drawText("MY HIGHLIGHT", x, y + 8, labelPaint);
            y += 12;

            String quote = (highlight.startsWith("\"") && highlight.endsWith("\"")) ? highlight : "\"" + highlight + "\"";
            StaticLayout hlLayout = createStaticLayout(quote, highlightPaint, width - 24);

            RectF hlRect = new RectF(x, y, x + width, y + hlLayout.getHeight() + 12);
            canvas.drawRoundRect(hlRect, 4, 4, highlightBgPaint);

            // Left bronze accent bar
            canvas.drawRect(x, y, x + 3.5f, y + hlLayout.getHeight() + 12, bronzeLinePaint);

            canvas.save();
            canvas.translate(x + 12, y + 6);
            hlLayout.draw(canvas);
            canvas.restore();

            y += hlLayout.getHeight() + 18;
        }

        // Reader Note
        String note = item.getNote() != null ? item.getNote().trim() : "";
        if (!note.isEmpty()) {
            canvas.drawLine(x, y, x + width, y, dividerPaint);
            y += 10;

            canvas.drawText("MY NOTE", x, y + 8, labelPaint);
            y += 12;

            StaticLayout noteLayout = createStaticLayout(note, notePaint, width - 8);
            canvas.save();
            canvas.translate(x, y);
            noteLayout.draw(canvas);
            canvas.restore();

            y += noteLayout.getHeight() + 8;
        }

        // Bottom divider for entry
        canvas.drawLine(x, y, x + width, y, dividerPaint);
        return y;
    }

    private static int calculateEntryHeight(DictionaryModel item, TextPaint wordTitlePaint, TextPaint bodyPaint,
                                            TextPaint examplePaint, TextPaint highlightPaint, TextPaint notePaint,
                                            TextPaint metaPaint, int width) {
        int height = 24; // Title + Phonetics line

        if (item.getMeanings() != null) {
            for (Meaning meaning : item.getMeanings()) {
                if (meaning.getPartOfSpeech() != null && !meaning.getPartOfSpeech().trim().isEmpty()) {
                    height += 18; // POS badge
                }
                height += 10; // Divider

                if (meaning.getDefinitions() != null) {
                    for (Definition def : meaning.getDefinitions()) {
                        if (def != null && def.getDefinition() != null && !def.getDefinition().trim().isEmpty()) {
                            height += 12; // "DEFINITION" label
                            StaticLayout defLayout = createStaticLayout(def.getDefinition().trim(), bodyPaint, width - 8);
                            height += defLayout.getHeight() + 6;

                            if (def.getExample() != null && !def.getExample().trim().isEmpty()) {
                                height += 12; // "EXAMPLE" label
                                StaticLayout exLayout = createStaticLayout("\"" + def.getExample().trim() + "\"", examplePaint, width - 16);
                                height += exLayout.getHeight() + 6;
                            }
                        }
                    }
                }

                if (meaning.getSynonyms() != null && !meaning.getSynonyms().isEmpty()) {
                    height += 12; // "SYNONYMS" label
                    StaticLayout synLayout = createStaticLayout(TextUtils.join(" · ", meaning.getSynonyms()), metaPaint, width - 8);
                    height += synLayout.getHeight() + 6;
                }
            }
        }

        // Reader metadata height
        String chapter = item.getChapter() != null ? item.getChapter().trim() : "";
        String page = item.getPage() != null ? item.getPage().trim() : "";
        if (!chapter.isEmpty() || !page.isEmpty()) {
            height += 40;
        }

        String highlight = item.getHighlight() != null ? item.getHighlight().trim() : "";
        if (!highlight.isEmpty()) {
            height += 22;
            String quote = (highlight.startsWith("\"") && highlight.endsWith("\"")) ? highlight : "\"" + highlight + "\"";
            StaticLayout hlLayout = createStaticLayout(quote, highlightPaint, width - 24);
            height += hlLayout.getHeight() + 18;
        }

        String note = item.getNote() != null ? item.getNote().trim() : "";
        if (!note.isEmpty()) {
            height += 22;
            StaticLayout noteLayout = createStaticLayout(note, notePaint, width - 8);
            height += noteLayout.getHeight() + 8;
        }

        return height + 10;
    }

    private static void drawHeader(Canvas canvas, String title, int margin, TextPaint headerPaint, Paint linePaint) {
        canvas.drawText("WordShelf", margin, margin - 14, headerPaint);
        float titleWidth = headerPaint.measureText(title);
        canvas.drawText(title, PAGE_WIDTH - margin - titleWidth, margin - 14, headerPaint);
        canvas.drawLine(margin, margin - 8, PAGE_WIDTH - margin, margin - 8, linePaint);
    }

    private static void drawFooter(Canvas canvas, int pageNum, int totalPages, int margin, TextPaint footerPaint) {
        String footerText = String.valueOf(pageNum);
        float width = footerPaint.measureText(footerText);
        canvas.drawText(footerText, (PAGE_WIDTH - width) / 2f, PAGE_HEIGHT - (margin / 2f), footerPaint);
    }

    private static StaticLayout createStaticLayout(CharSequence text, TextPaint paint, int width) {
        int safeWidth = Math.max(10, width);
        CharSequence safeText = text != null ? text : "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return StaticLayout.Builder.obtain(safeText, 0, safeText.length(), paint, safeWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0.0f, 1.18f)
                    .setIncludePad(false)
                    .build();
        } else {
            //noinspection deprecation
            return new StaticLayout(safeText, paint, safeWidth, Layout.Alignment.ALIGN_NORMAL, 1.18f, 0.0f, false);
        }
    }
}
