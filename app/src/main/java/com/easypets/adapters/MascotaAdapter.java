package com.easypets.adapters;

import android.content.Context;
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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.easypets.R;
import com.easypets.models.Mascota;

import java.util.List;

/**
 * Adaptador para el RecyclerView responsable de la representación visual del listado de mascotas.
 * Incorpora una estrategia de carga híbrida para los avatares (soportando URLs de Firebase Storage
 * y cadenas Base64 por retrocompatibilidad), además de gestionar previsualizaciones a pantalla
 * completa y delegar eventos de navegación mediante interfaces.
 */
public class MascotaAdapter extends RecyclerView.Adapter<MascotaAdapter.MascotaViewHolder> {

    private List<Mascota> listaMascotas;
    private OnItemClickListener listener;

    /**
     * Interfaz de comunicación para delegar los eventos de selección
     * sobre un elemento específico de la colección de mascotas.
     */
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

        if (mascota.getFotoPerfilUrl() != null && !mascota.getFotoPerfilUrl().isEmpty()) {
            if (mascota.getFotoPerfilUrl().startsWith("http")) {
                com.bumptech.glide.Glide.with(holder.itemView.getContext())
                        .load(mascota.getFotoPerfilUrl())
                        .centerCrop()
                        .into(holder.ivFotoMascota);

                holder.ivFotoMascota.setPadding(0, 0, 0, 0);
                holder.ivFotoMascota.setImageTintList(null);
                holder.ivFotoMascota.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                try {
                    byte[] decodedString = Base64.decode(mascota.getFotoPerfilUrl(), Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    holder.ivFotoMascota.setImageBitmap(decodedByte);

                    holder.ivFotoMascota.setPadding(0, 0, 0, 0);
                    holder.ivFotoMascota.setImageTintList(null);
                    holder.ivFotoMascota.setScaleType(ImageView.ScaleType.CENTER_CROP);
                } catch (Exception e) {
                    ponerHuellaPorDefecto(holder);
                }
            }
        } else {
            ponerHuellaPorDefecto(holder);
        }

        holder.ivFotoMascota.setOnClickListener(v -> {
            if (holder.ivFotoMascota.getDrawable() != null) {
                mostrarFotoGrande(v.getContext(), holder.ivFotoMascota.getDrawable());
            }
        });
    }

    /**
     * Despliega un diálogo inmersivo sin bordes para la previsualización ampliada
     * de la fotografía asociada al perfil de la mascota.
     *
     * @param context  Contexto subyacente de la vista actual.
     * @param drawable Recurso gráfico a renderizar en pantalla completa.
     */
    private void mostrarFotoGrande(Context context, android.graphics.drawable.Drawable drawable) {
        android.app.Dialog dialog = new android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_ver_foto);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        ImageView ivGrande = dialog.findViewById(R.id.ivFotoGrande);
        ImageButton btnCerrar = dialog.findViewById(R.id.btnCerrarFoto);

        ivGrande.setImageDrawable(drawable);

        btnCerrar.setOnClickListener(v -> dialog.dismiss());
        ivGrande.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    public int getItemCount() {
        return listaMascotas.size();
    }

    /**
     * Aplica un estado visual por defecto (placeholder vectorial) con estilos específicos
     * en los casos donde la mascota no posea avatar o el procesamiento de este falle.
     *
     * @param holder El ViewHolder objetivo sobre el cual aplicar la configuración.
     */
    private void ponerHuellaPorDefecto(MascotaViewHolder holder) {
        Context context = holder.itemView.getContext();
        holder.ivFotoMascota.setImageResource(R.drawable.huella);
        holder.ivFotoMascota.setPadding(30, 30, 30, 30);
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