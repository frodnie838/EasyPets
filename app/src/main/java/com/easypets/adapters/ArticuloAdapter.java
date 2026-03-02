package com.easypets.adapters;

import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.easypets.R;
import com.easypets.models.Articulo;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ArticuloAdapter extends RecyclerView.Adapter<ArticuloAdapter.ArticuloViewHolder> {
    private List<Articulo> articulos;
    private OnArticuloClickListener listener;

    public interface OnArticuloClickListener { void onArticuloClick(Articulo articulo); }

    public ArticuloAdapter(List<Articulo> articulos, OnArticuloClickListener listener) {
        this.articulos = articulos;
        this.listener = listener;
    }

    @NonNull @Override
    public ArticuloViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_articulo, parent, false);
        return new ArticuloViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArticuloViewHolder holder, int position) {
        Articulo articulo = articulos.get(position);
        holder.tvTitulo.setText(articulo.getTitulo());
        holder.tvDescripcion.setText(articulo.getDescripcionCorta());
        String fecha = new SimpleDateFormat("dd MMM", Locale.getDefault()).format(new Date(articulo.getTimestampCreacion()));
        holder.tvAutor.setText(articulo.isEsOficial() ? "✓ Oficial • " + fecha : "Por: " + articulo.getAutor());

        if (articulo.getImagenPortadaBase64() != null && !articulo.getImagenPortadaBase64().isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(articulo.getImagenPortadaBase64(), Base64.DEFAULT);
                holder.ivIcono.setImageBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                holder.ivIcono.setScaleType(ImageView.ScaleType.CENTER_CROP);
                holder.ivIcono.setImageTintList(null);
            } catch (Exception e) {
                holder.ivIcono.setImageResource(articulo.isEsOficial() ? R.drawable.consejos : R.drawable.profile);
            }
        } else {
            holder.ivIcono.setImageResource(articulo.isEsOficial() ? R.drawable.consejos : R.drawable.profile);
        }
        holder.itemView.setOnClickListener(v -> listener.onArticuloClick(articulo));
    }

    @Override public int getItemCount() { return articulos.size(); }

    public static class ArticuloViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvDescripcion, tvAutor;
        ImageView ivIcono;
        public ArticuloViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTituloArticulo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionArticulo);
            tvAutor = itemView.findViewById(R.id.tvAutorArticulo);
            ivIcono = itemView.findViewById(R.id.ivIconoArticulo);
        }
    }
}