package com.easypets.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Actividad encargada de gestionar el registro de nuevos usuarios.
 * Incluye validación de campos, comprobación de disponibilidad del nickname,
 * aceptación de términos y condiciones, e integración con Google Sign-In.
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText nombreEditText, apellidosEditText, nickEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private CheckBox termsCheckBox;
    private Button btnRegistrar, btnGoogle;
    private TextView loginTextView, tvTerminos;

    private FirebaseAuth mAuth;
    private DatabaseReference db;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Configuración de la barra de estado para mantener consistencia visual con MainActivity
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();
        credentialManager = CredentialManager.create(this);

        nombreEditText = findViewById(R.id.nombreEditText);
        apellidosEditText = findViewById(R.id.apellidosEditText);
        nickEditText = findViewById(R.id.nickEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.passwordConfirmEditText);
        termsCheckBox = findViewById(R.id.termsCheckBox);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        btnGoogle = findViewById(R.id.btnGoogle);
        loginTextView = findViewById(R.id.loginTextView);
        tvTerminos = findViewById(R.id.tvTerminos);

        // Estado inicial del botón de registro
        btnRegistrar.setEnabled(false);
        btnRegistrar.setAlpha(0.5f);

        configurarTextoTerminos(tvTerminos);

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

        // Intercepta la solicitud automática proveniente del fallo en LoginActivity
        boolean abrirGoogle = getIntent().getBooleanExtra("abrirGoogleAutomatico", false);
        if (abrirGoogle) {
            signInConGoogle();
        }
    }

    /**
     * Aplica un SpannableString al TextView de términos y condiciones para habilitar
     * la interactividad parcial del texto.
     *
     * @param tv TextView objetivo de la configuración.
     */
    private void configurarTextoTerminos(TextView tv) {
        String textoCompleto = "Acepto los Términos y Condiciones";
        SpannableString spannableString = new SpannableString(textoCompleto);

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                mostrarDialogoTerminos();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(ContextCompat.getColor(RegisterActivity.this, R.color.color_acento_primario));
                ds.setUnderlineText(true);
            }
        };

        // Aplica el formato interactivo únicamente a la subcadena "Términos y Condiciones"
        spannableString.setSpan(clickableSpan, 11, 33, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tv.setText(spannableString);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setHighlightColor(android.graphics.Color.TRANSPARENT);
    }

    /**
     * Despliega un cuadro de diálogo nativo con la normativa de privacidad
     * y uso responsable de la aplicación.
     */
    private void mostrarDialogoTerminos() {
        new AlertDialog.Builder(this)
                .setTitle("Términos y Condiciones")
                .setMessage("Bienvenido a EasyPets.\n\n" +
                        "1. Uso de la aplicación: Al registrarte, aceptas usar esta aplicación con el único propósito de gestionar la información de tus mascotas.\n\n" +
                        "2. Privacidad: Nos tomamos muy en serio la privacidad de tus datos. Tus fotos, ubicaciones y datos se almacenan de forma segura.\n\n" +
                        "3. Comunidad: Te comprometes a mantener un ambiente de respeto en el foro y la sección de educación.\n\n" +
                        "4. Responsabilidad: EasyPets no sustituye el consejo de un veterinario profesional.")
                .setPositiveButton("Entendido", null)
                .show();
    }

    /**
     * Lanza la interfaz del sistema para la autenticación federada con cuenta de Google.
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
                this, request, new android.os.CancellationSignal(),
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        manejarRespuestaGoogle(result);
                    }
                    @Override
                    public void onError(GetCredentialException e) {
                        if (e instanceof androidx.credentials.exceptions.NoCredentialException ||
                                (e.getMessage() != null && e.getMessage().contains("No credentials available"))) {
                            Toast.makeText(RegisterActivity.this, "No hay cuentas. Vamos a añadir una...", Toast.LENGTH_LONG).show();
                            try {
                                Intent addAccountIntent = new Intent(android.provider.Settings.ACTION_ADD_ACCOUNT);
                                addAccountIntent.putExtra(android.provider.Settings.EXTRA_ACCOUNT_TYPES, new String[]{"com.google"});
                                startActivity(addAccountIntent);
                            } catch (Exception ex) {
                                startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                            }
                        }
                    }
                }
        );
    }

    /**
     * Procesa la respuesta del CredentialManager para validar y crear el perfil del nuevo usuario.
     *
     * @param result Respuesta exitosa de la API de credenciales.
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
                        String uid = user.getUid();

                        db.child("usuarios").child(uid).get().addOnCompleteListener(taskDb -> {
                            if (taskDb.isSuccessful() && taskDb.getResult().exists()) {
                                irAMain();
                            } else {
                                String nombre = googleCredential.getGivenName();
                                String apellidos = googleCredential.getFamilyName();
                                String correo = user.getEmail();
                                String fotoUrl = "";
                                if (user.getPhotoUrl() != null) {
                                    fotoUrl = user.getPhotoUrl().toString();
                                } else if (googleCredential.getProfilePictureUri() != null) {
                                    fotoUrl = googleCredential.getProfilePictureUri().toString();
                                }

                                if (nombre == null) nombre = user.getDisplayName();
                                if (nombre == null) nombre = "Usuario";
                                if (apellidos == null) apellidos = "";

                                String nickGenerado = nombre.replaceAll("\\s+", "") + (System.currentTimeMillis() % 10000);
                                guardarDatosFirestore(uid, nombre, apellidos, nickGenerado, correo, fotoUrl);
                            }
                        });

                    } else {
                        Toast.makeText(RegisterActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Exception e) {
            Toast.makeText(this, "Fallo al leer la cuenta de Google", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Captura los datos de los EditText, valida las reglas de negocio y procede
     * a registrar la credencial en Firebase Authentication. Previamente, verifica
     * que el nickname seleccionado no se encuentre ya registrado.
     */
    private void registrarUsuario() {
        String nombre = nombreEditText.getText().toString().trim();
        String apellidos = apellidosEditText.getText().toString().trim();
        String nick = nickEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(nombre)) { nombreEditText.setError("Introduce un nombre"); return; }
        if (TextUtils.isEmpty(nick)) { nickEditText.setError("Introduce un nick"); return; }
        if (nick.contains(" ")) { nickEditText.setError("El nick no puede tener espacios"); return; }
        if (TextUtils.isEmpty(email)) { emailEditText.setError("Introduce un correo"); return; }
        if (TextUtils.isEmpty(password) || password.length() < 6) { passwordEditText.setError("Mínimo 6 carácteres"); return; }
        if (!password.equals(confirmPassword)) { confirmPasswordEditText.setError("Las contraseñas no son iguales"); return; }

        btnRegistrar.setEnabled(false);
        btnRegistrar.setText("Comprobando...");

        // Verificación de disponibilidad del nickname en la base de datos
        db.child("usuarios").orderByChild("nick").equalTo(nick).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    nickEditText.setError("Este nick ya está en uso");
                    nickEditText.requestFocus();
                    btnRegistrar.setEnabled(true);
                    btnRegistrar.setText("Registrarse");
                } else {
                    mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                guardarDatosFirestore(user.getUid(), nombre, apellidos, nick, email,null);
                            }
                        } else {
                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                Toast.makeText(RegisterActivity.this, "Este correo ya está registrado", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(RegisterActivity.this, "Error al registrarse", Toast.LENGTH_SHORT).show();
                            }
                            btnRegistrar.setEnabled(true);
                            btnRegistrar.setText("Registrarse");
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RegisterActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
                btnRegistrar.setEnabled(true);
                btnRegistrar.setText("Registrarse");
            }
        });
    }

    /**
     * Escribe el nodo estructural del usuario en la base de datos en tiempo real
     * tras validarse correctamente sus credenciales en Authentication.
     *
     * @param uid       Identificador de usuario.
     * @param nombre    Nombre proporcionado en el formulario.
     * @param apellidos Apellidos proporcionados (opcional).
     * @param nick      Nombre de usuario único.
     * @param email     Dirección de correo electrónico.
     * @param fotoUrl   Ruta del avatar del proveedor externo, nulo en registro tradicional.
     */
    private void guardarDatosFirestore(String uid, String nombre, String apellidos, String nick, String email, String fotoUrl) {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Europe/Madrid"));
        String fecha = sdf.format(date);

        Map<String, Object> usuario = new HashMap<>();
        usuario.put("idUsuario", uid);
        usuario.put("nombre", nombre);
        usuario.put("apellidos", apellidos);
        usuario.put("nick", nick);
        usuario.put("correo", email);
        usuario.put("fechaRegistro", fecha);
        usuario.put("timestamp", System.currentTimeMillis());
        usuario.put("rol", "usuario");

        if (fotoUrl != null && !fotoUrl.isEmpty()) {
            usuario.put("fotoPerfil", fotoUrl);
        }

        db.child("usuarios").child(uid).setValue(usuario)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "¡Bienvenido @" + nick + "!", Toast.LENGTH_SHORT).show();
                    irAMain();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Fallo al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnRegistrar.setEnabled(true);
                    btnRegistrar.setText("Registrarse");
                });
    }

    private void irAMain() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}