package com.easypets.ui.perfil;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
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
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class AjustesActivity extends AppCompatActivity {

    private SwitchMaterial switchNotificaciones;
    private View layoutSoporte;
    private TextView tvEliminarCuenta, tvTerminos, tvPrivacidad;
    private SharedPreferences prefs;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        prefs = getSharedPreferences("AjustesEasyPets", MODE_PRIVATE);

        switchNotificaciones = findViewById(R.id.switchNotificaciones);
        layoutSoporte = findViewById(R.id.layoutSoporte);
        tvEliminarCuenta = findViewById(R.id.tvEliminarCuentaLink);
        tvTerminos = findViewById(R.id.tvTerminos);
        tvPrivacidad = findViewById(R.id.tvPrivacidad);

        ImageView btnVolverAjustes = findViewById(R.id.btnVolverAjustes);
        btnVolverAjustes.setOnClickListener(v -> finish());

        tvTerminos.setOnClickListener(v -> mostrarDialogoLegal("Términos y Condiciones", getString(R.string.terminos_condiciones_texto)));
        tvPrivacidad.setOnClickListener(v -> mostrarDialogoLegal("Política de Privacidad", getString(R.string.politica_privacidad_texto)));

        layoutSoporte.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:frodnie838@g.educaand.es"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Soporte EasyPets");
            startActivity(Intent.createChooser(intent, "Enviar correo..."));
        });

        tvEliminarCuenta.setOnClickListener(v -> mostrarConfirmacionBorrado());

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.isAnonymous()) {
            tvEliminarCuenta.setVisibility(View.GONE);
            switchNotificaciones.setEnabled(false);
        } else {
            configurarSwitchNotificaciones(user);
        }
    }

    private void configurarSwitchNotificaciones(FirebaseUser user) {
        // 1. Verificamos en Firebase si realmente tiene el token guardado (estado real)
        mDatabase.child("usuarios").child(user.getUid()).child("fcmToken")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        switchNotificaciones.setChecked(snapshot.exists());
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        // 2. Controlamos el cambio
        switchNotificaciones.setOnCheckedChangeListener((v, isChecked) -> {
            prefs.edit().putBoolean("notificaciones", isChecked).apply();

            if (isChecked) {
                // Generar y subir el token a Firebase para recibir push
                FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
                    mDatabase.child("usuarios").child(user.getUid()).child("fcmToken").setValue(token);
                    Toast.makeText(this, "Notificaciones activadas", Toast.LENGTH_SHORT).show();
                });
            } else {
                // Borrar el token de Firebase (Corte real de notificaciones)
                mDatabase.child("usuarios").child(user.getUid()).child("fcmToken").removeValue();
                Toast.makeText(this, "Notificaciones desactivadas", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarConfirmacionBorrado() {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Acción Crítica")
                .setMessage("¿Deseas eliminar tu cuenta para siempre? Se borrarán todos tus datos (mascotas, eventos, hilos, fotos de comunidad y artículos).")
                .setPositiveButton("Eliminar", (dialog, which) -> procesoBorradoCascada())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoLegal(String titulo, String mensaje) {
        TextView customTitle = new TextView(this);
        customTitle.setText(titulo);
        customTitle.setPadding(60, 60, 60, 0);
        customTitle.setTextSize(20);
        customTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        customTitle.setTextColor(ContextCompat.getColor(this, R.color.color_acento_primario));

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogCustom)
                .setCustomTitle(customTitle)
                .setMessage(mensaje)
                .setPositiveButton("Entendido", (d, which) -> d.dismiss())
                .show();

        TextView tvMensaje = dialog.findViewById(android.R.id.message);
        if (tvMensaje != null) {
            tvMensaje.setTextColor(Color.BLACK);
        }
    }

    private void procesoBorradoCascada() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();

        // 1. Borrar foto de perfil de Storage (Ahorro de servidor)
        StorageReference refPerfil = FirebaseStorage.getInstance().getReference("perfiles").child(uid + ".jpg");
        refPerfil.delete().addOnCompleteListener(task -> {}); // Ignoramos si no tenía foto

        // 2. Borrar publicaciones de Mascotas en Galería y sus comentarios
        db.child("mascotas_comunidad").orderByChild("idAutor").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            db.child("mascotas_comentarios").child(ds.getKey()).removeValue();
                            ds.getRef().removeValue();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        // 3. Borrar buzón de notificaciones
        db.child("notificaciones").child(uid).removeValue();

        // 4. Borrar artículos del usuario
        db.child("articulos_comunidad").orderByChild("idAutor").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) ds.getRef().removeValue();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        // 5. Borrar hilos del usuario (y las respuestas asociadas a esos hilos)
        db.child("foro_hilos").orderByChild("idAutor").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            db.child("foro_respuestas").child(ds.getKey()).removeValue();
                            ds.getRef().removeValue();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        // 6. Borrado Lógico de las respuestas en hilos de OTRAS personas
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

        // 7. Borrado final en cascada: Eventos -> Mascotas -> Perfil -> Cuenta Firebase Auth
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