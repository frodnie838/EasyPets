package com.easypets.ui.servicios.guarderias;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.easypets.R;

public class GuarderiasFragment extends Fragment {

    private RecyclerView rvGuarderias;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_guarderias, container, false);

        // Configurar RecyclerView
        rvGuarderias = root.findViewById(R.id.rvGuarderias);
        rvGuarderias.setLayoutManager(new LinearLayoutManager(getContext()));

        // Aquí conectaremos el adaptador con Yelp mañana

        return root;
    }
}