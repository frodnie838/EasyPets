package com.easypets.ui.mascotas;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.easypets.R;
import com.easypets.adapters.MascotaAdapter;
import com.easypets.models.Mascota;
import com.easypets.repositories.MascotaRepository;
import com.easypets.ui.auth.LoginActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class MascotasFragment extends Fragment {

    private View layoutLogueado;
    private View layoutInvitado;
    private Button btnIrALogin;

    private View layoutEstadoVacio;
    private Button btnAñadirPrimeraMascota;
    private FloatingActionButton fabAgregarMascota;

    private RecyclerView recyclerViewMascotas;
    private MascotaAdapter mascotaAdapter;
    private List<Mascota> listaMascotas;

    private MascotaRepository mascotaRepository;

    public MascotasFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mascotas, container, false);

        layoutLogueado = view.findViewById(R.id.layoutUsuarioLogueado);
        layoutInvitado = view.findViewById(R.id.layoutInvitado);
        btnIrALogin = view.findViewById(R.id.btnIrALogin);

        layoutEstadoVacio = view.findViewById(R.id.layoutEstadoVacio);
        btnAñadirPrimeraMascota = view.findViewById(R.id.btnAñadirPrimeraMascota);
        fabAgregarMascota = view.findViewById(R.id.fabAgregarMascota);

        mascotaRepository = new MascotaRepository();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Si hay usuario y NO es un invitado (anónimo)
        if (user != null && !user.isAnonymous()) {
            layoutLogueado.setVisibility(View.VISIBLE);
            layoutInvitado.setVisibility(View.GONE);

            View.OnClickListener listenerIrAAñadir = v -> {
                Intent intent = new Intent(getActivity(), AgregarMascotaActivity.class);
                startActivity(intent);
            };
            fabAgregarMascota.setOnClickListener(listenerIrAAñadir);
            btnAñadirPrimeraMascota.setOnClickListener(listenerIrAAñadir);

            recyclerViewMascotas = view.findViewById(R.id.recyclerViewMascotas);
            recyclerViewMascotas.setLayoutManager(new LinearLayoutManager(getContext()));

            listaMascotas = new ArrayList<>();

            mascotaAdapter = new MascotaAdapter(listaMascotas, mascotaClicada -> {
                Intent intent = new Intent(getActivity(), MascotaDetalleActivity.class);
                intent.putExtra("idMascota", mascotaClicada.getIdMascota());
                intent.putExtra("nombreMascota", mascotaClicada.getNombre());
                startActivity(intent);
            });

            recyclerViewMascotas.setAdapter(mascotaAdapter);
            cargarMascotas(user.getUid());

        } else {
            // Si es un invitado o no hay sesión
            layoutLogueado.setVisibility(View.GONE);
            layoutInvitado.setVisibility(View.VISIBLE);

            btnIrALogin.setOnClickListener(v -> startActivity(new Intent(getActivity(), LoginActivity.class)));
        }

        return view;
    }

    private void cargarMascotas(String uid) {
        mascotaRepository.escucharMascotas(uid, new MascotaRepository.LeerMascotasCallback() {
            @Override
            public void onResultado(List<Mascota> mascotasDescargadas) {
                listaMascotas.clear();
                listaMascotas.addAll(mascotasDescargadas);
                mascotaAdapter.notifyDataSetChanged();

                if (listaMascotas.isEmpty()) {
                    recyclerViewMascotas.setVisibility(View.GONE);
                    layoutEstadoVacio.setVisibility(View.VISIBLE);
                } else {
                    recyclerViewMascotas.setVisibility(View.VISIBLE);
                    layoutEstadoVacio.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error al cargar mascotas: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mascotaRepository != null) {
            mascotaRepository.detenerEscucha();
        }
    }
}