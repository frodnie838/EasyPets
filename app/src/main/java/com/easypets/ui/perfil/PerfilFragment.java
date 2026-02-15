package com.easypets.ui.perfil;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.credentials.CredentialManager;
import androidx.fragment.app.Fragment;

import com.easypets.R;
import com.easypets.ui.auth.LoginActivity;
import com.easypets.ui.main.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class PerfilFragment extends Fragment {

    private TextView tvNombre, tvCorreo;
    private Button btnLogout;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private CredentialManager credentialManager;

    public PerfilFragment() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 1. Inflar el diseño
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        // 2. Inicializar Firebase y Google
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        credentialManager = CredentialManager.create(requireContext());

        // 3. Vincular Vistas
        tvNombre = view.findViewById(R.id.tvNombrePerfil);
        tvCorreo = view.findViewById(R.id.tvCorreoPerfil);
        btnLogout = view.findViewById(R.id.btnLogout);

        // 4. Cargar datos del usuario y configurar el botón DEPENDIENDO de quién sea
        cargarDatosUsuario();

        // (¡ELIMINADO EL PASO 5 QUE SOBREESCRIBÍA EL BOTÓN!)

        return view;
    }

    private void cargarDatosUsuario() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // --- LÓGICA USUARIO LOGUEADO ---
            tvCorreo.setText(user.getEmail());

            // Leemos el nombre y apellidos de la Base de Datos
            mDatabase.child("users").child(user.getUid()).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    DataSnapshot data = task.getResult();
                    String nombre = data.child("nombre").getValue(String.class);
                    String apellidos = data.child("apellidos").getValue(String.class);

                    tvNombre.setText(nombre + " " + apellidos);
                } else {
                    tvNombre.setText("Usuario");
                }
            });

            // Configuramos su botón específicamente para SALIR
            btnLogout.setText("Cerrar Sesión");
            btnLogout.setOnClickListener(v -> cerrarSesion());

        } else {
            // --- LÓGICA INVITADO ---
            tvNombre.setText("Modo Invitado");
            tvCorreo.setText("Sin registrar");

            // Configuramos su botón específicamente para ENTRAR
            btnLogout.setText("Iniciar Sesión");
            btnLogout.setBackgroundTintList(null);
            btnLogout.setOnClickListener(v -> {
                startActivity(new Intent(getActivity(), LoginActivity.class));
            });
        }
    }

    private void cerrarSesion() {
        // 1. Cerrar Firebase
        mAuth.signOut();

        // 2. Limpiar las credenciales en el teléfono (El nuevo método)
        credentialManager.clearCredentialStateAsync(
                new androidx.credentials.ClearCredentialStateRequest(),
                new android.os.CancellationSignal(),
                androidx.core.content.ContextCompat.getMainExecutor(requireContext()),
                new androidx.credentials.CredentialManagerCallback<Void, androidx.credentials.exceptions.ClearCredentialException>() {
                    @Override
                    public void onResult(Void result) {
                        irAlLogin();
                    }

                    @Override
                    public void onError(androidx.credentials.exceptions.ClearCredentialException e) {
                        volverAlInicio();
                    }
                }
        );
    }

    private void volverAlInicio() {
        Intent intent = new Intent(requireActivity(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void irAlLogin() {
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}