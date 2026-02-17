package com.easypets.ui.mascotas;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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

    // ✨ AQUÍ GUARDAMOS LA FOTO CONVERTIDA A TEXTO ✨
    private String fotoBase64 = "";

    // ✨ EL LANZADOR DE LA GALERÍA Y COMPRESOR DE IMAGEN ✨
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    try {
                        // 1. Leer imagen de la galería
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        Bitmap bitmapOriginal = BitmapFactory.decodeStream(inputStream);

                        // 2. Reducir tamaño a 400x400 para no saturar Firebase
                        int maxResolucion = 400;
                        int ancho = bitmapOriginal.getWidth();
                        int alto = bitmapOriginal.getHeight();
                        float ratio = (float) ancho / alto;
                        if (ancho > alto) {
                            ancho = maxResolucion;
                            alto = (int) (ancho / ratio);
                        } else {
                            alto = maxResolucion;
                            ancho = (int) (alto * ratio);
                        }
                        Bitmap bitmapReducido = Bitmap.createScaledBitmap(bitmapOriginal, ancho, alto, true);

                        // 3. Mostrar en la pantalla (Quitando el padding de la huella)
                        ivFotoMascota.setImageBitmap(bitmapReducido);
                        ivFotoMascota.setPadding(0, 0, 0, 0);

                        // 4. Convertir a Texto Base64
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmapReducido.compress(Bitmap.CompressFormat.JPEG, 70, baos); // Calidad 70%
                        byte[] bytesImagen = baos.toByteArray();
                        fotoBase64 = Base64.encodeToString(bytesImagen, Base64.DEFAULT);

                    } catch (Exception e) {
                        Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_mascota);

        mascotaRepository = new MascotaRepository();

        // Vincular vistas
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

        // Desplegable de Especies
        String[] especies = getResources().getStringArray(R.array.lista_especies);
        ArrayAdapter<String> adapterEspecies = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, especies);
        dropdownEspecie.setAdapter(adapterEspecies);

        // Calendario
        etFechaNacimiento.setOnClickListener(v -> mostrarCalendario());

        // Botones
        btnCancelar.setOnClickListener(v -> finish());
        btnGuardar.setOnClickListener(v -> guardarOActualizarMascota());

        // Al tocar la foto, abrir la galería
        findViewById(R.id.cardFotoMascota).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        // Comprobamos si venimos a Editar
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

                // ✨ RECUPERAR FOTO DE LA BASE DE DATOS Y DECODIFICARLA ✨
                if (mascota.getFotoPerfilUrl() != null && !mascota.getFotoPerfilUrl().isEmpty()) {
                    fotoBase64 = mascota.getFotoPerfilUrl(); // La guardamos por si el usuario actualiza sin cambiar de foto
                    try {
                        byte[] decodedString = Base64.decode(fotoBase64, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        ivFotoMascota.setImageBitmap(decodedByte);
                        ivFotoMascota.setPadding(0, 0, 0, 0); // Quitar el padding de la huella
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
        datePickerDialog.show();
    }

    private void guardarOActualizarMascota() {
        String nombre = etNombre.getText().toString().trim();
        String especie = dropdownEspecie.getText().toString().trim();
        String raza = etRaza.getText().toString().trim();
        String color = etColor.getText().toString().trim();
        String pesoStr = etPeso.getText().toString().trim();
        String fechaNacimiento = etFechaNacimiento.getText().toString().trim();

        String sexo = "";
        int selectedId = rgSexo.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton rbSeleccionado = findViewById(selectedId);
            sexo = rbSeleccionado.getText().toString();
        }

        if (TextUtils.isEmpty(nombre)) { etNombre.setError("Obligatorio"); return; }
        if (TextUtils.isEmpty(especie)) { dropdownEspecie.setError("Obligatorio"); return; }
        if (TextUtils.isEmpty(sexo)) { Toast.makeText(this, "Selecciona el sexo", Toast.LENGTH_SHORT).show(); return; }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // ✨ CREAMOS LA MASCOTA CON EL TEXTO DE LA FOTO ✨
        Mascota mascotaListaParaSubir = new Mascota(
                idMascotaAEditar,
                nombre,
                especie,
                raza.isEmpty() ? "Desconocida" : raza,
                sexo,
                fechaNacimiento.isEmpty() ? "Desconocida" : fechaNacimiento,
                color.isEmpty() ? "Desconocido" : color,
                pesoStr.isEmpty() ? "0" : pesoStr,
                "",
                false,
                "",
                fotoBase64, // Pasamos la foto en texto
                System.currentTimeMillis()
        );

        if (idMascotaAEditar == null) {
            mascotaRepository.guardarMascota(user.getUid(), mascotaListaParaSubir, new MascotaRepository.AccionCallback() {
                @Override
                public void onExito() {
                    finish();
                }
                @Override
                public void onError(String error) {
                    Toast.makeText(AgregarMascotaActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            mascotaRepository.actualizarMascota(user.getUid(), mascotaListaParaSubir, new MascotaRepository.AccionCallback() {
                @Override
                public void onExito() {
                    finish();
                }
                @Override
                public void onError(String error) {
                    Toast.makeText(AgregarMascotaActivity.this, "Error al actualizar: " + error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}