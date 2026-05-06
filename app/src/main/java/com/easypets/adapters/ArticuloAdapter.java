package com.easypets.adapters;

import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.easypets.R;
import com.easypets.models.Articulo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adaptador para el RecyclerView encargado de renderizar la lista de artículos.
 * Gestiona la visualización de artículos oficiales y comunitarios, integrando
 * soporte de imágenes híbrido (Base64 heredado y URLs de Firebase Storage),
 * además de implementar la lógica del botón "Me gusta" en tiempo real y el menú
 * contextual según los permisos del usuario (edición/eliminación o reportes).
 */
public class ArticuloAdapter extends RecyclerView.Adapter<ArticuloAdapter.ArticuloViewHolder> {

    private List<Articulo> articulos;
    private OnArticuloClickListener listener;
    private boolean mostrarOpciones = false;

    /**
     * Interfaz para la gestión delegada de eventos e interacciones del usuario
     * con los elementos que componen la lista de artículos.
     */
    public interface OnArticuloClickListener {
        void onArticuloClick(Articulo articulo);
        void onEditarClick(Articulo articulo);
        void onBorrarClick(Articulo articulo);
        void onReportarClick(Articulo articulo);
    }

    public ArticuloAdapter(List<Articulo> articulos, OnArticuloClickListener listener) {
        this.articulos = articulos;
        this.listener = listener;
    }

    public void setMostrarOpciones(boolean mostrarOpciones) {
        this.mostrarOpciones = mostrarOpciones;
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
            if (articulo.getImagenPortadaBase64().startsWith("http")) {
                com.bumptech.glide.Glide.with(holder.itemView.getContext())
                        .load(articulo.getImagenPortadaBase64())
                        .centerCrop()
                        .into(holder.ivIcono);
                holder.ivIcono.setImageTintList(null);
            } else {
                try {
                    byte[] decodedString = Base64.decode(articulo.getImagenPortadaBase64(), Base64.DEFAULT);
                    holder.ivIcono.setImageBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                    holder.ivIcono.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    holder.ivIcono.setImageTintList(null);
                } catch (Exception e) {
                    holder.ivIcono.setImageResource(articulo.isEsOficial() ? R.drawable.consejos : R.drawable.profile);
                }
            }
        } else {
            holder.ivIcono.setImageResource(articulo.isEsOficial() ? R.drawable.consejos : R.drawable.profile);
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserId = (currentUser != null) ? currentUser.getUid() : "";
        boolean soyAutor = currentUser != null && articulo.getIdAutor() != null && articulo.getIdAutor().equals(currentUserId);

        if (currentUser != null && (soyAutor || !articulo.isEsOficial())) {
            holder.btnMenu.setVisibility(View.VISIBLE);
            holder.btnMenu.setOnClickListener(v -> {
                android.widget.PopupMenu popup = new android.widget.PopupMenu(v.getContext(), v);

                if (soyAutor) {
                    popup.getMenu().add(0, 1, 0, "✏️ Editar");
                    popup.getMenu().add(0, 2, 0, "🗑️ Eliminar");
                } else {
                    popup.getMenu().add(0, 3, 0, "🚩 Reportar contenido inapropiado");
                }

                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 1) {
                        listener.onEditarClick(articulo);
                    } else if (item.getItemId() == 2) {
                        listener.onBorrarClick(articulo);
                    } else if (item.getItemId() == 3) {
                        listener.onReportarClick(articulo);
                    }
                    return true;
                });
                popup.show();
            });
        } else {
            holder.btnMenu.setVisibility(View.GONE);
        }

        int numeroDeLikes = 0;
        boolean isLikedByMe = false;

        if (articulo.getLikes() != null) {
            numeroDeLikes = articulo.getLikes().size();
            isLikedByMe = articulo.getLikes().containsKey(currentUserId);
        }

        holder.tvLikeCount.setVisibility(View.VISIBLE);
        holder.tvLikeCount.setText(String.valueOf(numeroDeLikes));

        if (isLikedByMe) {
            holder.btnLike.setImageResource(R.drawable.ic_heart_filled);
            holder.btnLike.setImageTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.color_acento_primario)));
        } else {
            holder.btnLike.setImageResource(R.drawable.ic_heart_outline);
        }

        boolean finalIsLikedByMe = isLikedByMe;
        holder.btnLike.setOnClickListener(v -> {
            if (currentUserId.isEmpty()) return;

            String nodoFirebase = articulo.isEsOficial() ? "articulos_oficiales" : "articulos_comunidad";

            DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference(nodoFirebase)
                    .child(articulo.getId()).child("likes").child(currentUserId);

            if (finalIsLikedByMe) {
                likesRef.removeValue();
            } else {
                likesRef.setValue(true);
            }
        });

        holder.itemView.setOnClickListener(v -> listener.onArticuloClick(articulo));
    }

    @Override public int getItemCount() { return articulos.size(); }

    public static class ArticuloViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvDescripcion, tvAutor, tvLikeCount;
        ImageView ivIcono;
        ImageButton btnMenu, btnLike;

        public ArticuloViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTituloArticulo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionArticulo);
            tvAutor = itemView.findViewById(R.id.tvAutorArticulo);
            ivIcono = itemView.findViewById(R.id.ivIconoArticulo);
            btnMenu = itemView.findViewById(R.id.btnMenuArticulo);
            btnLike = itemView.findViewById(R.id.btnLikeArticulo);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCountArticulo);
        }
    }
}