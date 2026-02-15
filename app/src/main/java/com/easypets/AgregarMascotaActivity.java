package com.easypets;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class AgregarMascotaActivity extends AppCompatActivity {

    private EditText etNombre, etEspecie, etRaza, etPeso;
    private Button btnGuardar;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ¡OJO! Si tienes enableEdgeToEdge(); aquí, bórralo
        setContentView(R.layout.activity_agregar_mascota);

        // 1. Inicializar Firebase Database
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // 2. Vincular las vistas
        etNombre = findViewById(R.id.etNombreMascota);
        etEspecie = findViewById(R.id.etEspecie);
        etRaza = findViewById(R.id.etRaza);
        etPeso = findViewById(R.id.etPeso);
        btnGuardar = findViewById(R.id.btnGuardarMascota);

        // 3. Configurar el botón
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
        if (user == null) {
            Toast.makeText(this, "Error: No estás logueado", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();

        // Generar un ID ÚNICO aleatorio para esta mascota
        String idMascota = mDatabase.child("mascotas").child(uid).push().getKey();

        // Crear el "Paquete" de datos a guardar
        Map<String, Object> mascotaMap = new HashMap<>();
        mascotaMap.put("idMascota", idMascota);
        mascotaMap.put("nombre", nombre);
        mascotaMap.put("especie", especie);
        mascotaMap.put("raza", raza.isEmpty() ? "Desconocida" : raza); // Si está vacío pone Desconocida
        mascotaMap.put("peso", pesoStr.isEmpty() ? "0" : pesoStr);
        mascotaMap.put("timestamp", System.currentTimeMillis());

        // Guardar en la base de datos (mascotas -> UID -> idMascota)
        if (idMascota != null) {
            mDatabase.child("mascotas").child(uid).child(idMascota).setValue(mascotaMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "¡Mascota guardada con éxito!", Toast.LENGTH_SHORT).show();
                        finish(); // Cierra esta pantalla y vuelve a la anterior
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }
}