package com.easypets.adapters; // Asegúrate de que este es tu paquete correcto

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

    // Constructor: le pasamos la lista de mascotas cuando creamos el adaptador
    public MascotaAdapter(List<Mascota> listaMascotas) {
        this.listaMascotas = listaMascotas;
    }

    @NonNull
    @Override
    public MascotaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Aquí le decimos qué diseño XML usar para cada fila (nuestro item_mascota)
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mascota, parent, false);
        return new MascotaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MascotaViewHolder holder, int position) {
        // Aquí "pintamos" los datos de la mascota en su fila correspondiente
        Mascota mascota = listaMascotas.get(position);

        holder.tvNombre.setText(mascota.getNombre());
        holder.tvRaza.setText(mascota.getEspecie() + " • " + mascota.getRaza());
        holder.tvPeso.setText("Peso: " + mascota.getPeso() + " kg");
    }

    @Override
    public int getItemCount() {
        return listaMascotas.size(); // Le dice a Android cuántas filas tiene que dibujar
    }

    // Esta clase "sujeta" las vistas de la tarjeta para no tener que buscarlas todo el rato
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