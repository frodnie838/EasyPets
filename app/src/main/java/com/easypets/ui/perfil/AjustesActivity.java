package com.easypets.ui.perfil;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.ClearCredentialException;

import com.easypets.R;
import com.easypets.ui.auth.LoginActivity;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AjustesActivity extends AppCompatActivity {

    private SwitchMaterial switchNotificaciones, switchModoOscuro;
    private View layoutSoporte;
    private TextView tvEliminarCuenta;
    private SharedPreferences prefs;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);

        mAuth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences("AjustesEasyPets", MODE_PRIVATE);

        switchNotificaciones = findViewById(R.id.switchNotificaciones);
        switchModoOscuro = findViewById(R.id.switchModoOscuro);
        layoutSoporte = findViewById(R.id.layoutSoporte);
        tvEliminarCuenta = findViewById(R.id.tvEliminarCuentaLink);

        switchNotificaciones.setChecked(prefs.getBoolean("notificaciones", true));
        switchModoOscuro.setChecked(prefs.getBoolean("oscuro", false));

        switchNotificaciones.setOnCheckedChangeListener((v, isChecked) -> {
            prefs.edit().putBoolean("notificaciones", isChecked).apply();
            Toast.makeText(this, isChecked ? "Notificaciones activadas" : "Notificaciones desactivadas", Toast.LENGTH_SHORT).show();
        });

        layoutSoporte.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:frodnie838@g.educaand.es"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Soporte EasyPets");
            startActivity(Intent.createChooser(intent, "Enviar correo..."));
        });

        tvEliminarCuenta.setOnClickListener(v -> mostrarConfirmacionBorrado());

        if (mAuth.getCurrentUser() == null) {
            tvEliminarCuenta.setVisibility(View.GONE);
        }
    }

    private void mostrarConfirmacionBorrado() {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Acción Crítica")
                .setMessage("¿Deseas eliminar tu cuenta para siempre? Se borrarán todos tus datos (mascotas, eventos, hilos y artículos).")
                .setPositiveButton("Eliminar", (dialog, which) -> procesoBorradoCascada())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void procesoBorradoCascada() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();

        // 1. Borrar artículos del usuario
        db.child("articulos_comunidad").orderByChild("idAutor").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) ds.getRef().removeValue();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        // 2. Borrar hilos del usuario (y las respuestas asociadas a esos hilos)
        db.child("foro_hilos").orderByChild("idAutor").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            // Borramos las respuestas de ese hilo concreto
                            db.child("foro_respuestas").child(ds.getKey()).removeValue();
                            // Borramos el hilo
                            ds.getRef().removeValue();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        // 3. Borrado Lógico de las respuestas en hilos de OTRAS personas
        db.child("foro_respuestas").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot hiloSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot respuestaSnapshot : hiloSnapshot.getChildren()) {
                        String autorId = respuestaSnapshot.child("idAutor").getValue(String.class);
                        if (uid.equals(autorId)) {
                            respuestaSnapshot.getRef().child("eliminado").setValue(true);
                            respuestaSnapshot.getRef().child("texto").setValue("🚫 Este mensaje ha sido eliminado porque el usuario borró su cuenta.");
                            respuestaSnapshot.getRef().child("idAutor").setValue("deleted");
                            respuestaSnapshot.getRef().child("editado").setValue(false);
                        }
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 4. Borrado final en cascada: Eventos -> Mascotas -> Perfil -> Cuenta Firebase
        db.child("eventos").child(uid).removeValue().addOnCompleteListener(tEventos -> {
            db.child("mascotas").child(uid).removeValue().addOnCompleteListener(tMascotas -> {
                db.child("usuarios").child(uid).removeValue().addOnCompleteListener(tUser -> {
                    user.delete().addOnCompleteListener(tAuth -> {
                        if (tAuth.isSuccessful()) {
                            Toast.makeText(AjustesActivity.this, "Cuenta y datos eliminados correctamente", Toast.LENGTH_SHORT).show();
                            mAuth.signOut();
                            limpiarYSalir();
                        } else {
                            Toast.makeText(AjustesActivity.this, "Por seguridad, cierra sesión, vuelve a entrar y repite este paso.", Toast.LENGTH_LONG).show();
                        }
                    });
                });
            });
        });
    }

    private void limpiarYSalir() {
        CredentialManager cm = CredentialManager.create(this);
        cm.clearCredentialStateAsync(new ClearCredentialStateRequest(), new android.os.CancellationSignal(),
                ContextCompat.getMainExecutor(this), new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override public void onResult(Void result) { irALogin(); }
                    @Override public void onError(ClearCredentialException e) { irALogin(); }
                });
    }

    private void irALogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}