package com.easypets.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.easypets.R;
import com.easypets.ui.base.MainActivity;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private MaterialButton btnLogin, btnGoogle;
    private TextView registerTextView, forgotPasswordTextView, guestTextView;

    private FirebaseAuth mAuth;
    // LA NUEVA HERRAMIENTA DE GOOGLE (ANDROID 14+)
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 2. Inicializar el nuevo Credential Manager
        credentialManager = CredentialManager.create(this);

        // 3. Vincular Vistas
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.btnGoogle);
        registerTextView = findViewById(R.id.registerTextView);
        guestTextView = findViewById(R.id.guestTextView);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);

        // 4. Listeners (Botones)

        // Botón Login Normal
        btnLogin.setOnClickListener(v -> loginUsuario());

        // Configurar el "Enter" en el teclado
        passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                loginUsuario();
                return true;
            }
            return false;
        });

        // Botón Login Google
        btnGoogle.setOnClickListener(v -> signInConGoogle());

        // Ir a Registro
        registerTextView.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(i);
        });

        // Ir al Main (Invitado)
        guestTextView.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(i);
            finish();
        });

        // Olvidé contraseña
        forgotPasswordTextView.setOnClickListener(v -> mostrarDialogoRecuperarPassword());
    }

    // ------------------------------------------------------------
    // LÓGICA DE LOGIN EMAIL / PASSWORD
    // ------------------------------------------------------------
    private void loginUsuario() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError(getString(R.string.error_correo_vacio));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError(getString(R.string.error_password_vacio));
            return;
        }

        // Intento de login en Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        irAMainActivity();
                    } else {
                        String mensajeError;
                        Exception e = task.getException();

                        if (e instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                            // El usuario no existe o está deshabilitado
                            mensajeError = "Esta cuenta no existe o ha sido deshabilitada.";
                        } else if (e instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                            // La contraseña no coincide o el email está mal formado
                            mensajeError = "El correo o la contraseña son incorrectos.";
                        } else if (e instanceof com.google.firebase.FirebaseNetworkException) {
                            // Error de conexión (modo avión, sin datos...)
                            mensajeError = "No hay conexión a internet.";
                        } else if (e instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                            mensajeError = "Este correo ya está en uso con otro método de acceso.";
                        } else {
                            // Si es un error raro (como demasiados intentos), lo capturamos aquí
                            mensajeError = "Error al iniciar sesión. Inténtalo de nuevo más tarde.";
                        }

                        Toast.makeText(LoginActivity.this, mensajeError, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ------------------------------------------------------------
    // LÓGICA DE LOGIN CON GOOGLE (NUEVO SISTEMA)
    // ------------------------------------------------------------
    private void signInConGoogle() {
        // Configuramos la petición para obtener el ID de Google
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(false)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        // Lanzamos la nueva interfaz nativa de Android
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
                        // ✨ AQUÍ ESTÁ LA MAGIA ✨
                        // Si nos dice que no hay credenciales (No credentials available)
                        // o el usuario cierra la ventana, intentamos el modo "Registro" como fallback.
                        if (e instanceof androidx.credentials.exceptions.NoCredentialException ||
                                e.getMessage().contains("No credentials available")) {

                            // Mostramos un mensajito amable al usuario
                            Toast.makeText(LoginActivity.this, "No se encontraron cuentas guardadas. Vamos a añadir una.", Toast.LENGTH_SHORT).show();

                            // Redirigimos al usuario a la pantalla de Registro,
                            // que sabemos que SÍ funciona para emuladores vacíos.
                            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                            // Le pasamos un aviso a RegisterActivity de que venimos de un fallo de Login
                            intent.putExtra("abrirGoogleAutomatico", true);
                            startActivity(intent);
                            finish(); // Cerramos el login actual

                        } else {
                            // Para cualquier otro error (sin internet, etc), mostramos el mensaje normal
                            Toast.makeText(LoginActivity.this, "Error de Google: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
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

                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("usuarios").child(user.getUid());

                        userRef.get().addOnCompleteListener(dbTask -> {
                            if (dbTask.isSuccessful() && !dbTask.getResult().exists()) {
                                String nombre = googleCredential.getGivenName();
                                String apellidos = googleCredential.getFamilyName();
                                String urlFoto = "";
                                if (user.getPhotoUrl() != null) {
                                    urlFoto = user.getPhotoUrl().toString();
                                } else if (googleCredential.getProfilePictureUri() != null) {
                                    urlFoto = googleCredential.getProfilePictureUri().toString();
                                }
                                if (nombre == null) nombre = user.getDisplayName();
                                if (nombre == null) nombre = "Usuario";
                                if (apellidos == null) apellidos = "";
                                String nickGenerado = nombre.replaceAll("\\s+", "") + (System.currentTimeMillis() % 10000);
                                guardarDatosEnBaseDeDatos(user.getUid(), nombre, apellidos, nickGenerado, user.getEmail(), urlFoto);
                            } else {
                                irAMainActivity();
                            }
                        });
                    } else {
                        Toast.makeText(LoginActivity.this, "Error Auth: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Exception e) {
            Toast.makeText(this, "Fallo al procesar credencial", Toast.LENGTH_SHORT).show();
        }
    }

    private void guardarDatosEnBaseDeDatos(String uid, String nombre, String apellidos, String nick, String email, String fotoUrl) {
        long timestamp = System.currentTimeMillis();

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("Europe/Madrid"));
        String fechaBonita = sdf.format(new java.util.Date(timestamp));

        Map<String, Object> usuario = new HashMap<>();
        usuario.put("idUsuario", uid);
        usuario.put("nombre", nombre);
        usuario.put("apellidos", apellidos);
        usuario.put("nick", nick);
        usuario.put("correo", email);
        usuario.put("fechaRegistro", fechaBonita);
        usuario.put("timestamp", timestamp);
        usuario.put("rol", "usuario");

        if (fotoUrl != null && !fotoUrl.isEmpty()) {
            usuario.put("fotoPerfil", fotoUrl);
        }

        FirebaseDatabase.getInstance().getReference().child("usuarios").child(uid).setValue(usuario)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(LoginActivity.this, "¡Bienvenido " + nombre + "!", Toast.LENGTH_SHORT).show();
                    irAMainActivity();
                })
                .addOnFailureListener(e -> {
                    irAMainActivity();
                });
    }

    // ------------------------------------------------------------
    // LÓGICA DE RECUPERAR CONTRASEÑA
    // ------------------------------------------------------------
    private void mostrarDialogoRecuperarPassword() {
        EditText resetMail = new EditText(this);
        AlertDialog.Builder passwordResetDialog = new AlertDialog.Builder(this);
        passwordResetDialog.setTitle("Recuperar Contraseña");

        // Fíjate que aquí uso una string externa si la tienes, o pongo el texto normal
        passwordResetDialog.setMessage(getString(R.string.correo_recuperar_pass) != null ? getString(R.string.correo_recuperar_pass) : "Introduce tu correo para recibir el enlace.");

        passwordResetDialog.setView(resetMail);

        passwordResetDialog.setPositiveButton("Enviar", (dialog, which) -> {
            String mail = resetMail.getText().toString();
            if (TextUtils.isEmpty(mail)) {
                Toast.makeText(LoginActivity.this, "Debes escribir un correo", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.sendPasswordResetEmail(mail).addOnSuccessListener(aVoid -> {
                Toast.makeText(LoginActivity.this, "Enlace enviado a tu correo.", Toast.LENGTH_LONG).show();
            }).addOnFailureListener(e -> {
                Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        });

        passwordResetDialog.setNegativeButton(getString(R.string.cancelar) != null ? getString(R.string.cancelar) : "Cancelar", (dialog, which) -> {
            // No hacer nada
        });

        passwordResetDialog.create().show();
    }

    // ------------------------------------------------------------
    // NAVEGACIÓN
    // ------------------------------------------------------------
    private void irAMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            irAMainActivity();
        }
    }
}