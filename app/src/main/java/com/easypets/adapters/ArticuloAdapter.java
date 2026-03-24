package com.easypets.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.format.DateUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.easypets.R;
import com.easypets.models.Articulo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class ArticuloAdapter extends RecyclerView.Adapter<ArticuloAdapter.ViewHolder> {

    private List<Articulo> listaArticulos;
    private OnArticuloClickListener listener;
    private String currentUserId;
    private boolean mostrarOpciones = false;

    public interface OnArticuloClickListener {
        void onArticuloClick(Articulo articulo);
        void onEditarClick(Articulo articulo);
        void onBorrarClick(Articulo articulo);
        void onReportarClick(Articulo articulo);
    }

    public ArticuloAdapter(List<Articulo> listaArticulos, OnArticuloClickListener listener) {
        this.listaArticulos = listaArticulos;
        this.listener = listener;
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    public void setMostrarOpciones(boolean mostrarOpciones) {
        this.mostrarOpciones = mostrarOpciones;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_articulo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Articulo articulo = listaArticulos.get(position);
        if (articulo == null) return; // 🛡️ PROGRAMACIÓN DEFENSIVA: Evitar nulos

        // Títulos y Descripciones seguros
        holder.tvTitulo.setText(articulo.getTitulo() != null ? articulo.getTitulo() : "Sin título");
        holder.tvDescripcion.setText(articulo.getDescripcionCorta() != null ? articulo.getDescripcionCorta() : "");

        // Autor seguro
        if (articulo.isEsOficial()) {
            holder.tvAutor.setText("Por: EasyPets Oficial");
            holder.tvAutor.setTextColor(android.graphics.Color.parseColor("#4CAF50")); // Verde
        } else {
            String nombreAutor = articulo.getAutor() != null ? articulo.getAutor() : "Usuario";
            holder.tvAutor.setText("Por: " + nombreAutor);
            holder.tvAutor.setTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.color_acento_primario));
        }

        // Tiempo relativo seguro
        long timestamp = articulo.getTimestampCreacion() > 0 ? articulo.getTimestampCreacion() : System.currentTimeMillis();
        CharSequence tiempoRelativo = DateUtils.getRelativeTimeSpanString(
                timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
        );
        holder.tvTiempo.setText(" • " + tiempoRelativo);

        // Likes seguros
        int likesCount = (articulo.getLikes() != null) ? articulo.getLikes().size() : 0;
        holder.tvLikeCount.setText(String.valueOf(likesCount));

        boolean leHeDadoLike = currentUserId != null && articulo.getLikes() != null && articulo.getLikes().containsKey(currentUserId);
        if (leHeDadoLike) {
            holder.btnLike.setImageResource(R.drawable.ic_heart_filled);
            holder.btnLike.setColorFilter(android.graphics.Color.parseColor("#E53935"));
        } else {
            holder.btnLike.setImageResource(R.drawable.ic_heart_outline);
            holder.btnLike.setColorFilter(android.graphics.Color.parseColor("#757575"));
        }

        holder.btnLike.setOnClickListener(v -> {
            if (currentUserId == null || articulo.getId() == null) return;
            DatabaseReference likeRef = FirebaseDatabase.getInstance()
                    .getReference(articulo.isEsOficial() ? "articulos_oficiales" : "articulos_comunidad")
                    .child(articulo.getId()).child("likes").child(currentUserId);

            if (leHeDadoLike) likeRef.removeValue();
            else likeRef.setValue(true);
        });

        // 🛡️ AQUÍ ESTABA EL CRASH: Comprobamos que el ID del autor NO SEA NULO antes de compararlo
        String idAutor = articulo.getIdAutor();

        if (mostrarOpciones && currentUserId != null && idAutor != null && idAutor.equals(currentUserId)) {
            holder.btnMenu.setVisibility(View.VISIBLE);
            holder.btnMenu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                popup.getMenu().add("Editar artículo");
                popup.getMenu().add("Eliminar artículo");
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getTitle().equals("Editar artículo")) listener.onEditarClick(articulo);
                    else if (item.getTitle().equals("Eliminar artículo")) listener.onBorrarClick(articulo);
                    return true;
                });
                popup.show();
            });
        } else if (!articulo.isEsOficial() && currentUserId != null && idAutor != null && !idAutor.equals(currentUserId)) {
            holder.btnMenu.setVisibility(View.VISIBLE);
            holder.btnMenu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                popup.getMenu().add("🚩 Reportar artículo");
                popup.setOnMenuItemClickListener(item -> {
                    listener.onReportarClick(articulo);
                    return true;
                });
                popup.show();
            });
        } else {
            holder.btnMenu.setVisibility(View.GONE);
        }

        // Lógica Híbrida de Imagen segura
        if (articulo.getImagenPortadaBase64() != null && !articulo.getImagenPortadaBase64().isEmpty()) {
            if (articulo.getImagenPortadaBase64().startsWith("http")) {
                com.bumptech.glide.Glide.with(holder.itemView.getContext())
                        .load(articulo.getImagenPortadaBase64())
                        .into(holder.ivIcono);
            } else {
                try {
                    byte[] decodedString = Base64.decode(articulo.getImagenPortadaBase64(), Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    holder.ivIcono.setImageBitmap(decodedByte);
                } catch (Exception e) {
                    holder.ivIcono.setImageResource(R.drawable.consejos);
                }
            }
        } else {
            holder.ivIcono.setImageResource(R.drawable.consejos);
        }

        holder.itemView.setOnClickListener(v -> listener.onArticuloClick(articulo));
    }

    @Override
    public int getItemCount() {
        return listaArticulos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvDescripcion, tvAutor, tvLikeCount, tvTiempo;
        ImageView ivIcono;
        ImageButton btnLike, btnMenu;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTituloArticulo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionArticulo);
            tvAutor = itemView.findViewById(R.id.tvAutorArticulo);
            tvTiempo = itemView.findViewById(R.id.tvTiempoArticulo);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCountArticulo);
            ivIcono = itemView.findViewById(R.id.ivIconoArticulo);
            btnLike = itemView.findViewById(R.id.btnLikeArticulo);
            btnMenu = itemView.findViewById(R.id.btnMenuArticulo);
        }
    }
}