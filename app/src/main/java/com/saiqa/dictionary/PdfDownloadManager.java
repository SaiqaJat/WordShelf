package com.saiqa.dictionary;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PdfDownloadManager {

    public static final String CHANNEL_ID = "wordshelf_pdf_downloads";
    public static final String CHANNEL_NAME = "Vocabulary PDF Downloads";
    private static final String TAG = "PdfDownloadManager";

    public interface DownloadCallback {
        void onSuccess(String fileName, Uri fileUri);
        void onFailure(Exception exception);
    }

    public static String generateMeaningfulFileName(String collectionTitle) {
        if (collectionTitle == null || collectionTitle.trim().isEmpty()) {
            return "Vocabulary_Collection.pdf";
        }

        // Clean up title: remove trailing "Definition" or "Definitions" if single word
        String cleaned = collectionTitle.trim();
        if (cleaned.toLowerCase().endsWith(" definition")) {
            cleaned = cleaned.substring(0, cleaned.length() - " definition".length()).trim();
        }

        // Replace non-alphanumeric characters with underscores
        String sanitized = cleaned.replaceAll("[^a-zA-Z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (sanitized.isEmpty()) {
            sanitized = "Vocabulary";
        }

        // Capitalize words nicely if needed, or ensure "Vocabulary" suffix
        if (!sanitized.toLowerCase().endsWith("vocabulary")) {
            sanitized += "_Vocabulary";
        }

        return sanitized + ".pdf";
    }

    public static void savePdfToDownloads(Context context, File sourceCacheFile, String collectionTitle, DownloadCallback callback) {
        if (sourceCacheFile == null || !sourceCacheFile.exists()) {
            if (callback != null) {
                callback.onFailure(new IOException("Source PDF file does not exist"));
            }
            return;
        }

        final String fileName = generateMeaningfulFileName(collectionTitle);

        new Thread(() -> {
            Uri savedUri = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentResolver resolver = context.getContentResolver();
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                    values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                    values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/WordShelf");
                    values.put(MediaStore.Downloads.IS_PENDING, 1);

                    savedUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (savedUri == null) {
                        throw new IOException("Failed to create MediaStore entry in Downloads");
                    }

                    try (InputStream in = new FileInputStream(sourceCacheFile);
                         OutputStream out = resolver.openOutputStream(savedUri)) {
                        if (out == null) throw new IOException("Failed to open output stream");
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                        out.flush();
                    }

                    values.clear();
                    values.put(MediaStore.Downloads.IS_PENDING, 0);
                    resolver.update(savedUri, values, null, null);

                } else {
                    File downloadsDir = new File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            "WordShelf"
                    );
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs();
                    }
                    File destinationFile = new File(downloadsDir, fileName);

                    try (InputStream in = new FileInputStream(sourceCacheFile);
                         OutputStream out = new FileOutputStream(destinationFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                        out.flush();
                    }

                    savedUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", destinationFile);
                }

                // Show system notification
                showDownloadSuccessNotification(context, fileName, savedUri);

                final Uri finalUri = savedUri;
                if (callback != null) {
                    callback.onSuccess(fileName, finalUri);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error saving PDF to device", e);
                if (callback != null) {
                    callback.onFailure(e);
                }
            }
        }).start();
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                channel.setDescription("Notifications for downloaded vocabulary PDFs");
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public static void showDownloadSuccessNotification(Context context, String fileName, Uri fileUri) {
        try {
            createNotificationChannel(context);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted, skipping notification");
                    return;
                }
            }

            Intent openIntent = new Intent(Intent.ACTION_VIEW);
            openIntent.setDataAndType(fileUri, "application/pdf");
            openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_download)
                    .setContentTitle("Book saved")
                    .setContentText(fileName + " has been saved to your device.")
                    .setSubText("WordShelf")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            int notificationId = (int) System.currentTimeMillis();
            notificationManager.notify(notificationId, builder.build());

        } catch (SecurityException se) {
            Log.w(TAG, "SecurityException posting download notification", se);
        } catch (Exception e) {
            Log.e(TAG, "Failed to show download notification", e);
        }
    }
}
