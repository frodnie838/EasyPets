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

    // --- Vistas Base ---
    private View layoutLogueado;
    private View layoutInvitado;
    private Button btnIrALogin;

    // --- Vistas del Estado Vacío (Empty State) ---
    private View layoutEstadoVacio;
    private Button btnAñadirPrimeraMascota;
    private FloatingActionButton fabAgregarMascota;

    // --- Lista y Adaptador ---
    private RecyclerView recyclerViewMascotas;
    private MascotaAdapter mascotaAdapter;
    private List<Mascota> listaMascotas;

    // --- Repositorio (Nuestra conexión limpia con Firebase) ---
    private MascotaRepository mascotaRepository;

    public MascotasFragment() {
        // Constructor vacío requerido por Android
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mascotas, container, false);

        // 1. Vincular las vistas de los Layouts
        layoutLogueado = view.findViewById(R.id.layoutUsuarioLogueado);
        layoutInvitado = view.findViewById(R.id.layoutInvitado);
        btnIrALogin = view.findViewById(R.id.btnIrALogin);

        layoutEstadoVacio = view.findViewById(R.id.layoutEstadoVacio);
        btnAñadirPrimeraMascota = view.findViewById(R.id.btnAñadirPrimeraMascota);
        fabAgregarMascota = view.findViewById(R.id.fabAgregarMascota);

        // 2. Inicializar el Repositorio y comprobar el Usuario
        mascotaRepository = new MascotaRepository();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            // ==========================================
            // EL USUARIO ESTÁ LOGUEADO
            // ==========================================
            layoutLogueado.setVisibility(View.VISIBLE);
            layoutInvitado.setVisibility(View.GONE);

            // A) Configurar el botón flotante (+) y el botón central (Estado vacío)
            View.OnClickListener listenerIrAAñadir = v -> {
                Intent intent = new Intent(getActivity(), AgregarMascotaActivity.class);
                startActivity(intent);
            };
            fabAgregarMascota.setOnClickListener(listenerIrAAñadir);
            btnAñadirPrimeraMascota.setOnClickListener(listenerIrAAñadir);

            // B) Configurar la lista (RecyclerView)
            recyclerViewMascotas = view.findViewById(R.id.recyclerViewMascotas);
            recyclerViewMascotas.setLayoutManager(new LinearLayoutManager(getContext()));

            listaMascotas = new ArrayList<>();

            // C)  LA MAGIA DEL CLIC EN LA TARJETA
            mascotaAdapter = new MascotaAdapter(listaMascotas, mascotaClicada -> {
                // 1. Preparamos el viaje a la nueva pantalla (Ficha de la mascota)
                Intent intent = new Intent(getActivity(), MascotaDetalleActivity.class);

                // 2. Pasamos los datos esenciales en la "mochila" del Intent
                intent.putExtra("idMascota", mascotaClicada.getIdMascota());
                intent.putExtra("nombreMascota", mascotaClicada.getNombre());

                // 3. Viajamos
                startActivity(intent);
            });

            recyclerViewMascotas.setAdapter(mascotaAdapter);

            // D) Le pedimos al repositorio que descargue las mascotas en tiempo real
            cargarMascotas(user.getUid());

        } else {
            // ==========================================
            // EL USUARIO ES UN INVITADO
            // ==========================================
            layoutLogueado.setVisibility(View.GONE);
            layoutInvitado.setVisibility(View.VISIBLE);

            btnIrALogin.setOnClickListener(v -> {
                startActivity(new Intent(getActivity(), LoginActivity.class));
            });
        }

        return view;
    }

    private void cargarMascotas(String uid) {
        // Usamos nuestro código ultra-limpio del repositorio
        mascotaRepository.escucharMascotas(uid, new MascotaRepository.LeerMascotasCallback() {
            @Override
            public void onResultado(List<Mascota> mascotasDescargadas) {
                listaMascotas.clear();
                listaMascotas.addAll(mascotasDescargadas);
                mascotaAdapter.notifyDataSetChanged();

                // LÓGICA DEL ESTADO VACÍO
                if (listaMascotas.isEmpty()) {
                    // Si no tiene mascotas: Escondemos la lista y mostramos el dibujo del transportín
                    recyclerViewMascotas.setVisibility(View.GONE);
                    layoutEstadoVacio.setVisibility(View.VISIBLE);
                } else {
                    // Si ya tiene mascotas: Mostramos la lista y escondemos el dibujo
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

    // Método automático que se ejecuta al salir de esta pestaña
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Le avisamos al repositorio de que apague el oído ("Matamos al Zombie")
        if (mascotaRepository != null) {
            mascotaRepository.detenerEscucha();
        }
    }
}