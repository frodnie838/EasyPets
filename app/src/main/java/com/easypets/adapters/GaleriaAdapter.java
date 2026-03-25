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

import java.util.HashMap;
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

        CharSequence tiempoRelativo = DateUtils.getRelativeTimeSpanString(
                publicacion.getTimestamp(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
        );
        holder.tvTiempo.setText(tiempoRelativo);

        if (publicacion.getFotoBase64() != null && !publicacion.getFotoBase64().isEmpty()) {
            if (publicacion.getFotoBase64().startsWith("http")) {
                com.bumptech.glide.Glide.with(holder.itemView.getContext())
                        .load(publicacion.getFotoBase64())
                        .into(holder.ivFoto);
            } else {
                try {
                    byte[] decodedString = Base64.decode(publicacion.getFotoBase64(), Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    holder.ivFoto.setImageBitmap(decodedByte);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            holder.ivFoto.setImageResource(R.drawable.logo_sin_letra_trans);
        }

        int totalLikes = (publicacion.getLikes() != null) ? publicacion.getLikes().size() : 0;
        holder.tvLikesCount.setText(String.valueOf(totalLikes));

        if (holder.comentariosRef != null && holder.comentariosListener != null) {
            holder.comentariosRef.removeEventListener(holder.comentariosListener);
        }

        holder.comentariosRef = FirebaseDatabase.getInstance().getReference("mascotas_comentarios").child(publicacion.getId());
        holder.comentariosListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long contadorReal = snapshot.getChildrenCount();
                holder.tvComentariosCount.setText(String.valueOf(contadorReal));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        holder.comentariosRef.addValueEventListener(holder.comentariosListener);

        boolean leHeDadoLike = miUid != null && publicacion.getLikes() != null && publicacion.getLikes().containsKey(miUid);

        // Limpiamos los colores por defecto del diseño antes de aplicar el nuevo
        holder.btnLike.setImageTintList(null);

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

        // ✨ NUEVA LÓGICA DE LIKE: ACTUALIZACIÓN OPTIMISTA ✨
        holder.btnLike.setOnClickListener(v -> {
            if (miUid == null) return;

            // 1. Envía la orden real a Firebase a través del Fragment
            listener.onLikeClick(publicacion);

            // 2. Modifica los datos localmente al instante
            if (publicacion.getLikes() == null) {
                publicacion.setLikes(new HashMap<>());
            }

            if (publicacion.getLikes().containsKey(miUid)) {
                publicacion.getLikes().remove(miUid); // Le quita el Like local
            } else {
                publicacion.getLikes().put(miUid, true); // Le da el Like local
            }

            // 3. Ordena repintar SOLO esta tarjeta (es súper rápido y fluido)
            notifyItemChanged(position);
        });

        holder.btnComentar.setOnClickListener(v -> listener.onComentarClick(publicacion));
        holder.btnOpciones.setOnClickListener(v -> listener.onOpcionesClick(publicacion, v));
    }

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
        TextView tvNombreMascota, tvAutor, tvDescripcion, tvLikesCount, tvComentariosCount, tvTiempo;
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
            tvTiempo = itemView.findViewById(R.id.tvTiempoGaleria);
            ivFoto = itemView.findViewById(R.id.ivFotoGaleria);
            btnLike = itemView.findViewById(R.id.btnLikeGaleria);
            btnComentar = itemView.findViewById(R.id.btnComentarGaleria);
            btnOpciones = itemView.findViewById(R.id.btnOpcionesGaleria);
        }
    }
}