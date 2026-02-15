package com.easypets.ui.mascotas;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.easypets.R;
import com.easypets.models.Mascota;
import com.easypets.repositories.MascotaRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class AgregarMascotaActivity extends AppCompatActivity {

    private EditText etNombre, etEspecie, etRaza, etPeso;
    private Button btnGuardar;
    private MascotaRepository mascotaRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_mascota);

        mascotaRepository = new MascotaRepository();

        // Vincular las vistas
        etNombre = findViewById(R.id.etNombreMascota);
        etEspecie = findViewById(R.id.etEspecie);
        etRaza = findViewById(R.id.etRaza);
        etPeso = findViewById(R.id.etPeso);
        btnGuardar = findViewById(R.id.btnGuardarMascota);

        // Configurar el botón
        btnGuardar.setOnClickListener(v -> guardarMascota());
    }

    private void guardarMascota() {
        // Leer lo que escribió el usuario
        String nombre = etNombre.getText().toString().trim();
        String especie = etEspecie.getText().toString().trim();
        String raza = etRaza.getText().toString().trim();
        String pesoStr = etPeso.getText().toString().trim();

        // Validación básica (Obligamos a poner nombre y especie)
        if (TextUtils.isEmpty(nombre)) {
            etNombre.setError("El nombre es obligatorio");
            return;
        }
        if (TextUtils.isEmpty(especie)) {
            etEspecie.setError("La especie es obligatoria");
            return;
        }

        // Comprobar que hay usuario logueado
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // 1. Creamos el objeto Mascota con todos sus datos empaquetados
        Mascota nuevaMascota = new Mascota(
                null, // El ID se lo pondrá el repositorio
                nombre,
                especie,
                raza.isEmpty() ? "Desconocida" : raza,
                pesoStr.isEmpty() ? "0" : pesoStr,
                System.currentTimeMillis()
        );

        // Le decimos al Repositorio: "¡Toma la mascota y guárdala tú!"
        mascotaRepository.guardarMascota(user.getUid(), nuevaMascota, new MascotaRepository.AccionCallback() {
            @Override
            public void onExito() {
                Toast.makeText(AgregarMascotaActivity.this, "¡Guardada!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(AgregarMascotaActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}