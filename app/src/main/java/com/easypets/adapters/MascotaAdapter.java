package com.easypets.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.easypets.R;
import com.easypets.models.Mascota;

import java.util.List;

public class MascotaAdapter extends RecyclerView.Adapter<MascotaAdapter.MascotaViewHolder> {

    private List<Mascota> listaMascotas;
    private OnItemClickListener listener; // NUEVO: El escuchador de clics

    // NUEVO: Creamos una interfaz para comunicar el clic a la pantalla
    public interface OnItemClickListener {
        void onItemClick(Mascota mascota);
    }

    // Constructor actualizado (ahora pide la lista y el escuchador)
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

        // ✨ NUEVO: Le decimos a la fila entera que reaccione al clic ✨
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(mascota);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaMascotas.size();
    }

    public static class MascotaViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvRaza, tvPeso;

        public MascotaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreFila);
            tvRaza = itemView.findViewById(R.id.tvRazaFila);
            tvPeso = itemView.findViewById(R.id.tvPesoFila);
        }
    }
}