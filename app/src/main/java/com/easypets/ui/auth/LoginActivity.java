package com.easypets.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.easypets.ui.main.MainActivity;
import com.easypets.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
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
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 2. Configurar Google Sign In (Igual que en Registro)
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

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
            // Si la acción es "Done" (Hecho) o "Go" (Ir)
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                loginUsuario(); // <--- Llamamos a tu función de login
                return true;    // Indicamos que ya hemos manejado el evento
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
        // Ir al Main
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
            emailEditText.setError("El correo es obligatorio");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("La contraseña es obligatoria");
            return;
        }

        // Intento de login en Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        irAMainActivity();
                    } else {
                        Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ------------------------------------------------------------
    // LÓGICA DE LOGIN CON GOOGLE
    // ------------------------------------------------------------
    private void signInConGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(this, "Fallo Google: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        // Comprobación si tiene cuenta registrada
                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid());

                        userRef.get().addOnCompleteListener(dbTask -> {
                            if (dbTask.isSuccessful()) {
                                if (dbTask.getResult().exists()) {
                                    // Si ya tiene entramos directamente sin tocar nada
                                    irAMainActivity();
                                } else {
                                    // Si no tiene y es la primera vez, guardamos sus datos
                                    String nombre = account.getGivenName();
                                    String apellidos = account.getFamilyName();
                                    String correo = user.getEmail();

                                    if (nombre == null) nombre = "";
                                    if (apellidos == null) apellidos = "";

                                    // Llamamos al método de guardar (tienes que copiarlo abajo)
                                    guardarDatosEnBaseDeDatos(user.getUid(), nombre, apellidos, correo);
                                }
                            } else {
                                // Si falla la conexión a la BD, dejamos entrar igual
                                irAMainActivity();
                            }
                        });

                    } else {
                        Toast.makeText(this, "Error Auth: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void guardarDatosEnBaseDeDatos(String uid, String nombre, String apellidos, String email) {
        long timestamp = System.currentTimeMillis();

        // Formatear fecha
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("Europe/Madrid"));
        String fechaBonita = sdf.format(new java.util.Date(timestamp));

        Map<String, Object> usuario = new HashMap<>();
        usuario.put("idUsuario", uid);
        usuario.put("nombre", nombre);
        usuario.put("apellidos", apellidos);
        usuario.put("correo", email);
        usuario.put("fechaRegistro", fechaBonita);
        usuario.put("timestamp", timestamp);

        // Usamos FirebaseDatabase.getInstance().getReference() directamente si no tienes variable global mDatabase
        FirebaseDatabase.getInstance().getReference().child("users").child(uid).setValue(usuario)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(LoginActivity.this, "¡Bienvenido "+nombre+"!", Toast.LENGTH_SHORT).show();
                    irAMainActivity();
                })
                .addOnFailureListener(e -> {
                    // Si falla al guardar, entramos igualmente (mejor eso que quedarse bloqueado)
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
        passwordResetDialog.setMessage("Introduce tu correo para recibir el enlace de recuperación.");
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

        passwordResetDialog.setNegativeButton("Cancelar", (dialog, which) -> {
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

    // Opcional: Si el usuario ya está logueado, saltar login al abrir app
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            irAMainActivity();
        }
    }
}