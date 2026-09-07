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
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PdfExporter {

    public static void exportSingleWord(Context context, DictionaryModel model) {
        if (model == null) return;
        List<DictionaryModel> list = new ArrayList<>();
        list.add(model);
        exportBookCollection(context, model.getWord() != null ? model.getWord() + " Definition" : "Vocabulary Word", list);
    }

    public static void exportBookCollection(Context context, String collectionTitle, List<DictionaryModel> wordList) {
        if (wordList == null || wordList.isEmpty()) {
            Toast.makeText(context, "No saved words to export as PDF", Toast.LENGTH_SHORT).show();
            return;
        }

        // Sort collection alphabetically
        List<DictionaryModel> sortedList = new ArrayList<>(wordList);
        Collections.sort(sortedList, (a, b) -> {
            String w1 = a.getWord() != null ? a.getWord().trim() : "";
            String w2 = b.getWord() != null ? b.getWord().trim() : "";
            return w1.compareToIgnoreCase(w2);
        });

        PdfDocument pdfDocument = new PdfDocument();

        int pageWidth = 595;  // A4 width in points
        int pageHeight = 842; // A4 height in points
        int margin = 48;
        int usableWidth = pageWidth - (margin * 2);

        // --- PRINT-FRIENDLY COLOR PALETTE ---
        int colorNavy = Color.parseColor("#2C3E50");
        int colorBronze = Color.parseColor("#8E6E53");
        int colorDarkText = Color.parseColor("#1E293B");
        int colorSecondaryText = Color.parseColor("#64748B");
        int colorBodyText = Color.parseColor("#334155");
        int colorExampleText = Color.parseColor("#475569");
        int colorBorder = Color.parseColor("#E2E8F0");
        int colorBadgeBg = Color.parseColor("#F1F5F9");

        // --- TYPOGRAPHY & PAINTS ---
        Typeface serifBold = Typeface.create(Typeface.SERIF, Typeface.BOLD);
        Typeface serifNormal = Typeface.create(Typeface.SERIF, Typeface.NORMAL);
        Typeface serifItalic = Typeface.create(Typeface.SERIF, Typeface.ITALIC);
        Typeface sansBold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
        Typeface sansNormal = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

        TextPaint coverBrandPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        coverBrandPaint.setColor(colorBronze);
        coverBrandPaint.setTypeface(sansBold);
        coverBrandPaint.setTextSize(11);
        coverBrandPaint.setTextAlign(Paint.Align.CENTER);

        TextPaint coverTitlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        coverTitlePaint.setColor(colorNavy);
        coverTitlePaint.setTypeface(serifBold);
        coverTitlePaint.setTextSize(30);
        coverTitlePaint.setTextAlign(Paint.Align.CENTER);

        TextPaint coverSubPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        coverSubPaint.setColor(colorSecondaryText);
        coverSubPaint.setTypeface(serifItalic);
        coverSubPaint.setTextSize(15);
        coverSubPaint.setTextAlign(Paint.Align.CENTER);

        TextPaint coverMetaPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        coverMetaPaint.setColor(colorDarkText);
        coverMetaPaint.setTypeface(sansNormal);
        coverMetaPaint.setTextSize(12);

        TextPaint sectionTitlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        sectionTitlePaint.setColor(colorNavy);
        sectionTitlePaint.setTypeface(serifBold);
        sectionTitlePaint.setTextSize(20);

        TextPaint wordTitlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        wordTitlePaint.setColor(colorDarkText);
        wordTitlePaint.setTypeface(serifBold);
        wordTitlePaint.setTextSize(16);

        TextPaint phoneticPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        phoneticPaint.setColor(colorSecondaryText);
        phoneticPaint.setTypeface(serifItalic);
        phoneticPaint.setTextSize(12);

        TextPaint posBadgePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        posBadgePaint.setColor(colorNavy);
        posBadgePaint.setTypeface(sansBold);
        posBadgePaint.setTextSize(9);

        TextPaint bodyPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(colorBodyText);
        bodyPaint.setTypeface(serifNormal);
        bodyPaint.setTextSize(11);

        TextPaint examplePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        examplePaint.setColor(colorExampleText);
        examplePaint.setTypeface(serifItalic);
        examplePaint.setTextSize(10.5f);

        TextPaint metaPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        metaPaint.setColor(colorSecondaryText);
        metaPaint.setTypeface(sansNormal);
        metaPaint.setTextSize(9.5f);

        TextPaint headerFooterPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        headerFooterPaint.setColor(Color.parseColor("#94A3B8"));
        headerFooterPaint.setTypeface(sansNormal);
        headerFooterPaint.setTextSize(9);

        Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dividerPaint.setColor(colorBorder);
        dividerPaint.setStrokeWidth(0.75f);

        Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        framePaint.setColor(colorNavy);
        framePaint.setStyle(Paint.Style.STROKE);
        framePaint.setStrokeWidth(1.5f);

        Paint badgeBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgeBgPaint.setColor(colorBadgeBg);
        badgeBgPaint.setStyle(Paint.Style.FILL);

        // --- PASS 1: SMART PAGINATION & PAGE INDEXING ---
        boolean hasTOC = sortedList.size() >= 5;
        int currentPage = 1;
        int contentStartPage = hasTOC ? 3 : 2;

        currentPage = contentStartPage;
        int currentY = margin + 35; // Reserve for top header

        Map<Integer, Integer> wordPageMap = new HashMap<>(); // Word index -> Page number

        for (int i = 0; i < sortedList.size(); i++) {
            DictionaryModel item = sortedList.get(i);
            int entryHeight = calculateEntryHeight(item, bodyPaint, examplePaint, metaPaint, usableWidth);

            if (currentY + entryHeight > pageHeight - margin - 35) {
                currentPage++;
                currentY = margin + 35;
            }

            wordPageMap.put(i, currentPage);
            currentY += entryHeight + 16;
        }

        int totalPages = currentPage;

        // --- PASS 2: RENDER PDF PAGES ---

        // 1. RENDER COVER PAGE
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Draw background & frame
        canvas.drawColor(Color.WHITE);
        canvas.drawRect(margin - 10, margin - 10, pageWidth - margin + 10, pageHeight - margin + 10, framePaint);
        canvas.drawRect(margin - 14, margin - 14, pageWidth - margin + 14, pageHeight - margin + 14, framePaint);

        canvas.drawText("WORDSHELF — PERSONAL VOCABULARY", pageWidth / 2f, pageHeight / 4f - 20, coverBrandPaint);
        canvas.drawText(collectionTitle, pageWidth / 2f, pageHeight / 4f + 25, coverTitlePaint);
        canvas.drawText("Personal Reading Vocabulary Collection", pageWidth / 2f, pageHeight / 4f + 55, coverSubPaint);

        canvas.drawLine(margin + 60, pageHeight / 4f + 80, pageWidth - margin - 60, pageHeight / 4f + 80, dividerPaint);

        // Metadata Card
        float cardTop = pageHeight / 2f + 20;
        float cardLeft = margin + 40;
        float cardRight = pageWidth - margin - 40;
        float cardBottom = cardTop + 100;

        RectF cardRect = new RectF(cardLeft, cardTop, cardRight, cardBottom);
        canvas.drawRoundRect(cardRect, 12, 12, badgeBgPaint);
        canvas.drawRoundRect(cardRect, 12, 12, framePaint);

        String dateStr = new SimpleDateFormat("MMMM d, yyyy", Locale.US).format(new Date());
        canvas.drawText("Collection Size: " + sortedList.size() + " Unique Word(s)", cardLeft + 24, cardTop + 38, coverMetaPaint);
        canvas.drawText("Export Date: " + dateStr, cardLeft + 24, cardTop + 68, coverMetaPaint);

        canvas.drawText("Generated with WordShelf App", pageWidth / 2f, pageHeight - margin - 20, headerFooterPaint);
        pdfDocument.finishPage(page);

        // 2. RENDER TABLE OF CONTENTS (IF 5+ WORDS)
        if (hasTOC) {
            pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create();
            page = pdfDocument.startPage(pageInfo);
            canvas = page.getCanvas();
            canvas.drawColor(Color.WHITE);

            drawHeader(canvas, collectionTitle, pageWidth, margin, headerFooterPaint, dividerPaint);
            drawFooter(canvas, 2, totalPages, pageWidth, pageHeight, margin, headerFooterPaint);

            canvas.drawText("Table of Contents", margin, margin + 40, sectionTitlePaint);
            canvas.drawLine(margin, margin + 48, pageWidth - margin, margin + 48, dividerPaint);

            int tocY = margin + 70;
            TextPaint dotsPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            dotsPaint.setColor(colorBorder);
            dotsPaint.setTypeface(sansNormal);
            dotsPaint.setTextSize(10);

            for (int i = 0; i < sortedList.size(); i++) {
                if (tocY > pageHeight - margin - 50) {
                    break; // Keep TOC concise on single page
                }

                String wordStr = (i + 1) + ". " + (sortedList.get(i).getWord() != null ? sortedList.get(i).getWord() : "");
                int targetPage = wordPageMap.containsKey(i) ? wordPageMap.get(i) : 3;

                canvas.drawText(wordStr, margin, tocY, bodyPaint);
                String pageStr = String.valueOf(targetPage);
                float pageStrWidth = bodyPaint.measureText(pageStr);
                canvas.drawText(pageStr, pageWidth - margin - pageStrWidth, tocY, bodyPaint);

                float wordWidth = bodyPaint.measureText(wordStr);
                float dotsStart = margin + wordWidth + 10;
                float dotsEnd = pageWidth - margin - pageStrWidth - 10;

                if (dotsEnd > dotsStart) {
                    StringBuilder dots = new StringBuilder();
                    while (dotsPaint.measureText(dots.toString() + ". ") < (dotsEnd - dotsStart)) {
                        dots.append(". ");
                    }
                    canvas.drawText(dots.toString(), dotsStart, tocY, dotsPaint);
                }

                tocY += 20;
            }

            pdfDocument.finishPage(page);
        }

        // 3. RENDER CONTENT PAGES
        currentPage = contentStartPage;
        pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPage).create();
        page = pdfDocument.startPage(pageInfo);
        canvas = page.getCanvas();
        canvas.drawColor(Color.WHITE);

        drawHeader(canvas, collectionTitle, pageWidth, margin, headerFooterPaint, dividerPaint);
        drawFooter(canvas, currentPage, totalPages, pageWidth, pageHeight, margin, headerFooterPaint);

        currentY = margin + 35;

        for (int i = 0; i < sortedList.size(); i++) {
            DictionaryModel item = sortedList.get(i);
            int entryHeight = calculateEntryHeight(item, bodyPaint, examplePaint, metaPaint, usableWidth);

            if (currentY + entryHeight > pageHeight - margin - 35) {
                pdfDocument.finishPage(page);
                currentPage++;

                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPage).create();
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                canvas.drawColor(Color.WHITE);

                drawHeader(canvas, collectionTitle, pageWidth, margin, headerFooterPaint, dividerPaint);
                drawFooter(canvas, currentPage, totalPages, pageWidth, pageHeight, margin, headerFooterPaint);

                currentY = margin + 35;
            }

            // --- DRAW WORD ENTRY ---
            String wordStr = (i + 1) + ". " + (item.getWord() != null ? item.getWord().trim() : "");
            String phoneticStr = item.getPhonetic() != null ? item.getPhonetic().trim() : "";

            canvas.drawText(wordStr, margin, currentY + 14, wordTitlePaint);
            if (!phoneticStr.isEmpty()) {
                float titleWidth = wordTitlePaint.measureText(wordStr);
                canvas.drawText("  " + phoneticStr, margin + titleWidth, currentY + 14, phoneticPaint);
            }

            currentY += 22;

            if (item.getMeanings() != null) {
                for (Meaning meaning : item.getMeanings()) {
                    // Part of Speech Badge
                    String pos = meaning.getPartOfSpeech();
                    if (pos != null && !pos.trim().isEmpty()) {
                        String posText = pos.trim().toUpperCase(Locale.US);
                        float posWidth = posBadgePaint.measureText(posText);
                        RectF badgeRect = new RectF(margin, currentY, margin + posWidth + 12, currentY + 14);
                        canvas.drawRoundRect(badgeRect, 4, 4, badgeBgPaint);
                        canvas.drawText(posText, margin + 6, currentY + 10, posBadgePaint);
                        currentY += 20;
                    }

                    // Definitions & Examples
                    if (meaning.getDefinitions() != null) {
                        for (int d = 0; d < meaning.getDefinitions().size(); d++) {
                            Definition def = meaning.getDefinitions().get(d);
                            if (def != null && def.getDefinition() != null && !def.getDefinition().trim().isEmpty()) {
                                String defText = (d + 1) + ". " + def.getDefinition().trim();
                                StaticLayout defLayout = createStaticLayout(defText, bodyPaint, usableWidth - 12);

                                canvas.save();
                                canvas.translate(margin + 6, currentY);
                                defLayout.draw(canvas);
                                canvas.restore();

                                currentY += defLayout.getHeight() + 4;

                                if (def.getExample() != null && !def.getExample().trim().isEmpty()) {
                                    String exText = "Example: \"" + def.getExample().trim() + "\"";
                                    StaticLayout exLayout = createStaticLayout(exText, examplePaint, usableWidth - 24);

                                    canvas.save();
                                    canvas.translate(margin + 18, currentY);
                                    exLayout.draw(canvas);
                                    canvas.restore();

                                    currentY += exLayout.getHeight() + 4;
                                }
                            }
                        }
                    }

                    // Synonyms / Antonyms
                    if (meaning.getSynonyms() != null && !meaning.getSynonyms().isEmpty()) {
                        String syns = "Synonyms: " + TextUtils.join(", ", meaning.getSynonyms());
                        StaticLayout synLayout = createStaticLayout(syns, metaPaint, usableWidth - 12);
                        canvas.save();
                        canvas.translate(margin + 6, currentY);
                        synLayout.draw(canvas);
                        canvas.restore();
                        currentY += synLayout.getHeight() + 4;
                    }

                    if (meaning.getAntonyms() != null && !meaning.getAntonyms().isEmpty()) {
                        String ants = "Antonyms: " + TextUtils.join(", ", meaning.getAntonyms());
                        StaticLayout antLayout = createStaticLayout(ants, metaPaint, usableWidth - 12);
                        canvas.save();
                        canvas.translate(margin + 6, currentY);
                        antLayout.draw(canvas);
                        canvas.restore();
                        currentY += antLayout.getHeight() + 4;
                    }

                    currentY += 4;
                }
            }

            // Divider between word entries
            currentY += 6;
            canvas.drawLine(margin, currentY, pageWidth - margin, currentY, dividerPaint);
            currentY += 10;
        }

        pdfDocument.finishPage(page);

        // Save PDF to Cache & Launch Preview Activity
        try {
            String sanitizeName = collectionTitle.replaceAll("[^a-zA-Z0-9.-]", "_");
            File pdfFile = new File(context.getCacheDir(), sanitizeName + "_vocabulary.pdf");
            FileOutputStream fos = new FileOutputStream(pdfFile);
            pdfDocument.writeTo(fos);
            pdfDocument.close();
            fos.close();

            // Launch PDF Preview Activity
            Intent previewIntent = new Intent(context, PdfPreviewActivity.class);
            previewIntent.putExtra(PdfPreviewActivity.EXTRA_PDF_PATH, pdfFile.getAbsolutePath());
            previewIntent.putExtra(PdfPreviewActivity.EXTRA_TITLE, collectionTitle);
            context.startActivity(previewIntent);

        } catch (Exception e) {
            Log.e("PDF_EXPORTER", "Error generating PDF", e);
            pdfDocument.close();
            Toast.makeText(context, "Failed to generate PDF: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static int calculateEntryHeight(DictionaryModel item, TextPaint bodyPaint, TextPaint examplePaint, TextPaint metaPaint, int usableWidth) {
        int height = 22; // Word Title + Phonetic line
        if (item.getMeanings() != null) {
            for (Meaning meaning : item.getMeanings()) {
                if (meaning.getPartOfSpeech() != null && !meaning.getPartOfSpeech().trim().isEmpty()) {
                    height += 20; // Part of speech badge
                }
                if (meaning.getDefinitions() != null) {
                    for (Definition def : meaning.getDefinitions()) {
                        if (def != null && def.getDefinition() != null && !def.getDefinition().trim().isEmpty()) {
                            StaticLayout defLayout = createStaticLayout(def.getDefinition().trim(), bodyPaint, usableWidth - 12);
                            height += defLayout.getHeight() + 4;
                            if (def.getExample() != null && !def.getExample().trim().isEmpty()) {
                                StaticLayout exLayout = createStaticLayout(def.getExample().trim(), examplePaint, usableWidth - 24);
                                height += exLayout.getHeight() + 4;
                            }
                        }
                    }
                }
                if (meaning.getSynonyms() != null && !meaning.getSynonyms().isEmpty()) {
                    StaticLayout synLayout = createStaticLayout(TextUtils.join(", ", meaning.getSynonyms()), metaPaint, usableWidth - 12);
                    height += synLayout.getHeight() + 4;
                }
                if (meaning.getAntonyms() != null && !meaning.getAntonyms().isEmpty()) {
                    StaticLayout antLayout = createStaticLayout(TextUtils.join(", ", meaning.getAntonyms()), metaPaint, usableWidth - 12);
                    height += antLayout.getHeight() + 4;
                }
                height += 4;
            }
        }
        return height + 16; // Divider line + padding
    }

    private static void drawHeader(Canvas canvas, String title, int pageWidth, int margin, TextPaint headerPaint, Paint linePaint) {
        String headerText = "WordShelf: " + title;
        canvas.drawText(headerText, margin, margin - 14, headerPaint);
        canvas.drawLine(margin, margin - 8, pageWidth - margin, margin - 8, linePaint);
    }

    private static void drawFooter(Canvas canvas, int pageNum, int totalPages, int pageWidth, int pageHeight, int margin, TextPaint footerPaint) {
        String footerText = "Page " + pageNum + " of " + totalPages;
        float width = footerPaint.measureText(footerText);
        canvas.drawText(footerText, (pageWidth - width) / 2f, pageHeight - (margin / 2f), footerPaint);
    }

    private static StaticLayout createStaticLayout(CharSequence text, TextPaint paint, int width) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return StaticLayout.Builder.obtain(text, 0, text.length(), paint, width)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0.0f, 1.15f)
                    .setIncludePad(false)
                    .build();
        } else {
            //noinspection deprecation
            return new StaticLayout(text, paint, width, Layout.Alignment.ALIGN_NORMAL, 1.15f, 0.0f, false);
        }
    }
}
