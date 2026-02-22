package com.easypets.ui.mascotas;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.easypets.R;
import com.easypets.models.Mascota;
import com.easypets.repositories.MascotaRepository;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MascotaDetalleActivity extends AppCompatActivity {

    private TextView tvNombre, tvEspecie, tvRaza, tvPeso, tvSexo, tvColor, tvFechaNacimiento;
    private MaterialButton btnEliminar, btnEditar;
    private ImageView ivDetalleFoto;

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
        btnEditar = findViewById(R.id.btnEditarMascota);
        ivDetalleFoto = findViewById(R.id.ivDetalleFoto);

        if (user != null && idMascotaSeleccionada != null) {
            iniciarListenerTiempoReal();
        }

        // Botón Editar
        btnEditar.setOnClickListener(v -> {
            Intent intent = new Intent(MascotaDetalleActivity.this, AgregarMascotaActivity.class);
            intent.putExtra("idMascota", idMascotaSeleccionada);
            startActivity(intent);
        });

        // Botón Eliminar
        btnEliminar.setOnClickListener(v -> mostrarDialogoConfirmacion());
    }

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
                    tvNombre.setText(mascota.getNombre());
                    tvEspecie.setText(mascota.getEspecie() != null ? mascota.getEspecie() : "Desconocida");
                    tvRaza.setText(mascota.getRaza() != null ? mascota.getRaza() : "Desconocida");
                    tvSexo.setText(mascota.getSexo() != null ? mascota.getSexo() : "Desconocido");
                    tvColor.setText(mascota.getColor() != null ? mascota.getColor() : "Desconocido");
                    tvFechaNacimiento.setText(mascota.getFechaNacimiento() != null ? mascota.getFechaNacimiento() : "Desconocida");
                    tvPeso.setText(mascota.getPeso() + " kg");

                    // ✨ LÓGICA DE LA FOTO CORREGIDA PARA EL DETALLE ✨
                    if (mascota.getFotoPerfilUrl() != null && !mascota.getFotoPerfilUrl().isEmpty()) {
                        try {
                            byte[] decodedString = Base64.decode(mascota.getFotoPerfilUrl(), Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            ivDetalleFoto.setImageBitmap(decodedByte);

                            // Ajustes para foto real
                            ivDetalleFoto.setPadding(0, 0, 0, 0);
                            ivDetalleFoto.setImageTintList(null); // Quitar filtro verde
                            ivDetalleFoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        } catch (Exception e) {
                            ponerHuellaPorDefectoDetalle();
                        }
                    } else {
                        ponerHuellaPorDefectoDetalle();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(MascotaDetalleActivity.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
            }
        };

        mascotaRef.addValueEventListener(mascotaListener);
    }

    // ✨ MÉTODO AUXILIAR PARA LA HUELLA ✨
    private void ponerHuellaPorDefectoDetalle() {
        ivDetalleFoto.setImageResource(R.drawable.huella);
        ivDetalleFoto.setPadding(50, 50, 50, 50);
        ivDetalleFoto.setImageTintList(ContextCompat.getColorStateList(this, R.color.color_acento_primario));
        ivDetalleFoto.setScaleType(ImageView.ScaleType.FIT_CENTER);
    }

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