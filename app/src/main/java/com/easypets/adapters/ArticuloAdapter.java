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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ArticuloAdapter extends RecyclerView.Adapter<ArticuloAdapter.ArticuloViewHolder> {
    private List<Articulo> articulos;
    private OnArticuloClickListener listener;
    private boolean mostrarOpciones = false; // ✨ Interruptor para los 3 puntos

    // ✨ Interfaz actualizada para manejar las nuevas acciones
    public interface OnArticuloClickListener {
        void onArticuloClick(Articulo articulo);
        void onEditarClick(Articulo articulo);
        void onBorrarClick(Articulo articulo);
    }

    public ArticuloAdapter(List<Articulo> articulos, OnArticuloClickListener listener) {
        this.articulos = articulos;
        this.listener = listener;
    }

    // ✨ Método para activar/desactivar el menú desde el fragmento
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

        // Manejo de imagen
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

        // ✨ LÓGICA DE LOS 3 PUNTITOS
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (mostrarOpciones && currentUser != null && articulo.getIdAutor().equals(currentUser.getUid())) {
            holder.btnMenu.setVisibility(View.VISIBLE);
            holder.btnMenu.setOnClickListener(v -> {
                android.widget.PopupMenu popup = new android.widget.PopupMenu(v.getContext(), v);
                popup.getMenu().add("Editar");
                popup.getMenu().add("Eliminar");

                popup.setOnMenuItemClickListener(item -> {
                    if (item.getTitle().equals("Editar")) {
                        listener.onEditarClick(articulo);
                    } else if (item.getTitle().equals("Eliminar")) {
                        listener.onBorrarClick(articulo);
                    }
                    return true;
                });
                popup.show();
            });
        } else {
            holder.btnMenu.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onArticuloClick(articulo));
    }

    @Override public int getItemCount() { return articulos.size(); }

    public static class ArticuloViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvDescripcion, tvAutor;
        ImageView ivIcono;
        ImageButton btnMenu; // ✨ Referencia al nuevo botón

        public ArticuloViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTituloArticulo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionArticulo);
            tvAutor = itemView.findViewById(R.id.tvAutorArticulo);
            ivIcono = itemView.findViewById(R.id.ivIconoArticulo);
            btnMenu = itemView.findViewById(R.id.btnMenuArticulo); // ✨ Lo vinculamos
        }
    }
}