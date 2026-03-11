package com.easypets.ui.mascotas;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.easypets.R;
import com.easypets.models.Mascota;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class EditarCartillaActivity extends AppCompatActivity {

    private EditText etMicrochip, etAlergias, etPatologias, etMedicacion;
    private CheckBox cbEsterilizado;
    private MaterialButton btnGuardar, btnCancelar;
    private String idMascota;
    private DatabaseReference mascotaRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_cartilla);

        // Barra blanca del sistema
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        // Vistas
        etMicrochip = findViewById(R.id.etCartillaMicrochip);
        cbEsterilizado = findViewById(R.id.cbCartillaEsterilizado);
        etAlergias = findViewById(R.id.etCartillaAlergias);
        etPatologias = findViewById(R.id.etCartillaPatologias);
        etMedicacion = findViewById(R.id.etCartillaMedicacion);
        btnGuardar = findViewById(R.id.btnGuardarCartilla);
        btnCancelar = findViewById(R.id.btnCancelarCartilla);

        btnCancelar.setOnClickListener(v -> finish());

        // Obtener la mascota de Firebase
        idMascota = getIntent().getStringExtra("idMascota");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && idMascota != null) {
            mascotaRef = FirebaseDatabase.getInstance().getReference()
                    .child("mascotas").child(user.getUid()).child(idMascota);

            cargarDatosCartilla();
        }

        btnGuardar.setOnClickListener(v -> guardarCartilla());
    }

    private void cargarDatosCartilla() {
        mascotaRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Mascota mascota = snapshot.getValue(Mascota.class);
                if (mascota != null) {
                    if (mascota.getMicrochip() != null) etMicrochip.setText(mascota.getMicrochip());
                    cbEsterilizado.setChecked(mascota.isEsterilizado());
                    if (mascota.getAlergias() != null) etAlergias.setText(mascota.getAlergias());
                    if (mascota.getPatologias() != null) etPatologias.setText(mascota.getPatologias());
                    if (mascota.getMedicacionActual() != null) etMedicacion.setText(mascota.getMedicacionActual());
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(EditarCartillaActivity.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void guardarCartilla() {
        btnGuardar.setEnabled(false);
        btnGuardar.setText("Guardando...");

        // Usamos un Map para actualizar SOLO los datos médicos sin borrar el nombre, foto, etc.
        Map<String, Object> actualizacionesMedicas = new HashMap<>();
        actualizacionesMedicas.put("microchip", etMicrochip.getText().toString().trim());
        actualizacionesMedicas.put("esterilizado", cbEsterilizado.isChecked());
        actualizacionesMedicas.put("alergias", etAlergias.getText().toString().trim());
        actualizacionesMedicas.put("patologias", etPatologias.getText().toString().trim());
        actualizacionesMedicas.put("medicacionActual", etMedicacion.getText().toString().trim());

        mascotaRef.updateChildren(actualizacionesMedicas)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cartilla actualizada", Toast.LENGTH_SHORT).show();
                    finish(); // Vuelve al detalle
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show();
                    btnGuardar.setEnabled(true);
                    btnGuardar.setText("Guardar Cartilla");
                });
    }
}