package com.easypets;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
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
    private GoogleSignInClient mGoogleSignInClient;

    public PerfilFragment() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 1. Inflar el diseño
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        // 2. Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Configurar Google (Para poder cerrar sesión)
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        // 3. Vincular Vistas
        tvNombre = view.findViewById(R.id.tvNombrePerfil);
        tvCorreo = view.findViewById(R.id.tvCorreoPerfil);
        btnLogout = view.findViewById(R.id.btnLogout);

        // 4. Cargar datos del usuario
        cargarDatosUsuario();

        // 5. Configurar Botón Salir
        btnLogout.setOnClickListener(v -> cerrarSesion());

        return view;
    }

    private void cargarDatosUsuario() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Ponemos el correo directamente del Auth (es más rápido)
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
        }else {
            // Lógica invitado:
            tvNombre.setText("Modo Invitado");
            tvCorreo.setText("Sin registrar");

            // Cambiamos el texto y función del botón
            btnLogout.setText("Iniciar Sesión");
            btnLogout.setBackgroundTintList(null); // O el color que quieras
            btnLogout.setOnClickListener(v -> {
                startActivity(new Intent(getActivity(), LoginActivity.class));
            });
        }
    }

    private void cerrarSesion() {
        // Cerrar Firebase
        mAuth.signOut();

        // Cerrar Google y volver al Login
        mGoogleSignInClient.signOut().addOnCompleteListener(requireActivity(), task -> {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            // requireActivity().finish(); // Opcional
        });
    }
}