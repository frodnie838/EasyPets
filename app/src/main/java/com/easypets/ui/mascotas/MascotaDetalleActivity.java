package com.easypets.ui.mascotas;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageButton;
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

    private TextView tvNombre, tvEspecie, tvRaza, tvPeso, tvSexo, tvColor, tvFechaNacimiento, tvMicrochip, tvEsterilizado, tvAlergias, tvPatologias, tvMedicacion;
    private MaterialButton btnEliminar, btnEditar, btnEditarCartilla;
    private ImageView ivDetalleFoto;

    private String idMascotaSeleccionada;
    private FirebaseUser user;
    private DatabaseReference mascotaRef;
    private ValueEventListener mascotaListener;
    private android.widget.LinearLayout layoutSinDatosMedicos, layoutConDatosMedicos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mascota_detalle);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        ImageButton btnTopBack = findViewById(R.id.btnTopBack);
        btnTopBack.setOnClickListener(v -> finish());

        idMascotaSeleccionada = getIntent().getStringExtra("idMascota");
        user = FirebaseAuth.getInstance().getCurrentUser();

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
        tvMicrochip = findViewById(R.id.tvDetalleMicrochip);
        tvEsterilizado = findViewById(R.id.tvDetalleEsterilizado);
        tvAlergias = findViewById(R.id.tvDetalleAlergias);
        tvPatologias = findViewById(R.id.tvDetallePatologias);
        tvMedicacion = findViewById(R.id.tvDetalleMedicacion);
        layoutSinDatosMedicos = findViewById(R.id.layoutSinDatosMedicos);
        layoutConDatosMedicos = findViewById(R.id.layoutConDatosMedicos);
        btnEditarCartilla = findViewById(R.id.btnEditarCartilla);

        // Click listener para la foto de detalle
        ivDetalleFoto.setOnClickListener(v -> {
            if (ivDetalleFoto.getDrawable() != null) {
                mostrarFotoGrande();
            }
        });

        if (user != null && idMascotaSeleccionada != null) {
            iniciarListenerTiempoReal();
        }

        // Click para editar los datos básicos
        btnEditar.setOnClickListener(v -> {
            Intent intent = new Intent(MascotaDetalleActivity.this, AgregarMascotaActivity.class);
            intent.putExtra("idMascota", idMascotaSeleccionada);
            startActivity(intent);
        });

        // ✨ Click para abrir la nueva pantalla de Cartilla Sanitaria
        btnEditarCartilla.setOnClickListener(v -> {
            Intent intent = new Intent(MascotaDetalleActivity.this, EditarCartillaActivity.class);
            intent.putExtra("idMascota", idMascotaSeleccionada);
            startActivity(intent);
        });

        btnEliminar.setOnClickListener(v -> mostrarDialogoConfirmacion());
    }

    private void mostrarFotoGrande() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_ver_foto);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ImageView ivGrande = dialog.findViewById(R.id.ivFotoGrande);
        ImageButton btnCerrar = dialog.findViewById(R.id.btnCerrarFoto);

        ivGrande.setImageDrawable(ivDetalleFoto.getDrawable());

        btnCerrar.setOnClickListener(v -> dialog.dismiss());
        ivGrande.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
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

                    tvMicrochip.setText(mascota.getMicrochip() != null && !mascota.getMicrochip().isEmpty() ? mascota.getMicrochip() : "No registrado");
                    tvEsterilizado.setText(mascota.isEsterilizado() ? "Sí" : "No");
                    tvAlergias.setText(mascota.getAlergias() != null && !mascota.getAlergias().isEmpty() ? mascota.getAlergias() : "Ninguna");
                    tvPatologias.setText(mascota.getPatologias() != null && !mascota.getPatologias().isEmpty() ? mascota.getPatologias() : "Ninguna");
                    tvMedicacion.setText(mascota.getMedicacionActual() != null && !mascota.getMedicacionActual().isEmpty() ? mascota.getMedicacionActual() : "Ninguna");

                    // ✨ LA MAGIA QUE DECIDE QUÉ CAJA MOSTRAR
                    boolean tieneDatos = (mascota.getMicrochip() != null && !mascota.getMicrochip().isEmpty()) ||
                            mascota.isEsterilizado() ||
                            (mascota.getAlergias() != null && !mascota.getAlergias().isEmpty()) ||
                            (mascota.getPatologias() != null && !mascota.getPatologias().isEmpty()) ||
                            (mascota.getMedicacionActual() != null && !mascota.getMedicacionActual().isEmpty()
                    );

                    if (tieneDatos) {
                        // MODO EDICIÓN (Gris)
                        layoutSinDatosMedicos.setVisibility(View.GONE);
                        layoutConDatosMedicos.setVisibility(View.VISIBLE);
                        btnEditarCartilla.setText("Editar");

                        int colorGrisTexto = Color.parseColor("#555555");
                        int colorGrisBorde = Color.parseColor("#DDDDDD");

                        // Aplicamos todo como ColorStateList para evitar el crash
                        btnEditarCartilla.setTextColor(android.content.res.ColorStateList.valueOf(colorGrisTexto));
                        btnEditarCartilla.setStrokeColor(android.content.res.ColorStateList.valueOf(colorGrisBorde));
                        btnEditarCartilla.setIconTint(android.content.res.ColorStateList.valueOf(colorGrisTexto));

                    } else {
                        // MODO AÑADIR (Color Acento)
                        layoutSinDatosMedicos.setVisibility(View.VISIBLE);
                        layoutConDatosMedicos.setVisibility(View.GONE);
                        btnEditarCartilla.setText("Añadir datos médicos");

                        // IMPORTANTE: Resetear al color de la app para que no se quede gris
                        int colorAcento = ContextCompat.getColor(MascotaDetalleActivity.this, R.color.color_acento_primario);
                        android.content.res.ColorStateList cslAcento = android.content.res.ColorStateList.valueOf(colorAcento);

                        btnEditarCartilla.setTextColor(cslAcento);
                        btnEditarCartilla.setStrokeColor(cslAcento);
                        btnEditarCartilla.setIconTint(cslAcento);
                    }

                    if (mascota.getFotoPerfilUrl() != null && !mascota.getFotoPerfilUrl().isEmpty()) {
                        try {
                            byte[] decodedString = Base64.decode(mascota.getFotoPerfilUrl(), Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            ivDetalleFoto.setImageBitmap(decodedByte);
                            ivDetalleFoto.setPadding(0, 0, 0, 0);
                            ivDetalleFoto.setImageTintList(null);
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