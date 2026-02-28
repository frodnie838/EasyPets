package com.easypets.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.easypets.R;
import com.easypets.models.Veterinario;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class VeterinarioAdapter extends RecyclerView.Adapter<VeterinarioAdapter.VeterinarioViewHolder> {

    private List<Veterinario> listaVeterinarios = new ArrayList<>();

    public void setVeterinarios(List<Veterinario> veterinarios) {
        this.listaVeterinarios = veterinarios;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VeterinarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_veterinario, parent, false);
        return new VeterinarioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VeterinarioViewHolder holder, int position) {
        Veterinario vet = listaVeterinarios.get(position);
        Context context = holder.itemView.getContext();

        // Rellenar Textos
        holder.tvNombre.setText(vet.getNombre() != null && !vet.getNombre().isEmpty() ? vet.getNombre() : "Clínica Veterinaria");
        holder.tvDireccion.setText(vet.getDireccion() != null && !vet.getDireccion().isEmpty() ? vet.getDireccion() : "Dirección no disponible");
        holder.tvTelefono.setText(vet.getTelefono() != null && !vet.getTelefono().isEmpty() ? vet.getTelefono() : "Sin teléfono");

        // "Ver en Mapa"
        holder.btnMapa.setOnClickListener(v -> {
            String uri = "geo:" + vet.getLatitud() + "," + vet.getLongitud() + "?q=" + vet.getLatitud() + "," + vet.getLongitud() + "(" + vet.getNombre() + ")";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return listaVeterinarios.size();
    }

    static class VeterinarioViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvDireccion, tvTelefono;
        MaterialButton btnMapa;

        public VeterinarioViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreVeterinario);
            tvDireccion = itemView.findViewById(R.id.tvDireccionVeterinario);
            tvTelefono = itemView.findViewById(R.id.tvTelefonoVeterinario);
            btnMapa = itemView.findViewById(R.id.btnMapaVeterinario);
        }
    }
}