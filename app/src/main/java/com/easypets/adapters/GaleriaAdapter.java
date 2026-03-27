package com.easypets.adapters;

import android.graphics.Bitmap;
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
import com.easypets.models.PublicacionMascota;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class GaleriaAdapter extends RecyclerView.Adapter<GaleriaAdapter.ViewHolder> {

    private List<PublicacionMascota> listaPublicaciones;
    private String miUid;
    private OnGaleriaClickListener listener;
    private boolean mostrarOpciones = false;

    public interface OnGaleriaClickListener {
        void onLikeClick(PublicacionMascota publicacion);
        void onComentarClick(PublicacionMascota publicacion);
        void onOpcionesClick(PublicacionMascota publicacion, View anchorView);
    }

    public GaleriaAdapter(List<PublicacionMascota> listaPublicaciones, OnGaleriaClickListener listener) {
        this.listaPublicaciones = listaPublicaciones;
        this.listener = listener;
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            miUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    public void setMostrarOpciones(boolean mostrarOpciones) {
        this.mostrarOpciones = mostrarOpciones;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_galeria, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PublicacionMascota publicacion = listaPublicaciones.get(position);

        holder.tvNombreMascota.setText(publicacion.getNombreMascota());
        holder.tvAutor.setText(publicacion.getAutorNick());
        holder.tvDescripcion.setText(publicacion.getDescripcion());

        // Carga URLs modernas o Base64 antiguos
        if (publicacion.getFotoBase64() != null && !publicacion.getFotoBase64().isEmpty()) {
            if (publicacion.getFotoBase64().startsWith("http")) {
                // Es un enlace de Firebase Storage: Usamos la librería Glide (súper rápida)
                com.bumptech.glide.Glide.with(holder.itemView.getContext())
                        .load(publicacion.getFotoBase64())
                        .into(holder.ivFoto);
            } else {
                // Es una foto antigua en Base64: La decodificamos como antes
                try {
                    byte[] decodedString = Base64.decode(publicacion.getFotoBase64(), Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    holder.ivFoto.setImageBitmap(decodedByte);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            // Imagen por defecto si falla
            holder.ivFoto.setImageResource(R.drawable.logo_sin_letra_trans);
        }

        int totalLikes = (publicacion.getLikes() != null) ? publicacion.getLikes().size() : 0;
        holder.tvLikesCount.setText(String.valueOf(totalLikes));

        // 1. Si la tarjeta se está reciclando (el usuario hace scroll rápido), matamos el oyente viejo
        if (holder.comentariosRef != null && holder.comentariosListener != null) {
            holder.comentariosRef.removeEventListener(holder.comentariosListener);
        }

        // 2. Le ponemos la oreja a Firebase solo para esta foto exacta
        holder.comentariosRef = FirebaseDatabase.getInstance().getReference("mascotas_comentarios").child(publicacion.getId());
        holder.comentariosListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Cuenta exactamente cuántos comentarios hijos hay ahora mismo
                long contadorReal = snapshot.getChildrenCount();
                holder.tvComentariosCount.setText(String.valueOf(contadorReal));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        holder.comentariosRef.addValueEventListener(holder.comentariosListener);


        boolean leHeDadoLike = miUid != null && publicacion.getLikes() != null && publicacion.getLikes().containsKey(miUid);
        if (leHeDadoLike) {
            holder.btnLike.setImageResource(R.drawable.ic_heart_filled);
            holder.btnLike.setColorFilter(android.graphics.Color.parseColor("#E53935"));
        } else {
            holder.btnLike.setImageResource(R.drawable.ic_heart_outline);
            holder.btnLike.setColorFilter(android.graphics.Color.parseColor("#757575"));
        }

        if (miUid != null) {
            holder.btnOpciones.setVisibility(View.VISIBLE);
        } else {
            holder.btnOpciones.setVisibility(View.GONE);
        }

        holder.btnLike.setOnClickListener(v -> listener.onLikeClick(publicacion));
        holder.btnComentar.setOnClickListener(v -> listener.onComentarClick(publicacion));
        holder.btnOpciones.setOnClickListener(v -> listener.onOpcionesClick(publicacion, v));
    }

    // Evitar fugas de memoria
    // Cuando el usuario baja la pantalla y la foto deja de verse, desenchufamos la conexión
    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.comentariosRef != null && holder.comentariosListener != null) {
            holder.comentariosRef.removeEventListener(holder.comentariosListener);
        }
    }

    @Override
    public int getItemCount() {
        return listaPublicaciones.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreMascota, tvAutor, tvDescripcion, tvLikesCount, tvComentariosCount;
        ImageView ivFoto;
        ImageButton btnLike, btnComentar, btnOpciones;

        DatabaseReference comentariosRef;
        ValueEventListener comentariosListener;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreMascota = itemView.findViewById(R.id.tvNombreMascotaGaleria);
            tvAutor = itemView.findViewById(R.id.tvAutorGaleria);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionGaleria);
            tvLikesCount = itemView.findViewById(R.id.tvLikesCountGaleria);
            tvComentariosCount = itemView.findViewById(R.id.tvComentariosCountGaleria);
            ivFoto = itemView.findViewById(R.id.ivFotoGaleria);
            btnLike = itemView.findViewById(R.id.btnLikeGaleria);
            btnComentar = itemView.findViewById(R.id.btnComentarGaleria);
            btnOpciones = itemView.findViewById(R.id.btnOpcionesGaleria);
        }
    }
}