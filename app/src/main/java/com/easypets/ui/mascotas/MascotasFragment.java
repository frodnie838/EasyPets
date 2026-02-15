package com.easypets.ui.mascotas; // Fíjate que ahora está en tu nuevo paquete

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.easypets.R;
import com.easypets.adapters.MascotaAdapter;
import com.easypets.models.Mascota;
import com.easypets.ui.auth.LoginActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MascotasFragment extends Fragment {

    private View layoutLogueado;
    private View layoutInvitado;
    private Button btnIrALogin;

    // --- Variables para la lista (NUEVO) ---
    private RecyclerView recyclerViewMascotas;
    private MascotaAdapter mascotaAdapter;
    private List<Mascota> listaMascotas;

    // --- Variables de Firebase ---
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    public MascotasFragment() {
        // Constructor vacío
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mascotas, container, false);

        // 1. Vincular las vistas base
        layoutLogueado = view.findViewById(R.id.layoutUsuarioLogueado);
        layoutInvitado = view.findViewById(R.id.layoutInvitado);
        btnIrALogin = view.findViewById(R.id.btnIrALogin);

        // 2. Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            // --- EL USUARIO ESTÁ LOGUEADO ---
            layoutLogueado.setVisibility(View.VISIBLE);
            layoutInvitado.setVisibility(View.GONE);

            // A) Configurar el botón de Añadir Mascota
            View fabAgregar = view.findViewById(R.id.fabAgregarMascota);
            fabAgregar.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), AgregarMascotaActivity.class);
                startActivity(intent);
            });

            // B) Preparar el RecyclerView (La Lista)
            recyclerViewMascotas = view.findViewById(R.id.recyclerViewMascotas);
            // El LayoutManager le dice que ponga las tarjetas una debajo de otra
            recyclerViewMascotas.setLayoutManager(new LinearLayoutManager(getContext()));

            // C) Crear la lista vacía y conectarla al Adaptador
            listaMascotas = new ArrayList<>();
            mascotaAdapter = new MascotaAdapter(listaMascotas);
            recyclerViewMascotas.setAdapter(mascotaAdapter);

            // D) ¡Ir a buscar los datos a Firebase!
            cargarMascotas(user.getUid());

        } else {
            // --- EL USUARIO ES UN INVITADO ---
            layoutLogueado.setVisibility(View.GONE);
            layoutInvitado.setVisibility(View.VISIBLE);

            btnIrALogin.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
            });
        }

        return view;
    }

    // Método que descarga las mascotas de este usuario específico
    private void cargarMascotas(String uid) {
        // Leemos la ruta: mascotas -> UID del usuario
        mDatabase.child("mascotas").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // 1. Limpiamos la lista actual para que no se dupliquen datos
                listaMascotas.clear();

                // 2. Recorremos todas las mascotas que nos devuelve Firebase
                for (DataSnapshot data : snapshot.getChildren()) {
                    Mascota mascota = data.getValue(Mascota.class); // Magia: convierte el JSON a nuestro objeto Mascota
                    if (mascota != null) {
                        listaMascotas.add(mascota);
                    }
                }

                // 3. Le avisamos al adaptador que los datos han cambiado para que repinte la pantalla
                mascotaAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error al cargar mascotas: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}