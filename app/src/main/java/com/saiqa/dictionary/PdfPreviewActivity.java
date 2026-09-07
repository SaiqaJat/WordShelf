package com.saiqa.dictionary;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PdfPreviewActivity extends AppCompatActivity {

    public static final String EXTRA_PDF_PATH = "pdf_file_path";
    public static final String EXTRA_TITLE = "collection_title";

    private String pdfFilePath;
    private String collectionTitle = "Vocabulary Document";

    private RecyclerView rvPdfPages;
    private PdfPageAdapter adapter;
    private List<Bitmap> pageBitmaps = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pdf_preview);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_pdf_preview), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.preview_toolbar);
        TextView titleTv = findViewById(R.id.preview_title);
        TextView subtitleTv = findViewById(R.id.preview_subtitle);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        Intent intent = getIntent();
        if (intent != null) {
            pdfFilePath = intent.getStringExtra(EXTRA_PDF_PATH);
            if (intent.hasExtra(EXTRA_TITLE)) {
                collectionTitle = intent.getStringExtra(EXTRA_TITLE);
            }
        }

        if (titleTv != null) {
            titleTv.setText(collectionTitle + " - Preview");
        }
        if (subtitleTv != null) {
            subtitleTv.setText("Inspect pages below before saving");
        }

        rvPdfPages = findViewById(R.id.rv_pdf_pages);
        rvPdfPages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PdfPageAdapter();
        rvPdfPages.setAdapter(adapter);

        Button btnCancel = findViewById(R.id.btn_cancel_preview);
        Button btnDownload = findViewById(R.id.btn_download_pdf);

        btnCancel.setOnClickListener(v -> {
            Toast.makeText(this, "Export cancelled", Toast.LENGTH_SHORT).show();
            finish();
        });

        btnDownload.setOnClickListener(v -> downloadPdf());

        renderPdfPages();
    }

    private void renderPdfPages() {
        if (pdfFilePath == null) return;
        File file = new File(pdfFilePath);
        if (!file.exists()) {
            Toast.makeText(this, "PDF File not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        new Thread(() -> {
            try {
                ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
                PdfRenderer renderer = new PdfRenderer(fileDescriptor);

                List<Bitmap> bitmaps = new ArrayList<>();
                int pageCount = renderer.getPageCount();

                for (int i = 0; i < pageCount; i++) {
                    PdfRenderer.Page page = renderer.openPage(i);
                    // Render page at high quality (scale factor 2)
                    int width = page.getWidth() * 2;
                    int height = page.getHeight() * 2;

                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    bitmaps.add(bitmap);
                    page.close();
                }

                renderer.close();
                fileDescriptor.close();

                runOnUiThread(() -> {
                    pageBitmaps = bitmaps;
                    adapter.setPages(pageBitmaps);
                });

            } catch (Exception e) {
                Log.e("PDF_RENDER", "Failed to render PDF preview", e);
                runOnUiThread(() -> Toast.makeText(PdfPreviewActivity.this, "Error rendering PDF preview: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void downloadPdf() {
        if (pdfFilePath == null) return;
        File file = new File(pdfFilePath);
        if (!file.exists()) return;

        try {
            Uri pdfUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);

            Intent shareOrSaveIntent = new Intent(Intent.ACTION_SEND);
            shareOrSaveIntent.setType("application/pdf");
            shareOrSaveIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            shareOrSaveIntent.putExtra(Intent.EXTRA_SUBJECT, collectionTitle + " Vocabulary");
            shareOrSaveIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooser = Intent.createChooser(shareOrSaveIntent, "Save or Share PDF: " + collectionTitle);
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(chooser);

            Toast.makeText(this, "Opening PDF save options...", Toast.LENGTH_SHORT).show();
            finish();

        } catch (Exception e) {
            Log.e("PDF_DOWNLOAD", "Error downloading PDF", e);
            Toast.makeText(this, "Failed to download PDF: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        // Recycle bitmaps to save memory
        for (Bitmap b : pageBitmaps) {
            if (b != null && !b.isRecycled()) {
                b.recycle();
            }
        }
        super.onDestroy();
    }
}
