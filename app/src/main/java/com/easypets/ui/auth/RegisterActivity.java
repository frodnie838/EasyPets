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

import com.easypets.ui.main.MainActivity;
import com.easypets.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
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

import androidx.annotation.Nullable;


public class RegisterActivity extends AppCompatActivity {

    // Variables de la interfaz
    private EditText nombreEditText, apellidosEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private CheckBox termsCheckBox;
    private Button btnRegistrar, btnGoogle;
    private TextView loginTextView;

    // Variables de Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference db;
    // Variables para Google
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();
        //Configuracion de Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        //Vincular las vistas
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

        loginTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(i);
            }
        });

    }
    //Metodo de google sign in
    private void signInConGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    // Recibe el resultado de la seleccion de cuenta
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

    // Autentica en Firebase con la credencial de Google
    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        String nombre = account.getGivenName();
                        String apellidos = account.getFamilyName();
                        String correo = user.getEmail();

                        if (nombre == null) nombre = "";
                        if (apellidos == null) apellidos = "";
                        // Guardamos en Base de Datos
                        guardarDatosFirestore(user.getUid(), nombre, apellidos, correo);
                    } else {
                        Toast.makeText(this, "Error Auth Firebase: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void registrarUsuario() {
        String nombre = nombreEditText.getText().toString().trim();
        String apellidos = apellidosEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        // VALIDACIONES
        if (TextUtils.isEmpty(nombre)) {
            nombreEditText.setError("El nombre es obligatorio");
            return;
        }
        if (TextUtils.isEmpty(apellidos)) {
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
                        guardarDatosFirestore(user.getUid(), nombre, apellidos, email);
                    } else {
                        Toast.makeText(RegisterActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
    private void guardarDatosFirestore(String uid, String nombre, String apellidos, String email) {
        Date date = new Date(); //Fecha actual
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()); //Formato dia/mes/año hora:minutos
        sdf.setTimeZone(TimeZone.getTimeZone("Europe/Madrid")); //Guarda la hora española y no la del movil local/emulador
        String fecha = sdf.format(date); //Convierte el date en string con el formato dia/mes/año hora:minutos

        Map<String, Object> usuario = new HashMap<>();
        usuario.put("idUsuario", uid);
        usuario.put("nombre", nombre);
        usuario.put("apellidos", apellidos);
        usuario.put("correo", email);
        usuario.put("fechaRegistro", fecha);
        usuario.put("timestamp", System.currentTimeMillis());

        // Guarda en la colección "users"
        db.child("users").child(uid).setValue(usuario)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "¡Cuenta creada!", Toast.LENGTH_SHORT).show();
                    // Redirigir a LoginActivity y borra historial para que no vuelva atrás
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