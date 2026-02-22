package com.easypets.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.easypets.R;
import com.easypets.models.Mascota;

import java.util.List;

public class MascotaAdapter extends RecyclerView.Adapter<MascotaAdapter.MascotaViewHolder> {

    private List<Mascota> listaMascotas;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Mascota mascota);
    }

    public MascotaAdapter(List<Mascota> listaMascotas, OnItemClickListener listener) {
        this.listaMascotas = listaMascotas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MascotaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mascota, parent, false);
        return new MascotaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MascotaViewHolder holder, int position) {
        Mascota mascota = listaMascotas.get(position);

        holder.tvNombre.setText(mascota.getNombre());
        holder.tvRaza.setText(mascota.getEspecie() + " • " + mascota.getRaza());
        holder.tvPeso.setText("Peso: " + mascota.getPeso() + " kg");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(mascota);
            }
        });

        // ✨ LÓGICA DE FOTO CORREGIDA PARA LA LISTA ✨
        if (mascota.getFotoPerfilUrl() != null && !mascota.getFotoPerfilUrl().isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(mascota.getFotoPerfilUrl(), Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.ivFotoMascota.setImageBitmap(decodedByte);

                // Quitamos márgenes, quitamos tinte verde y ajustamos
                holder.ivFotoMascota.setPadding(0, 0, 0, 0);
                holder.ivFotoMascota.setImageTintList(null);
                holder.ivFotoMascota.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } catch (Exception e) {
                ponerHuellaPorDefecto(holder);
            }
        } else {
            ponerHuellaPorDefecto(holder);
        }
    }

    @Override
    public int getItemCount() {
        return listaMascotas.size();
    }

    // ✨ MÉTODO AUXILIAR PARA LA HUELLA ✨
    private void ponerHuellaPorDefecto(MascotaViewHolder holder) {
        Context context = holder.itemView.getContext();
        holder.ivFotoMascota.setImageResource(R.drawable.huella);
        holder.ivFotoMascota.setPadding(30, 30, 30, 30); // El padding original
        // Pintamos de nuevo la huella de verde
        holder.ivFotoMascota.setImageTintList(ContextCompat.getColorStateList(context, R.color.color_acento_primario));
        holder.ivFotoMascota.setScaleType(ImageView.ScaleType.FIT_CENTER);
    }

    public static class MascotaViewHolder extends RecyclerView.ViewHolder {
        public ImageView ivFotoMascota;
        TextView tvNombre, tvRaza, tvPeso;

        public MascotaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreFila);
            tvRaza = itemView.findViewById(R.id.tvRazaFila);
            tvPeso = itemView.findViewById(R.id.tvPesoFila);
            ivFotoMascota = itemView.findViewById(R.id.ivFotoMascota);
        }
    }
}