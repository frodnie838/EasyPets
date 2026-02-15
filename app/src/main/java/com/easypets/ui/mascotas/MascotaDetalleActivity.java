package com.easypets.ui.mascotas;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.easypets.R;
import com.easypets.models.Mascota;
import com.easypets.repositories.MascotaRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MascotaDetalleActivity extends AppCompatActivity {

    private TextView tvNombre, tvEspecie, tvRaza, tvPeso, tvSexo, tvColor, tvFechaNacimiento;
    private MaterialButton btnEliminar;
    private FloatingActionButton fabEditar;

    private String idMascotaSeleccionada;
    private FirebaseUser user;
    private DatabaseReference mascotaRef;
    private ValueEventListener mascotaListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mascota_detalle);

        idMascotaSeleccionada = getIntent().getStringExtra("idMascota");
        user = FirebaseAuth.getInstance().getCurrentUser();

        // 1. Vincular vistas
        tvNombre = findViewById(R.id.tvDetalleNombre);
        tvEspecie = findViewById(R.id.tvDetalleEspecie);
        tvRaza = findViewById(R.id.tvDetalleRaza);
        tvPeso = findViewById(R.id.tvDetallePeso);
        tvSexo = findViewById(R.id.tvDetalleSexo);
        tvColor = findViewById(R.id.tvDetalleColor);
        tvFechaNacimiento = findViewById(R.id.tvDetalleFechaNacimiento);
        btnEliminar = findViewById(R.id.btnEliminarMascota);
        fabEditar = findViewById(R.id.fabEditarMascota);

        if (user != null && idMascotaSeleccionada != null) {
            iniciarListenerTiempoReal();
        }

        // Botón Editar
        fabEditar.setOnClickListener(v -> {
            Intent intent = new Intent(MascotaDetalleActivity.this, AgregarMascotaActivity.class);
            intent.putExtra("idMascota", idMascotaSeleccionada);
            startActivity(intent);
        });

        // Botón Eliminar
        btnEliminar.setOnClickListener(v -> mostrarDialogoConfirmacion());
    }

    // ✨ LA CLAVE: Escuchar cambios en tiempo real ✨
    private void iniciarListenerTiempoReal() {
        mascotaRef = FirebaseDatabase.getInstance().getReference()
                .child("mascotas")
                .child(user.getUid())
                .child(idMascotaSeleccionada);

        mascotaListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Mascota mascota = snapshot.getValue(Mascota.class);
                if (mascota != null) {
                    // Rellenar/Actualizar la UI automáticamente
                    tvNombre.setText(mascota.getNombre());
                    tvEspecie.setText(mascota.getEspecie() != null ? mascota.getEspecie() : "Desconocida");
                    tvRaza.setText(mascota.getRaza() != null ? mascota.getRaza() : "Desconocida");
                    tvSexo.setText(mascota.getSexo() != null ? mascota.getSexo() : "Desconocido");
                    tvColor.setText(mascota.getColor() != null ? mascota.getColor() : "Desconocido");
                    tvFechaNacimiento.setText(mascota.getFechaNacimiento() != null ? mascota.getFechaNacimiento() : "Desconocida");
                    tvPeso.setText(mascota.getPeso() + " kg");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(MascotaDetalleActivity.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
            }
        };

        mascotaRef.addValueEventListener(mascotaListener);
    }

    // Importante: Detener el listener cuando cerramos la pantalla para no gastar batería/datos
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mascotaRef != null && mascotaListener != null) {
            mascotaRef.removeEventListener(mascotaListener);
        }
    }

    private void mostrarDialogoConfirmacion() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Mascota")
                .setMessage("¿Estás seguro de eliminar esta mascota?")
                .setPositiveButton("Eliminar", (dialog, which) -> procederAEliminar())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void procederAEliminar() {
        new MascotaRepository().eliminarMascota(user.getUid(), idMascotaSeleccionada, new MascotaRepository.AccionCallback() {
            @Override
            public void onExito() {
                finish();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MascotaDetalleActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}