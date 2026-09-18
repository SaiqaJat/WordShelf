package com.saiqa.dictionary;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PdfDownloadManagerTest {

    @Test
    public void testGenerateMeaningfulFileName_standardTitle() {
        String result = PdfDownloadManager.generateMeaningfulFileName("My Novel");
        assertEquals("My_Novel_Vocabulary.pdf", result);
    }

    @Test
    public void testGenerateMeaningfulFileName_titleWithSpacesAndPunctuation() {
        String result = PdfDownloadManager.generateMeaningfulFileName("The Great Gatsby!");
        assertEquals("The_Great_Gatsby_Vocabulary.pdf", result);
    }

    @Test
    public void testGenerateMeaningfulFileName_alreadyEndsWithVocabulary() {
        String result = PdfDownloadManager.generateMeaningfulFileName("Advanced English Vocabulary");
        assertEquals("Advanced_English_Vocabulary.pdf", result);
    }

    @Test
    public void testGenerateMeaningfulFileName_singleWordDefinition() {
        String result = PdfDownloadManager.generateMeaningfulFileName("serendipity Definition");
        assertEquals("serendipity_Vocabulary.pdf", result);
    }

    @Test
    public void testGenerateMeaningfulFileName_nullOrEmpty() {
        assertEquals("Vocabulary_Collection.pdf", PdfDownloadManager.generateMeaningfulFileName(null));
        assertEquals("Vocabulary_Collection.pdf", PdfDownloadManager.generateMeaningfulFileName("   "));
    }

    @Test
    public void testGenerateMeaningfulFileName_specialCharactersOnly() {
        String result = PdfDownloadManager.generateMeaningfulFileName("###@@@$$$");
        assertEquals("Vocabulary.pdf", result);
    }
}
