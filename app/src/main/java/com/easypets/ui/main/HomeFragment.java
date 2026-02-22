package com.easypets.ui.main;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.easypets.R;
import com.easypets.ui.auth.LoginActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeFragment extends Fragment {

    // Autenticación
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    // Vistas del XML
    private CardView cardMisMascotas, cardVeterinarios, cardCalendario, cardEducacion, cardTiendas, cardEventoDestacado;
    private TextView tvEventoDia, tvEventoMes, tvEventoTitulo, tvEventoHora, tvConsejoDia;
    private EditText etNotaRapida;
    private MaterialButton btnSeleccionarFecha, btnGuardarNota;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Vincular las vistas
        vincularVistas(view);

        // Configurar la lógica dependiendo de si es invitado o usuario
        if (currentUser == null) {
            configurarModoInvitado();
        } else {
            configurarModoUsuario();
        }

        // Configurar los botones generales (Explorar)
        configurarBotonesExplorar();

        return view;
    }

    private void vincularVistas(View view) {
        cardMisMascotas = view.findViewById(R.id.cardMisMascotas);
        cardVeterinarios = view.findViewById(R.id.cardVeterinarios);
        cardCalendario = view.findViewById(R.id.cardCalendario);
        cardEducacion = view.findViewById(R.id.cardEducacion);
        cardTiendas = view.findViewById(R.id.cardTiendas);
        cardEventoDestacado = view.findViewById(R.id.cardEventoDestacado);

        tvEventoDia = view.findViewById(R.id.tvEventoDia);
        tvEventoMes = view.findViewById(R.id.tvEventoMes);
        tvEventoTitulo = view.findViewById(R.id.tvEventoTitulo);
        tvEventoHora = view.findViewById(R.id.tvEventoHora);
        tvConsejoDia = view.findViewById(R.id.tvConsejoDia);

        etNotaRapida = view.findViewById(R.id.etNotaRapida);
        btnSeleccionarFecha = view.findViewById(R.id.btnSeleccionarFecha);
        btnGuardarNota = view.findViewById(R.id.btnGuardarNota);
    }

    // --- LÓGICA MODO INVITADO ---
    private void configurarModoInvitado() {
        // 1. Tarjeta Eventos
        tvEventoDia.setText("-");
        tvEventoMes.setText("");
        tvEventoTitulo.setText("Inicia sesión para ver eventos");
        tvEventoHora.setText("Solo para usuarios registrados");

        // 2. Bloquear Nota Rápida
        etNotaRapida.setEnabled(false);
        etNotaRapida.setHint("Regístrate para usar las notas rápidas");
        btnSeleccionarFecha.setEnabled(false);
        btnGuardarNota.setEnabled(false);

        // 3. Bloquear acceso a Mis Mascotas con un aviso
        cardMisMascotas.setOnClickListener(v -> mostrarAvisoRegistro("Gestionar Mascotas"));
        cardCalendario.setOnClickListener(v -> mostrarAvisoRegistro("Calendario Personal"));
    }

    // --- LÓGICA MODO USUARIO ---
    private void configurarModoUsuario() {
        // Aquí en el futuro llamaremos a Firebase para cargar los datos reales.
        // Por ahora dejamos datos de prueba.
        tvEventoTitulo.setText("Cargando tu próximo evento...");

        // Permitir ir a Mis Mascotas (En el futuro navegaremos al Fragmento correcto)
        cardMisMascotas.setOnClickListener(v ->
                Toast.makeText(getContext(), "Yendo a Mis Mascotas...", Toast.LENGTH_SHORT).show()
        );

        // Activar Nota Rápida
        btnGuardarNota.setOnClickListener(v -> {
            String nota = etNotaRapida.getText().toString().trim();
            if (nota.isEmpty()) {
                etNotaRapida.setError("Escribe algo primero");
            } else {
                Toast.makeText(getContext(), "Nota guardada: " + nota, Toast.LENGTH_SHORT).show();
                etNotaRapida.setText(""); // Limpiar tras guardar
            }
        });

        // El botón de fecha lo programaremos después
        btnSeleccionarFecha.setOnClickListener(v ->
                Toast.makeText(getContext(), "Abrir calendario...", Toast.LENGTH_SHORT).show()
        );
    }

    // --- BOTONES PÚBLICOS (Para todos) ---
    private void configurarBotonesExplorar() {
        cardVeterinarios.setOnClickListener(v -> Toast.makeText(getContext(), "Próximamente: Veterinarios", Toast.LENGTH_SHORT).show());
        cardEducacion.setOnClickListener(v -> Toast.makeText(getContext(), "Próximamente: Educación", Toast.LENGTH_SHORT).show());
        cardTiendas.setOnClickListener(v -> Toast.makeText(getContext(), "Próximamente: Tiendas", Toast.LENGTH_SHORT).show());

        // Consejo aleatorio de prueba
        tvConsejoDia.setText("El chocolate y la cebolla son muy tóxicos para perros y gatos. ¡Mantenlos fuera de su alcance!");
    }

    // --- CUADRO DE DIÁLOGO PARA INVITADOS ---
    private void mostrarAvisoRegistro(String funcion) {
        new AlertDialog.Builder(getContext())
                .setTitle("¡Crea una cuenta gratuita!")
                .setMessage("La función '" + funcion + "' requiere que inicies sesión para poder guardar tu información de forma segura en la nube.")
                .setPositiveButton("Registrarme", (dialog, which) -> {
                    // Cerrar sesión actual (anónima) y mandar al Login
                    mAuth.signOut();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    getActivity().finish();
                })
                .setNegativeButton("Más tarde", null)
                .show();
    }
}