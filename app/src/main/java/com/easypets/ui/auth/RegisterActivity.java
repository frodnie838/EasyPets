package com.easypets.ui.auth;

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
import androidx.core.content.ContextCompat;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.easypets.R;
import com.easypets.ui.main.MainActivity;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
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
    private EditText nombreEditText, apellidosEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private CheckBox termsCheckBox;
    private Button btnRegistrar, btnGoogle;
    private TextView loginTextView;

    // Variables de Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference db;

    // ✨ LA NUEVA API DE GOOGLE (Credential Manager) ✨
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();

        // Inicializar el nuevo Credential Manager
        credentialManager = CredentialManager.create(this);

        // Vincular las vistas
        nombreEditText = findViewById(R.id.nombreEditText);
        apellidosEditText = findViewById(R.id.apellidosEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.passwordConfirmEditText);
        termsCheckBox = findViewById(R.id.termsCheckBox);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        btnGoogle = findViewById(R.id.btnGoogle);
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
        btnGoogle.setOnClickListener(v -> signInConGoogle());

        loginTextView.setOnClickListener(v -> {
            Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(i);
        });

        // MAGIA: Si el usuario viene rebotado del Login porque no tenía cuentas,
        // abrimos el desplegable de Google automáticamente.
        boolean abrirGoogle = getIntent().getBooleanExtra("abrirGoogleAutomatico", false);
        if (abrirGoogle) {
            signInConGoogle();
        }
    }

    // ------------------------------------------------------------
    // ✨ NUEVO SISTEMA DE LOGIN CON GOOGLE ✨
    // ------------------------------------------------------------
    private void signInConGoogle() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // Fundamental: permite ver todas las cuentas y añadir nuevas
                .setServerClientId(getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(false)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                new android.os.CancellationSignal(),
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        manejarRespuestaGoogle(result);
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        // Si el error es que no hay cuentas en el dispositivo (móvil vacío)
                        if (e instanceof androidx.credentials.exceptions.NoCredentialException ||
                                (e.getMessage() != null && e.getMessage().contains("No credentials available"))) {

                            Toast.makeText(RegisterActivity.this, "No hay cuentas de Google. Vamos a añadir una...", Toast.LENGTH_LONG).show();

                            // Le abrimos la pantalla nativa de Android para añadir cuenta de Google
                            try {
                                Intent addAccountIntent = new Intent(android.provider.Settings.ACTION_ADD_ACCOUNT);
                                addAccountIntent.putExtra(android.provider.Settings.EXTRA_ACCOUNT_TYPES, new String[]{"com.google"});
                                startActivity(addAccountIntent);
                            } catch (Exception ex) {
                                // Si falla, lo mandamos a los ajustes generales
                                startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                            }
                        }
                        // Para cualquier otro error (como si el usuario cierra la pestaña manualmente), lo ignoramos.
                    }
                }
        );
    }

    private void manejarRespuestaGoogle(GetCredentialResponse result) {
        try {
            androidx.credentials.Credential credential = result.getCredential();

            if (credential instanceof androidx.credentials.CustomCredential &&
                    credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {

                GoogleIdTokenCredential googleCredential = GoogleIdTokenCredential.createFrom(credential.getData());
                AuthCredential authCredential = GoogleAuthProvider.getCredential(googleCredential.getIdToken(), null);

                mAuth.signInWithCredential(authCredential).addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        String nombre = googleCredential.getGivenName();
                        String apellidos = googleCredential.getFamilyName();
                        String correo = user.getEmail();

                        if (nombre == null) nombre = "";
                        if (apellidos == null) apellidos = "";

                        guardarDatosFirestore(user.getUid(), nombre, apellidos, correo);
                    } else {
                        Toast.makeText(RegisterActivity.this, "Error Auth: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Exception e) {
            Toast.makeText(this, "Fallo al procesar credencial", Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------------------------------------------------
    // REGISTRO TRADICIONAL EMAIL / CONTRASEÑA
    // ------------------------------------------------------------
    private void registrarUsuario() {
        String nombre = nombreEditText.getText().toString().trim();
        String apellidos = apellidosEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(nombre)) { nombreEditText.setError("El nombre es obligatorio"); return; }
        if (TextUtils.isEmpty(apellidos)) { apellidosEditText.setError("El primer apellido es obligatorio"); return; }
        if (TextUtils.isEmpty(email)) { emailEditText.setError("El correo es obligatorio"); return; }
        if (TextUtils.isEmpty(password) || password.length() < 6) { passwordEditText.setError("La contraseña debe tener al menos 6 caracteres"); return; }
        if (!password.equals(confirmPassword)) { confirmPasswordEditText.setError("Las contraseñas no coinciden"); return; }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        guardarDatosFirestore(user.getUid(), nombre, apellidos, email);
                    } else {
                        Toast.makeText(RegisterActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ------------------------------------------------------------
    // GUARDAR EN REALTIME DATABASE
    // ------------------------------------------------------------
    private void guardarDatosFirestore(String uid, String nombre, String apellidos, String email) {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Europe/Madrid"));
        String fecha = sdf.format(date);

        Map<String, Object> usuario = new HashMap<>();
        usuario.put("idUsuario", uid);
        usuario.put("nombre", nombre);
        usuario.put("apellidos", apellidos);
        usuario.put("correo", email);
        usuario.put("fechaRegistro", fecha);
        usuario.put("timestamp", System.currentTimeMillis());

        db.child("users").child(uid).setValue(usuario)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "¡Cuenta creada!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Error al guardar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}