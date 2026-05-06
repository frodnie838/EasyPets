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

/**
 * Fragmento principal encargado de mostrar y gestionar la lista de mascotas del usuario.
 * Presenta diferentes estados de interfaz dependiendo de si el usuario está autenticado,
 * es un invitado, o si aún no tiene mascotas registradas en la plataforma (Empty State).
 */
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
            layoutLogueado.setVisibility(View.GONE);
            layoutInvitado.setVisibility(View.VISIBLE);

            btnIrALogin.setOnClickListener(v -> startActivity(new Intent(getActivity(), LoginActivity.class)));
        }

        return view;
    }

    /**
     * Inicia un listener en tiempo real para obtener y mantener sincronizada
     * la lista de mascotas asociadas al usuario actual. Gestiona dinámicamente la visibilidad
     * del RecyclerView frente al estado vacío de la interfaz.
     *
     * @param uid Identificador único del usuario autenticado.
     */
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

    /**
     * Método del ciclo de vida del fragmento.
     * Se encarga de liberar recursos y detener los listeners activos de Firebase Database
     * para evitar fugas de memoria (Memory Leaks) cuando la vista es destruida.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mascotaRepository != null) {
            mascotaRepository.detenerEscucha();
        }
    }
}