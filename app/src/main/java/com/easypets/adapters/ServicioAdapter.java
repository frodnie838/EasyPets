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

/**
 * Adaptador para el RecyclerView encargado de renderizar los establecimientos locales.
 * Procesa la información estructurada obtenida de la API de Google Places, gestionando
 * el estado visual de apertura/cierre de los negocios y orquestando la navegación explícita
 * hacia la aplicación nativa de Google Maps (o su versión web como fallback de contingencia).
 */
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

        if (servicio.isTieneHorario()) {
            holder.tvEstado.setVisibility(View.VISIBLE);
            if (servicio.isAbiertoAhora()) {
                holder.tvEstado.setText("🟢 Abierto ahora");
                holder.tvEstado.setTextColor(Color.parseColor("#4CAF50"));
            } else {
                holder.tvEstado.setText("🔴 Cerrado");
                holder.tvEstado.setTextColor(Color.parseColor("#F44336"));
            }
        } else {
            holder.tvEstado.setVisibility(View.GONE);
        }

        Glide.with(context)
                .load(servicio.getFotoUrl())
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.ivFoto);

        holder.btnVerMapa.setOnClickListener(v -> {
            String busquedaEspecifica = servicio.getNombre() + ", " + servicio.getDireccion();
            String uriStr = "geo:0,0?q=" + Uri.encode(busquedaEspecifica);

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriStr));
            intent.setPackage("com.google.android.apps.maps");

            try {
                context.startActivity(intent);
            } catch (Exception e) {
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
        TextView tvNombre, tvRating, tvResenas, tvDireccion, tvEstado;
        MaterialButton btnVerMapa;

        public ServicioViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFoto = itemView.findViewById(R.id.ivFotoServicio);
            tvNombre = itemView.findViewById(R.id.tvNombreServicio);
            tvRating = itemView.findViewById(R.id.tvRatingServicio);
            tvResenas = itemView.findViewById(R.id.tvResenasServicio);
            tvDireccion = itemView.findViewById(R.id.tvDireccionServicio);
            tvEstado = itemView.findViewById(R.id.tvEstadoServicio);
            btnVerMapa = itemView.findViewById(R.id.btnVerMapaServicio);
        }
    }
}