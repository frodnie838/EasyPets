package com.easypets.ui.servicios;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.easypets.R;
import com.easypets.ui.servicios.veterinarios.VeterinariosFragment;

public class ServiciosFragment extends Fragment { // Cambia el nombre si usas otro

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Asegúrate de que el nombre del layout coincida con tu XML (fragment_servicios o fragment_veterinarios)
        View root = inflater.inflate(R.layout.fragment_servicios, container, false);

        // 1. Vinculamos las tarjetas y elementos del diseño
        CardView cardVeterinarios = root.findViewById(R.id.cardVeterinarios);
        CardView cardGuarderias = root.findViewById(R.id.cardGuarderias);
        CardView cardTiendas = root.findViewById(R.id.cardTiendas);
        CardView cardAdiestramiento = root.findViewById(R.id.cardAdiestramiento);
        LinearLayout searchBar = root.findViewById(R.id.searchBar);

        // 2. Programamos los clics (La "Magia")

        // Barra de búsqueda (Futuro)
        searchBar.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Buscador próximamente...", Toast.LENGTH_SHORT).show();
        });

        // 🐶 Clínicas Veterinarias
        cardVeterinarios.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_container, new VeterinariosFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // 🏡 Guarderías
        cardGuarderias.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_container, new com.easypets.ui.servicios.guarderias.GuarderiasFragment())
                    .addToBackStack(null) // Para que funcione el botón de ir hacia atrás
                    .commit();
        });

        // 🛍️ Tiendas
        cardTiendas.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Módulo Tiendas en construcción \uD83D\uDEA7", Toast.LENGTH_SHORT).show();
        });

        // 🎓 Adiestramiento
        cardAdiestramiento.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Módulo Educación en construcción \uD83D\uDEA7", Toast.LENGTH_SHORT).show();
        });

        return root;
    }
}