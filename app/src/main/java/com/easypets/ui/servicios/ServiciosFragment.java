package com.easypets.ui.servicios;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.easypets.R;
import com.easypets.ui.servicios.guarderias.GuarderiasFragment;
import com.easypets.ui.servicios.peluquerias.PeluqueriasFragment;
import com.easypets.ui.servicios.veterinarios.VeterinariosFragment;

public class ServiciosFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_servicios, container, false);

        // Vinculamos solo las tarjetas
        CardView cardVeterinarios = root.findViewById(R.id.cardVeterinarios);
        CardView cardGuarderias = root.findViewById(R.id.cardGuarderias);
        CardView cardTiendas = root.findViewById(R.id.cardTiendas);
        CardView cardPeluquerias = root.findViewById(R.id.cardPeluquerias);
        //CardView cardAdiestramiento = root.findViewById(R.id.cardAdiestramiento);

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
                    .replace(R.id.frame_container, new GuarderiasFragment())
                    .addToBackStack(null)
                    .commit();
        });

        cardPeluquerias.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_container, new PeluqueriasFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // 🛍️ Tiendas
        cardTiendas.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Módulo Tiendas en construcción \uD83D\uDEA7", Toast.LENGTH_SHORT).show();
        });

        // 🎓 Adiestramiento


        return root;
    }
}