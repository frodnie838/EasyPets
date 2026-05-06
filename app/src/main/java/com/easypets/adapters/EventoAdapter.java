package com.easypets.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.easypets.R;
import com.easypets.models.Evento;
import com.easypets.models.Mascota;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adaptador para el RecyclerView encargado de renderizar la agenda de eventos.
 * Vincula dinámicamente la información del evento con el listado de mascotas del usuario
 * para resolver dependencias de nombres. Aplica lógica de parseo temporal para
 * formatear el calendario y altera el estilo visual si detecta que la cita ha caducado.
 */
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
        Context context = holder.itemView.getContext();

        holder.tvTitulo.setText(evento.getTitulo());

        String fechaOriginal = evento.getFecha();
        boolean esPasado = false;

        if (fechaOriginal != null && fechaOriginal.contains("/")) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date fechaDelEvento = sdf.parse(fechaOriginal);

                Calendar hoyCal = Calendar.getInstance();
                hoyCal.set(Calendar.HOUR_OF_DAY, 0);
                hoyCal.set(Calendar.MINUTE, 0);
                hoyCal.set(Calendar.SECOND, 0);
                hoyCal.set(Calendar.MILLISECOND, 0);

                if (fechaDelEvento != null && fechaDelEvento.getTime() < hoyCal.getTimeInMillis()) {
                    esPasado = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            String[] partes = fechaOriginal.split("/");
            if (partes.length >= 2) {
                String[] meses = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
                try {
                    String dia = partes[0];
                    int mesIndex = Integer.parseInt(partes[1]) - 1;
                    String mes = meses[mesIndex];

                    holder.tvDia.setText(dia);
                    holder.tvMes.setText(mes);
                } catch (Exception e) {
                    holder.tvDia.setText("-");
                    holder.tvMes.setText("-");
                }
            } else {
                holder.tvDia.setText(fechaOriginal);
                holder.tvMes.setText("");
            }
        } else {
            holder.tvDia.setText("?");
            holder.tvMes.setText("");
        }

        if (esPasado) {
            holder.tvDia.setTextColor(ContextCompat.getColor(context, R.color.red));
            holder.tvMes.setTextColor(ContextCompat.getColor(context, R.color.red));
        } else {
            holder.tvDia.setTextColor(ContextCompat.getColor(context, R.color.color_acento_primario));
            holder.tvMes.setTextColor(ContextCompat.getColor(context, R.color.grey));
        }

        String detalle = "";
        if (evento.getHora() != null && !evento.getHora().isEmpty()) {
            detalle += evento.getHora() + " - ";
        }
        detalle += evento.getTipo();
        holder.tvDetalle.setText(detalle);

        String idMascota = evento.getIdMascota();
        if (idMascota != null && !idMascota.isEmpty()) {
            String nombreMascota = "Mascota borrada o desconocida";

            for (Mascota m : listaMascotas) {
                if (m.getIdMascota() != null && m.getIdMascota().equals(idMascota)) {
                    nombreMascota = m.getNombre();
                    break;
                }
            }

            holder.tvMascota.setText("🐾 " + nombreMascota);
            holder.tvMascota.setVisibility(View.VISIBLE);
        } else {
            holder.tvMascota.setVisibility(View.GONE);
        }

        if ("Veterinario".equals(evento.getTipo())) {
            holder.ivIcono.setImageResource(R.drawable.veterinario);
        } else if ("Peluquería".equals(evento.getTipo())) {
            holder.ivIcono.setImageResource(R.drawable.peluqueria);
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

    public Evento getEventoAt(int position) {
        return listaEventos.get(position);
    }

    static class EventoViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvDia, tvMes, tvDetalle, tvMascota;
        ImageView ivIcono;

        public EventoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTituloEvento);
            tvDia = itemView.findViewById(R.id.tvDiaEvento);
            tvMes = itemView.findViewById(R.id.tvMesEvento);
            tvDetalle = itemView.findViewById(R.id.tvDetalleEvento);
            tvMascota = itemView.findViewById(R.id.tvMascotaEvento);
            ivIcono = itemView.findViewById(R.id.ivTipoEvento);
        }
    }
}