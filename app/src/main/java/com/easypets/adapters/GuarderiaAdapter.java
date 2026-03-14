package com.easypets.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.easypets.R;
import com.easypets.models.Guarderia;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class GuarderiaAdapter extends RecyclerView.Adapter<GuarderiaAdapter.ViewHolder> {

    private List<Guarderia> listaGuarderias;
    private Context context;

    public GuarderiaAdapter(List<Guarderia> listaGuarderias, Context context) {
        this.listaGuarderias = listaGuarderias;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_guarderia, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Guarderia guarderia = listaGuarderias.get(position);

        holder.tvNombre.setText(guarderia.getNombre());
        holder.tvRating.setText(String.valueOf(guarderia.getRating()));
        holder.tvUbicacion.setText(guarderia.getUbicacion());
        holder.tvTelefono.setText(guarderia.getTelefono()); // ✨ Nuevo

        Glide.with(context)
                .load(guarderia.getImageUrl())
                .placeholder(R.drawable.ic_launcher_background)
                .transform(new CenterCrop(), new RoundedCorners(24))
                .into(holder.ivFoto);

        holder.btnRuta.setOnClickListener(v -> {
            // "geo:latitud,longitud?q=Nombre del sitio" le dice a Maps exactamente dónde poner la chincheta
            String uriMapas = "geo:" + guarderia.getLatitud() + "," + guarderia.getLongitud() + "?q=" + Uri.encode(guarderia.getNombre());
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriMapas));

            // Intentamos abrir Google Maps. Si no lo tiene instalado, abrirá el navegador.
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(mapIntent);
            } else {
                // Fallback si no hay Google Maps (abre en el navegador web)
                String webMaps = "https://maps.google.com/?q=" + guarderia.getLatitud() + "," + guarderia.getLongitud();
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webMaps)));
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaGuarderias.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFoto;
        TextView tvNombre, tvRating, tvUbicacion, tvTelefono;
        MaterialButton btnRuta;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFoto = itemView.findViewById(R.id.ivFotoGuarderia);
            tvNombre = itemView.findViewById(R.id.tvNombreGuarderia);
            tvRating = itemView.findViewById(R.id.tvRatingGuarderia);
            tvUbicacion = itemView.findViewById(R.id.tvUbicacionGuarderia);
            tvTelefono = itemView.findViewById(R.id.tvTelefonoGuarderia);
            btnRuta = itemView.findViewById(R.id.btnRutaGuarderia);
        }
    }
}