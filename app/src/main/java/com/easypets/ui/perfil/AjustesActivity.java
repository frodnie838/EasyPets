package com.easypets.ui.perfil;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.ClearCredentialException;

import com.easypets.R;
import com.easypets.ui.auth.LoginActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AjustesActivity extends AppCompatActivity {

    private SwitchMaterial switchNotificaciones;
    private View btnSoporte;
    private TextView tvEliminarCuenta;
    private SharedPreferences prefs;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);

        // Barra superior con botón de retorno
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Configuración");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mAuth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences("AjustesEasyPets", MODE_PRIVATE);

        switchNotificaciones = findViewById(R.id.switchNotificaciones);
        btnSoporte = findViewById(R.id.btnSoporte);
        tvEliminarCuenta = findViewById(R.id.tvEliminarCuentaLink);

        // Cargar ajustes guardados
        switchNotificaciones.setChecked(prefs.getBoolean("notificaciones", true));

        // Guardar preferencia de notificaciones
        switchNotificaciones.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("notificaciones", isChecked).apply();
            String msg = isChecked ? "Notificaciones activadas" : "Notificaciones desactivadas";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        // Botón de soporte por correo
        btnSoporte.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:frodnie838@g.educaand.es"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Soporte EasyPets");
            startActivity(Intent.createChooser(intent, "Enviar correo..."));
        });

        // Enlace para borrar cuenta
        tvEliminarCuenta.setOnClickListener(v -> mostrarConfirmacionBorrado());

        // Si es invitado, ocultar el enlace de eliminar
        if (mAuth.getCurrentUser() == null) {
            tvEliminarCuenta.setVisibility(View.GONE);
        }
    }

    // Muestra el diálogo de aviso antes de borrar todo
    private void mostrarConfirmacionBorrado() {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Acción Crítica")
                .setMessage("¿Deseas eliminar tu cuenta para siempre? Se borrarán todos tus datos y mascotas.")
                .setPositiveButton("Eliminar", (dialog, which) -> procesoBorradoCascada())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Borra mascotas, perfil y cuenta de autenticación
    private void procesoBorradoCascada() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();

        // Borrar mascotas -> Borrar Perfil -> Borrar Auth
        db.child("mascotas").child(uid).removeValue().addOnCompleteListener(t1 -> {
            db.child("users").child(uid).removeValue().addOnCompleteListener(t2 -> {
                user.delete().addOnCompleteListener(t3 -> {
                    if (t3.isSuccessful()) {
                        mAuth.signOut();
                        limpiarYSalir();
                    } else {
                        Toast.makeText(this, "Error de seguridad. Re-autentícate para borrar.", Toast.LENGTH_LONG).show();
                    }
                });
            });
        });
    }

    // Limpia rastro de Google y vuelve al Login
    private void limpiarYSalir() {
        CredentialManager cm = CredentialManager.create(this);
        cm.clearCredentialStateAsync(new ClearCredentialStateRequest(), new android.os.CancellationSignal(),
                ContextCompat.getMainExecutor(this), new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override public void onResult(Void result) { irALogin(); }
                    @Override public void onError(ClearCredentialException e) { irALogin(); }
                });
    }

    // Redirección final
    private void irALogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Manejo del botón de volver de la barra superior (Sustituye a onBackPressed)
    @Override
    public boolean onSupportNavigateUp() {
        finish(); // Cierra esta actividad y vuelve a la anterior
        return true;
    }
}