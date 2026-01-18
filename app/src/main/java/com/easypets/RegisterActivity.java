package com.easypets;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class RegisterActivity extends AppCompatActivity {

    // Variables de la interfaz
    private EditText nombreEditText, apellido1EditText, apellido2EditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private CheckBox termsCheckBox;
    private Button btnRegistrar;
    private TextView loginTextView;

    // Variables de Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();

        nombreEditText = findViewById(R.id.nombreEditText);
        apellido1EditText = findViewById(R.id.apellido1EditText);
        apellido2EditText = findViewById(R.id.apellido2EditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.passwordConfirmEditText);
        termsCheckBox = findViewById(R.id.termsCheckBox);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        loginTextView = findViewById(R.id.loginTextView);

        btnRegistrar.setEnabled(false);
        btnRegistrar.setAlpha(0.5f);

        termsCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                btnRegistrar.setEnabled(true);
                btnRegistrar.setAlpha(1.0f);
            } else {
                btnRegistrar.setEnabled(false);
                btnRegistrar.setAlpha(0.5f);
            }
        });

        btnRegistrar.setOnClickListener(v -> registrarUsuario());

        loginTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(i);
            }
        });

    }

    private void registrarUsuario() {
        String nombre = nombreEditText.getText().toString().trim();
        String apellido1 = apellido1EditText.getText().toString().trim();
        String apellido2 = apellido2EditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        // VALIDACIONES
        if (TextUtils.isEmpty(nombre)) {
            nombreEditText.setError("El nombre es obligatorio");
            return;
        }
        if (TextUtils.isEmpty(apellido1)) {
            nombreEditText.setError("El primer apellido es obligatorio");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("El correo es obligatorio");
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            passwordEditText.setError("La contraseña debe tener al menos 6 caracteres");
            return;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Las contraseñas no coinciden");
            return;
        }

        // Si pasa las validaciones, crea el usuario en Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {// Si el usuario se creó, se guarda sus datos en Firestore
                        FirebaseUser user = mAuth.getCurrentUser();
                        guardarDatosFirestore(user.getUid(), nombre, apellido1, apellido2, email);
                    } else {
                        Toast.makeText(RegisterActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void guardarDatosFirestore(String uid, String nombre, String apellido1, String apellido2, String email) {
        Date date = new Date(); //Fecha actual
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()); //Formato dia/mes/año hora:minutos
        sdf.setTimeZone(TimeZone.getTimeZone("Europe/Madrid")); //Guarda la hora española y no la del movil local/emulador
        String fecha = sdf.format(date); //Convierte el date en string con el formato dia/mes/año hora:minutos

        Map<String, Object> usuario = new HashMap<>();
        usuario.put("idUsuario", uid);
        usuario.put("nombre", nombre);
        usuario.put("apellido1", apellido1);
        usuario.put("apellido2", apellido2);
        usuario.put("correo", email);
        usuario.put("fechaRegistro", fecha);
        usuario.put("timestamp", System.currentTimeMillis());

        // Guarda en la colección "users"
        db.child("users").child(uid).setValue(usuario)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "¡Cuenta creada!", Toast.LENGTH_SHORT).show();
                    // Redirigir a LoginActivity y borra historial para que no vuelva atrás
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Error al guardar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}