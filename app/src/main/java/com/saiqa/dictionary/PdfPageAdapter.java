package com.saiqa.dictionary;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.ViewHolder> {

    private final List<Bitmap> pageBitmaps = new ArrayList<>();

    public void setPages(List<Bitmap> bitmaps) {
        pageBitmaps.clear();
        if (bitmaps != null) {
            pageBitmaps.addAll(bitmaps);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pdf_page, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Bitmap bitmap = pageBitmaps.get(position);
        holder.pdfPageImage.setImageBitmap(bitmap);
        holder.pdfPageNumber.setText("Page " + (position + 1) + " of " + pageBitmaps.size());
    }

    @Override
    public int getItemCount() {
        return pageBitmaps.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView pdfPageImage;
        TextView pdfPageNumber;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            pdfPageImage = itemView.findViewById(R.id.pdf_page_image);
            pdfPageNumber = itemView.findViewById(R.id.pdf_page_number);
        }
    }
}
