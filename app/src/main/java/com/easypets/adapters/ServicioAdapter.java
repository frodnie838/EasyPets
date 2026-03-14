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
import com.easypets.R;
import com.easypets.models.LocalServicio;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class ServicioAdapter extends RecyclerView.Adapter<ServicioAdapter.ServicioViewHolder> {

    private List<LocalServicio> listaServicios = new ArrayList<>();
    private Context context;

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

        // Cargar la foto con Glide
        Glide.with(context)
                .load(servicio.getFotoUrl())
                .placeholder(R.drawable.ic_launcher_background) // Imagen de carga temporal
                .into(holder.ivFoto);

        // El botón mágico para abrir Google Maps
        holder.btnVerMapa.setOnClickListener(v -> {
            String uriStr = "geo:" + servicio.getLatitud() + "," + servicio.getLongitud()
                    + "?q=" + servicio.getLatitud() + "," + servicio.getLongitud()
                    + "(" + Uri.encode(servicio.getNombre()) + ")";

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriStr));
            intent.setPackage("com.google.android.apps.maps");

            try {
                context.startActivity(intent);
            } catch (Exception e) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/search/?api=1&query="
                                + servicio.getLatitud() + "," + servicio.getLongitud()));
                context.startActivity(browserIntent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaServicios.size();
    }

    public static class ServicioViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFoto;
        TextView tvNombre, tvRating, tvResenas, tvDireccion;
        MaterialButton btnVerMapa;

        public ServicioViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFoto = itemView.findViewById(R.id.ivFotoServicio);
            tvNombre = itemView.findViewById(R.id.tvNombreServicio);
            tvRating = itemView.findViewById(R.id.tvRatingServicio);
            tvResenas = itemView.findViewById(R.id.tvResenasServicio);
            tvDireccion = itemView.findViewById(R.id.tvDireccionServicio);
            btnVerMapa = itemView.findViewById(R.id.btnVerMapaServicio);
        }
    }
}