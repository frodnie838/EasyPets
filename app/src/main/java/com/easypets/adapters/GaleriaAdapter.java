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

import java.util.List;

public class GaleriaAdapter extends RecyclerView.Adapter<GaleriaAdapter.ViewHolder> {

    private List<PublicacionMascota> listaPublicaciones;
    private String miUid;
    private OnGaleriaClickListener listener;

    public interface OnGaleriaClickListener {
        void onLikeClick(PublicacionMascota publicacion);
        void onComentarClick(PublicacionMascota publicacion); // ✨ NUEVO
        void onOpcionesClick(PublicacionMascota publicacion, View anchorView);
    }

    public GaleriaAdapter(List<PublicacionMascota> listaPublicaciones, OnGaleriaClickListener listener) {
        this.listaPublicaciones = listaPublicaciones;
        this.listener = listener;
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            miUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
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

        if (publicacion.getFotoBase64() != null && !publicacion.getFotoBase64().isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(publicacion.getFotoBase64(), Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.ivFoto.setImageBitmap(decodedByte);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        int totalLikes = (publicacion.getLikes() != null) ? publicacion.getLikes().size() : 0;
        holder.tvLikesCount.setText(String.valueOf(totalLikes));

        boolean leHeDadoLike = miUid != null && publicacion.getLikes() != null && publicacion.getLikes().containsKey(miUid);
        if (leHeDadoLike) {
            holder.btnLike.setImageResource(R.drawable.ic_heart_filled);
            holder.btnLike.setColorFilter(android.graphics.Color.parseColor("#E53935"));
        } else {
            holder.btnLike.setImageResource(R.drawable.ic_heart_outline);
            holder.btnLike.setColorFilter(android.graphics.Color.parseColor("#757575"));
        }

        if (miUid != null && miUid.equals(publicacion.getIdAutor())) {
            holder.btnOpciones.setVisibility(View.VISIBLE);
        } else {
            holder.btnOpciones.setVisibility(View.GONE);
        }

        holder.btnLike.setOnClickListener(v -> listener.onLikeClick(publicacion));
        holder.btnComentar.setOnClickListener(v -> listener.onComentarClick(publicacion)); // ✨ NUEVO
        holder.btnOpciones.setOnClickListener(v -> listener.onOpcionesClick(publicacion, v));
    }

    @Override
    public int getItemCount() {
        return listaPublicaciones.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreMascota, tvAutor, tvDescripcion, tvLikesCount;
        ImageView ivFoto;
        ImageButton btnLike, btnComentar, btnOpciones;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreMascota = itemView.findViewById(R.id.tvNombreMascotaGaleria);
            tvAutor = itemView.findViewById(R.id.tvAutorGaleria);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionGaleria);
            tvLikesCount = itemView.findViewById(R.id.tvLikesCountGaleria);
            ivFoto = itemView.findViewById(R.id.ivFotoGaleria);
            btnLike = itemView.findViewById(R.id.btnLikeGaleria);
            btnComentar = itemView.findViewById(R.id.btnComentarGaleria);
            btnOpciones = itemView.findViewById(R.id.btnOpcionesGaleria);
        }
    }
}