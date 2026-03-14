package com.easypets.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.easypets.R;
import com.easypets.models.LocalServicio;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class ServicioAdapter extends RecyclerView.Adapter<ServicioAdapter.ServicioViewHolder> {

    private List<LocalServicio> listaServicios = new ArrayList<>();
    private final Context context;

    public ServicioAdapter(Context context) {
        this.context = context;
    }

    public void setServicios(List<LocalServicio> lista) {
        this.listaServicios = lista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ServicioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_servicio, parent, false);
        return new ServicioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServicioViewHolder holder, int position) {
        LocalServicio servicio = listaServicios.get(position);

        holder.tvNombre.setText(servicio.getNombre());
        holder.tvDireccion.setText(servicio.getDireccion());
        holder.tvRating.setText(servicio.getRating() + " ⭐");
        holder.tvResenas.setText("(" + servicio.getTotalResenas() + " opiniones)");

        // --- LÓGICA DE ABIERTO / CERRADO ---
        if (servicio.isTieneHorario()) {
            holder.tvEstado.setVisibility(View.VISIBLE);
            if (servicio.isAbiertoAhora()) {
                holder.tvEstado.setText("🟢 Abierto ahora");
                holder.tvEstado.setTextColor(Color.parseColor("#4CAF50")); // Verde
            } else {
                holder.tvEstado.setText("🔴 Cerrado");
                holder.tvEstado.setTextColor(Color.parseColor("#F44336")); // Rojo
            }
        } else {
            holder.tvEstado.setVisibility(View.GONE); // Si no hay datos, lo ocultamos para que quede limpio
        }

        // Cargar la foto con Glide de forma limpia
        Glide.with(context)
                .load(servicio.getFotoUrl())
                .placeholder(R.drawable.ic_launcher_background) // Imagen de carga temporal
                .into(holder.ivFoto);

        // El botón mágico para abrir Google Maps
        holder.btnVerMapa.setOnClickListener(v -> {
            // Buscamos el lugar exacto para que abra la ficha con fotos y teléfono
            String busquedaEspecifica = servicio.getNombre() + ", " + servicio.getDireccion();
            String uriStr = "geo:0,0?q=" + Uri.encode(busquedaEspecifica);

            // Creamos la acción.
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriStr));
            intent.setPackage("com.google.android.apps.maps"); // Forzamos a que use Google Maps

            try {
                context.startActivity(intent);
            } catch (Exception e) {
                // Si falla (no tiene Google Maps instalado), abrimos la web oficial de Maps con la misma búsqueda
                String fallbackUrl = "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(busquedaEspecifica);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl));
                context.startActivity(browserIntent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaServicios != null ? listaServicios.size() : 0;
    }

    public static class ServicioViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFoto;
        TextView tvNombre, tvRating, tvResenas, tvDireccion, tvEstado; // Añadido el estado
        MaterialButton btnVerMapa;

        public ServicioViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFoto = itemView.findViewById(R.id.ivFotoServicio);
            tvNombre = itemView.findViewById(R.id.tvNombreServicio);
            tvRating = itemView.findViewById(R.id.tvRatingServicio);
            tvResenas = itemView.findViewById(R.id.tvResenasServicio);
            tvDireccion = itemView.findViewById(R.id.tvDireccionServicio);
            tvEstado = itemView.findViewById(R.id.tvEstadoServicio); // Vinculamos el estado con el XML
            btnVerMapa = itemView.findViewById(R.id.btnVerMapaServicio);
        }
    }
}