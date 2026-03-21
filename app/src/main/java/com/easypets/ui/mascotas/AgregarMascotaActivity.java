package com.easypets.ui.mascotas;

import android.app.DatePickerDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.easypets.R;
import com.easypets.models.Mascota;
import com.easypets.repositories.MascotaRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Calendar;

public class AgregarMascotaActivity extends AppCompatActivity {

    private EditText etNombre, etRaza, etPeso, etColor, etFechaNacimiento;
    private AutoCompleteTextView dropdownEspecie;
    private RadioGroup rgSexo;
    private RadioButton rbMacho, rbHembra;
    private Button btnGuardar, btnCancelar;
    private TextView tvTitulo;
    private ImageView ivFotoMascota;

    private MascotaRepository mascotaRepository;
    private String idMascotaAEditar = null;

    private String fotoBase64 = "";
    private Uri uriImagenSeleccionada = null; // ✨ NUEVO: Variable para Firebase Storage

    private final ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest> photoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    uriImagenSeleccionada = uri;
                    com.bumptech.glide.Glide.with(this).load(uri).into(ivFotoMascota);
                    ivFotoMascota.setPadding(0, 0, 0, 0);
                    ivFotoMascota.setImageTintList(null);
                    fotoBase64 = ""; // Limpiamos el texto antiguo si eligen foto nueva
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_mascota);

        mascotaRepository = new MascotaRepository();

        tvTitulo = findViewById(R.id.tvTituloAgregarMascota);
        etNombre = findViewById(R.id.etNombreMascota);
        dropdownEspecie = findViewById(R.id.dropdownEspecie);
        etRaza = findViewById(R.id.etRazaMascota);
        rgSexo = findViewById(R.id.rgSexo);
        rbMacho = findViewById(R.id.rbMacho);
        rbHembra = findViewById(R.id.rbHembra);
        etFechaNacimiento = findViewById(R.id.etFechaNacimiento);
        etColor = findViewById(R.id.etColorMascota);
        etPeso = findViewById(R.id.etPesoMascota);
        btnGuardar = findViewById(R.id.btnGuardarMascota);
        btnCancelar = findViewById(R.id.btnCancelarMascota);
        ivFotoMascota = findViewById(R.id.ivFotoMascota);

        String[] especies = getResources().getStringArray(R.array.lista_especies);
        ArrayAdapter<String> adapterEspecies = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, especies);
        dropdownEspecie.setAdapter(adapterEspecies);

        etFechaNacimiento.setOnClickListener(v -> mostrarCalendario());

        btnCancelar.setOnClickListener(v -> finish());
        btnGuardar.setOnClickListener(v -> guardarOActualizarMascota());

        findViewById(R.id.cardFotoMascota).setOnClickListener(v -> {
            photoPickerLauncher.launch(new androidx.activity.result.PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        idMascotaAEditar = getIntent().getStringExtra("idMascota");
        if (idMascotaAEditar != null) {
            prepararModoEdicion();
        }
    }

    private void prepararModoEdicion() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        if (tvTitulo != null) tvTitulo.setText("Editar Mascota");
        btnGuardar.setText("Actualizar Mascota");

        mascotaRepository.obtenerMascotaPorId(user.getUid(), idMascotaAEditar, new MascotaRepository.LeerUnaMascotaCallback() {
            @Override
            public void onResultado(Mascota mascota) {
                if (mascota.getNombre() != null) etNombre.setText(mascota.getNombre());
                if (mascota.getEspecie() != null) dropdownEspecie.setText(mascota.getEspecie(), false);
                etRaza.setText(mascota.getRaza() != null && !mascota.getRaza().equals("Desconocida") ? mascota.getRaza() : "");
                etColor.setText(mascota.getColor() != null && !mascota.getColor().equals("Desconocido") ? mascota.getColor() : "");
                etPeso.setText(mascota.getPeso() != null && !mascota.getPeso().equals("0") ? mascota.getPeso() : "");
                etFechaNacimiento.setText(mascota.getFechaNacimiento() != null && !mascota.getFechaNacimiento().equals("Desconocida") ? mascota.getFechaNacimiento() : "");

                if (mascota.getSexo() != null) {
                    if (mascota.getSexo().equals("Macho")) rbMacho.setChecked(true);
                    else if (mascota.getSexo().equals("Hembra")) rbHembra.setChecked(true);
                }

                if (mascota.getFotoPerfilUrl() != null && !mascota.getFotoPerfilUrl().isEmpty()) {
                    fotoBase64 = mascota.getFotoPerfilUrl();
                    if (fotoBase64.startsWith("http")) {
                        com.bumptech.glide.Glide.with(AgregarMascotaActivity.this).load(fotoBase64).into(ivFotoMascota);
                    } else {
                        try {
                            byte[] decodedString = Base64.decode(fotoBase64, Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            ivFotoMascota.setImageBitmap(decodedByte);
                        } catch (Exception e) {}
                    }
                    ivFotoMascota.setPadding(0, 0, 0, 0);
                    ivFotoMascota.setImageTintList(null);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(AgregarMascotaActivity.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarCalendario() {
        Calendar calendario = Calendar.getInstance();
        int anio = calendario.get(Calendar.YEAR);
        int mes = calendario.get(Calendar.MONTH);
        int dia = calendario.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String fechaSeleccionada = dayOfMonth + "/" + (month + 1) + "/" + year;
                    etFechaNacimiento.setText(fechaSeleccionada);
                }, anio, mes, dia);
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void guardarOActualizarMascota() {
        String nombre = etNombre.getText().toString().trim();
        String especie = dropdownEspecie.getText().toString().trim();
        String raza = etRaza.getText().toString().trim();
        String color = etColor.getText().toString().trim();
        String pesoStr = etPeso.getText().toString().trim();
        String fechaNacimiento = etFechaNacimiento.getText().toString().trim();

        // ✨ SOLUCIÓN: Usamos una variable temporal para descubrir el sexo...
        String sexoTemporal = "";
        int selectedId = rgSexo.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton rbSeleccionado = findViewById(selectedId);
            sexoTemporal = rbSeleccionado.getText().toString();
        }

        // ✨ ...y luego lo guardamos en una variable "final" para que Java nos deje usarla en la Lambda
        final String sexo = sexoTemporal;

        if (TextUtils.isEmpty(nombre)) { etNombre.setError("Obligatorio"); return; }
        if (TextUtils.isEmpty(especie)) { dropdownEspecie.setError("Obligatorio"); return; }
        if (TextUtils.isEmpty(sexo)) { Toast.makeText(this, "Selecciona el sexo", Toast.LENGTH_SHORT).show(); return; }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        btnGuardar.setEnabled(false);
        btnGuardar.setText("Subiendo datos...");

        if (uriImagenSeleccionada != null) {
            StorageReference ref = FirebaseStorage.getInstance().getReference("mascotas").child(user.getUid() + "_" + System.currentTimeMillis() + ".jpg");
            ref.putFile(uriImagenSeleccionada).addOnSuccessListener(taskSnapshot -> {
                ref.getDownloadUrl().addOnSuccessListener(url -> {
                    fotoBase64 = url.toString();
                    ejecutarGuardadoFinal(user.getUid(), nombre, especie, raza, sexo, fechaNacimiento, color, pesoStr);
                });
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show();
                btnGuardar.setEnabled(true);
                btnGuardar.setText("Guardar Mascota");
            });
        } else {
            ejecutarGuardadoFinal(user.getUid(), nombre, especie, raza, sexo, fechaNacimiento, color, pesoStr);
        }
    }

    private void ejecutarGuardadoFinal(String uid, String nombre, String especie, String raza, String sexo, String fechaNacimiento, String color, String pesoStr) {

        // ✨ ORDEN 100% CORREGIDO BASADO EN TU ARCHIVO MASCOTA.JAVA
        Mascota mascotaListaParaSubir = new Mascota(
                idMascotaAEditar,                                            // 1. id
                nombre,                                                      // 2. nombre
                especie,                                                     // 3. especie
                raza.isEmpty() ? "Desconocida" : raza,                       // 4. raza
                sexo,                                                        // 5. sexo
                fechaNacimiento.isEmpty() ? "Desconocida" : fechaNacimiento, // 6. fechaNacimiento
                color.isEmpty() ? "Desconocido" : color,                     // 7. color
                pesoStr.isEmpty() ? "0" : pesoStr,                           // 8. peso
                "",                                                          // 9. microchip
                false,                                                       // 10. esterilizado
                "",                                                          // 11. patologias
                fotoBase64,                                                  // 12. fotoPerfilUrl
                System.currentTimeMillis()                                   // 13. timestamp
        );

        if (idMascotaAEditar == null) {
            mascotaRepository.guardarMascota(uid, mascotaListaParaSubir, new MascotaRepository.AccionCallback() {
                @Override public void onExito() { finish(); }
                @Override public void onError(String error) {
                    Toast.makeText(AgregarMascotaActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    btnGuardar.setEnabled(true);
                }
            });
        } else {
            mascotaRepository.actualizarMascota(uid, mascotaListaParaSubir, new MascotaRepository.AccionCallback() {
                @Override public void onExito() { finish(); }
                @Override public void onError(String error) {
                    Toast.makeText(AgregarMascotaActivity.this, "Error al actualizar: " + error, Toast.LENGTH_LONG).show();
                    btnGuardar.setEnabled(true);
                }
            });
        }
    }
}