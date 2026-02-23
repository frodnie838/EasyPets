package com.easypets.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.easypets.R;
import com.easypets.models.Evento;
import com.easypets.models.Mascota;

import java.util.ArrayList;
import java.util.List;

public class EventoAdapter extends RecyclerView.Adapter<EventoAdapter.EventoViewHolder> {

    private List<Evento> listaEventos = new ArrayList<>();
    private List<Mascota> listaMascotas = new ArrayList<>();

    public void setEventos(List<Evento> eventos) {
        this.listaEventos = eventos;
        notifyDataSetChanged();
    }

    public void setMascotas(List<Mascota> mascotas) {
        this.listaMascotas = mascotas;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_evento, parent, false);
        return new EventoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventoViewHolder holder, int position) {
        Evento evento = listaEventos.get(position);

        holder.tvTitulo.setText(evento.getTitulo());

        String detalle = "";
        if (evento.getHora() != null && !evento.getHora().isEmpty()) {
            detalle += evento.getHora() + " - ";
        }
        detalle += evento.getTipo();
        holder.tvDetalle.setText(detalle);

        String idMascota = evento.getIdMascota();
        if (idMascota != null && !idMascota.isEmpty()) {
            String nombreMascota = "Mascota borrada o desconocida";

            // Buscamos el nombre de la mascota por su ID
            for (Mascota m : listaMascotas) {
                if (m.getIdMascota() != null && m.getIdMascota().equals(idMascota)) {
                    nombreMascota = m.getNombre();
                    break;
                }
            }

            holder.tvMascota.setText("🐾 " + nombreMascota);
            holder.tvMascota.setVisibility(View.VISIBLE); // Lo hacemos visible
        } else {
            holder.tvMascota.setVisibility(View.GONE); // Si es un evento "General", lo ocultamos
        }

        // Icono
        if ("Veterinario".equals(evento.getTipo())) {
            holder.ivIcono.setImageResource(R.drawable.veterinario);
        } else if ("Peluquería".equals(evento.getTipo())) {
            holder.ivIcono.setImageResource(R.drawable.servicios);
        } else if ("Guardería".equals(evento.getTipo())) {
            holder.ivIcono.setImageResource(R.drawable.guarderia);
        } else {
            holder.ivIcono.setImageResource(R.drawable.nota);
        }
    }

    @Override
    public int getItemCount() {
        return listaEventos.size();
    }

    static class EventoViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvDetalle, tvMascota;
        ImageView ivIcono;

        public EventoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTituloEvento);
            tvDetalle = itemView.findViewById(R.id.tvDetalleEvento);
            tvMascota = itemView.findViewById(R.id.tvMascotaEvento);
            ivIcono = itemView.findViewById(R.id.ivTipoEvento);
        }
    }
}