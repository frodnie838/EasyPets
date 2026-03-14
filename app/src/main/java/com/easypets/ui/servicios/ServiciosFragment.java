package com.easypets.ui.servicios;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.easypets.R;
import com.easypets.ui.servicios.guarderias.GuarderiasFragment;
import com.easypets.ui.servicios.veterinarios.VeterinariosFragment;

public class ServiciosFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_servicios, container, false);

        // 1. Vinculamos las tarjetas y elementos del diseño
        CardView cardVeterinarios = root.findViewById(R.id.cardVeterinarios);
        CardView cardGuarderias = root.findViewById(R.id.cardGuarderias);
        CardView cardTiendas = root.findViewById(R.id.cardTiendas);
        CardView cardAdiestramiento = root.findViewById(R.id.cardAdiestramiento);
        LinearLayout searchBar = root.findViewById(R.id.searchBar);

        // --- LÓGICA DEL BUSCADOR GLOBAL (MVVM) ---
        EditText etTopSearch = requireActivity().findViewById(R.id.etTopSearch);
        if (etTopSearch != null) {
            etTopSearch.setHint("Escribe una ciudad y pulsa buscar...");

            // Instanciamos a nuestro "mensajero"
            BusquedaViewModel viewModel = new ViewModelProvider(requireActivity()).get(BusquedaViewModel.class);

            etTopSearch.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String ciudad = etTopSearch.getText().toString().trim();
                    if (!ciudad.isEmpty()) {
                        // Le pasamos la ciudad al mensajero
                        viewModel.buscarCiudad(ciudad);

                        // Ocultamos el teclado por comodidad
                        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(etTopSearch.getWindowToken(), 0);
                        }

                    }
                    return true;
                }
                return false;
            });
        }

        // 2. Programamos los clics (La "Magia")

        // Barra de búsqueda local (Si la pulsan, enfocamos la barra de arriba automáticamente)
        if (searchBar != null) {
            searchBar.setOnClickListener(v -> {
                if (etTopSearch != null) {
                    etTopSearch.requestFocus();
                    InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(etTopSearch, InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            });
        }

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