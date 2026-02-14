package com.easypets;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MascotasFragment extends Fragment {

    private View layoutLogueado;
    private View layoutInvitado;
    private Button btnIrALogin;

    public MascotasFragment() {
        // Constructor vacío
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mascotas, container, false);

        // 1. Vincular vistas
        layoutLogueado = view.findViewById(R.id.layoutUsuarioLogueado);
        layoutInvitado = view.findViewById(R.id.layoutInvitado);
        btnIrALogin = view.findViewById(R.id.btnIrALogin);

        // 2. Comprobar si hay usuario
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            // --- ESTÁ LOGUEADO ---
            // Mostramos contenido, ocultamos aviso
            layoutLogueado.setVisibility(View.VISIBLE);
            layoutInvitado.setVisibility(View.GONE);

            // Aquí llamarías a cargarMascotas() más adelante...
        } else {
            // --- ES UN INVITADO ---
            // Ocultamos contenido, mostramos aviso
            layoutLogueado.setVisibility(View.GONE);
            layoutInvitado.setVisibility(View.VISIBLE);

            // Configuramos el botón para ir al Login
            btnIrALogin.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
            });
        }

        return view;
    }
}