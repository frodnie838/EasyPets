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

/**
 * Actividad responsable de gestionar la autenticación de usuarios en el sistema.
 * Implementa inicio de sesión mediante correo y contraseña, autenticación federada (Google),
 * opciones de recuperación de cuenta y acceso como invitado.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private MaterialButton btnLogin, btnGoogle;
    private TextView registerTextView, forgotPasswordTextView, guestTextView;

    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.btnGoogle);
        registerTextView = findViewById(R.id.registerTextView);
        guestTextView = findViewById(R.id.guestTextView);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);

        btnLogin.setOnClickListener(v -> loginUsuario());

        passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                loginUsuario();
                return true;
            }
            return false;
        });

        btnGoogle.setOnClickListener(v -> signInConGoogle());

        registerTextView.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(i);
        });

        guestTextView.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(i);
            finish();
        });

        forgotPasswordTextView.setOnClickListener(v -> mostrarDialogoRecuperarPassword());
    }

    /**
     * Valida las credenciales introducidas y solicita la autenticación a Firebase Auth.
     * Procesa y notifica los posibles errores de conexión o credenciales inválidas.
     */
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

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        irAMainActivity();
                    } else {
                        String mensajeError;
                        Exception e = task.getException();

                        if (e instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                            mensajeError = "Esta cuenta no existe o ha sido deshabilitada.";
                        } else if (e instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                            mensajeError = "El correo o la contraseña son incorrectos.";
                        } else if (e instanceof com.google.firebase.FirebaseNetworkException) {
                            mensajeError = "No hay conexión a internet.";
                        } else if (e instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                            mensajeError = "Este correo ya está en uso con otro método de acceso.";
                        } else {
                            mensajeError = "Error al iniciar sesión. Inténtalo de nuevo más tarde.";
                        }

                        Toast.makeText(LoginActivity.this, mensajeError, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Inicializa el flujo de autenticación mediante Google Sign-In utilizando CredentialManager.
     * En caso de no encontrar credenciales almacenadas, redirige al registro como método de contingencia.
     */
    private void signInConGoogle() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
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
                        if (e instanceof androidx.credentials.exceptions.NoCredentialException ||
                                e.getMessage().contains("No credentials available")) {

                            Toast.makeText(LoginActivity.this, "No se encontraron cuentas guardadas. Vamos a añadir una.", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                            intent.putExtra("abrirGoogleAutomatico", true);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Error de Google: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    /**
     * Procesa la respuesta exitosa del proveedor de credenciales y autentica al usuario en Firebase.
     *
     * @param result Respuesta de credenciales obtenida de Google.
     */
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
                        if (user != null) {
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
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Error Auth: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Exception e) {
            Toast.makeText(this, "Fallo al procesar credencial", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Persiste la información inicial del usuario en la base de datos tras un registro exitoso.
     *
     * @param uid       Identificador de usuario proporcionado por Firebase Auth.
     * @param nombre    Nombre del usuario.
     * @param apellidos Apellidos del usuario.
     * @param nick      Nickname generado automáticamente.
     * @param email     Correo electrónico del usuario.
     * @param fotoUrl   URL de la fotografía de perfil obtenida del proveedor.
     */
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

    /**
     * Despliega un cuadro de diálogo nativo permitiendo al usuario solicitar
     * un restablecimiento de contraseña vía correo electrónico.
     */
    private void mostrarDialogoRecuperarPassword() {
        EditText resetMail = new EditText(this);
        AlertDialog.Builder passwordResetDialog = new AlertDialog.Builder(this);
        passwordResetDialog.setTitle("Recuperar Contraseña");

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
            // Acción cancelada por el usuario
        });

        passwordResetDialog.create().show();
    }

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