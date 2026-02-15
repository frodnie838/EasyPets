package com.easypets.ui.mascotas;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.easypets.R;
import com.easypets.models.Mascota;
import com.easypets.repositories.MascotaRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;

public class AgregarMascotaActivity extends AppCompatActivity {

    private EditText etNombre, etRaza, etPeso, etColor, etFechaNacimiento;
    private AutoCompleteTextView dropdownEspecie;
    private RadioGroup rgSexo;
    private RadioButton rbMacho, rbHembra;
    private Button btnGuardar, btnCancelar;
    private TextView tvTitulo; // Para cambiar de "Nueva" a "Editar"

    private MascotaRepository mascotaRepository;
    private String idMascotaAEditar = null; // ✨ EDICIÓN: Variable para saber en qué modo estamos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_mascota);

        mascotaRepository = new MascotaRepository();

        // 1. Vincular vistas
        tvTitulo = findViewById(R.id.tvTituloAgregarMascota); // <-- NECESITARÁS AÑADIRLE UN ID EN EL XML (Ver nota abajo)
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

        // 2. Configurar el Desplegable de Especies
        String[] especies = getResources().getStringArray(R.array.lista_especies);
        ArrayAdapter<String> adapterEspecies = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, especies);
        dropdownEspecie.setAdapter(adapterEspecies);

        // 3. Configurar el Calendario
        etFechaNacimiento.setOnClickListener(v -> mostrarCalendario());

        // 4. Botones
        btnCancelar.setOnClickListener(v -> finish());
        btnGuardar.setOnClickListener(v -> guardarOActualizarMascota());

        // ✨ EDICIÓN: Comprobamos si venimos desde el botón Editar de la Ficha
        idMascotaAEditar = getIntent().getStringExtra("idMascota");
        if (idMascotaAEditar != null) {
            prepararModoEdicion();
        }
    }

    // ✨ EDICIÓN: Descargamos los datos y rellenamos los campos CON PARACAÍDAS
    private void prepararModoEdicion() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        if (tvTitulo != null) tvTitulo.setText("Editar Mascota");
        btnGuardar.setText("Actualizar Mascota");

        mascotaRepository.obtenerMascotaPorId(user.getUid(), idMascotaAEditar, new MascotaRepository.LeerUnaMascotaCallback() {
            @Override
            public void onResultado(Mascota mascota) {
                // 1. Datos básicos (Comprobamos que no sean nulos)
                if (mascota.getNombre() != null) etNombre.setText(mascota.getNombre());
                if (mascota.getEspecie() != null) dropdownEspecie.setText(mascota.getEspecie(), false);

                // 2. Datos que pueden estar en blanco o como "Desconocido"
                etRaza.setText(mascota.getRaza() != null && !mascota.getRaza().equals("Desconocida") ? mascota.getRaza() : "");
                etColor.setText(mascota.getColor() != null && !mascota.getColor().equals("Desconocido") ? mascota.getColor() : "");
                etPeso.setText(mascota.getPeso() != null && !mascota.getPeso().equals("0") ? mascota.getPeso() : "");
                etFechaNacimiento.setText(mascota.getFechaNacimiento() != null && !mascota.getFechaNacimiento().equals("Desconocida") ? mascota.getFechaNacimiento() : "");

                // 3. ✨ EL CULPABLE DEL CRASHEO ✨: El Sexo
                if (mascota.getSexo() != null) {
                    if (mascota.getSexo().equals("Macho")) {
                        rbMacho.setChecked(true);
                    } else if (mascota.getSexo().equals("Hembra")) {
                        rbHembra.setChecked(true);
                    }
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(AgregarMascotaActivity.this, R.string.error_cargar_datos, Toast.LENGTH_SHORT).show();
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

        Mascota mascotaListaParaSubir = new Mascota(
                idMascotaAEditar, // Si es nueva será null, si es editar mantendrá su ID
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
                "",
                System.currentTimeMillis()
        );

        // ✨ EDICIÓN: Decidimos si creamos o actualizamos
        if (idMascotaAEditar == null) {
            // MODO CREAR NUEVA
            mascotaRepository.guardarMascota(user.getUid(), mascotaListaParaSubir, new MascotaRepository.AccionCallback() {
                @Override
                public void onExito() {
                    Toast.makeText(AgregarMascotaActivity.this, "¡Mascota Guardada!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(AgregarMascotaActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // MODO ACTUALIZAR
            mascotaRepository.actualizarMascota(user.getUid(), mascotaListaParaSubir, new MascotaRepository.AccionCallback() {
                @Override
                public void onExito() {
                    Toast.makeText(AgregarMascotaActivity.this, "¡Mascota Actualizada!", Toast.LENGTH_SHORT).show();
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