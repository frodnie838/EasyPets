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
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Actividad encargada de la configuración de la cuenta del usuario.
 * Gestiona las preferencias de notificaciones Push (FCM), el acceso a la información
 * legal (Privacidad y Términos) y ejecuta la eliminación total (RGPD) de la cuenta en cascada.
 */
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

    /**
     * Sincroniza el estado del componente UI con el registro real del token FCM
     * en Firebase Database. Administra la suscripción y desuscripción a notificaciones push.
     *
     * @param user Instancia del usuario autenticado actualmente.
     */
    private void configurarSwitchNotificaciones(FirebaseUser user) {
        mDatabase.child("usuarios").child(user.getUid()).child("fcmToken")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        switchNotificaciones.setChecked(snapshot.exists());
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        switchNotificaciones.setOnCheckedChangeListener((v, isChecked) -> {
            prefs.edit().putBoolean("notificaciones", isChecked).apply();

            if (isChecked) {
                FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
                    mDatabase.child("usuarios").child(user.getUid()).child("fcmToken").setValue(token);
                    Toast.makeText(this, "Notificaciones activadas", Toast.LENGTH_SHORT).show();
                });
            } else {
                mDatabase.child("usuarios").child(user.getUid()).child("fcmToken").removeValue();
                Toast.makeText(this, "Notificaciones desactivadas", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Despliega una alerta de confirmación previa a la ejecución de procesos críticos
     * de eliminación de datos.
     */
    private void mostrarConfirmacionBorrado() {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Acción Crítica")
                .setMessage("¿Deseas eliminar tu cuenta para siempre? Se borrarán todos tus datos (mascotas, eventos, hilos, fotos de comunidad y artículos).")
                .setPositiveButton("Eliminar", (dialog, which) -> procesoBorradoCascada())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Renderiza un diálogo nativo personalizado para presentar textos legales extensos
     * aplicando estilos corporativos en las cabeceras.
     *
     * @param titulo  Cabecera del documento legal.
     * @param mensaje Cuerpo completo del documento.
     */
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

    /**
     * Ejecuta una transacción masiva de borrado estructurado en Firebase (Storage y Realtime DB).
     * Compila todas las promesas asíncronas de eliminación de activos multimedia, nodos privados
     * y aplica un borrado lógico (Soft Delete) en las interacciones comunitarias antes de purgar
     * finalmente la cuenta de Authentication.
     */
    private void procesoBorradoCascada() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();

        Toast.makeText(this, "Iniciando borrado seguro. Por favor, espera...", Toast.LENGTH_LONG).show();

        List<Task<?>> tareasDeBorrado = new ArrayList<>();

        tareasDeBorrado.add(FirebaseStorage.getInstance().getReference("perfiles")
                .child(uid + ".jpg").delete().continueWith(task -> null));

        tareasDeBorrado.add(FirebaseStorage.getInstance().getReference("mascotas_privadas").child(uid).listAll().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) return Tasks.forResult(null);
            List<Task<Void>> deletes = new ArrayList<>();
            for (StorageReference item : task.getResult().getItems()) deletes.add(item.delete());
            return Tasks.whenAll(deletes);
        }));

        tareasDeBorrado.add(FirebaseStorage.getInstance().getReference("mascotas_comunidad").child(uid).listAll().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) return Tasks.forResult(null);
            List<Task<Void>> deletes = new ArrayList<>();
            for (StorageReference item : task.getResult().getItems()) deletes.add(item.delete());
            return Tasks.whenAll(deletes);
        }));

        tareasDeBorrado.add(FirebaseStorage.getInstance().getReference("foro_imagenes").child(uid).listAll().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) return Tasks.forResult(null);
            List<Task<Void>> deletes = new ArrayList<>();
            for (StorageReference item : task.getResult().getItems()) deletes.add(item.delete());
            return Tasks.whenAll(deletes);
        }));

        tareasDeBorrado.add(db.child("notificaciones").child(uid).removeValue());
        tareasDeBorrado.add(db.child("eventos").child(uid).removeValue());
        tareasDeBorrado.add(db.child("mascotas").child(uid).removeValue());

        tareasDeBorrado.add(db.child("articulos_comunidad").orderByChild("idAutor").equalTo(uid).get().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) return Tasks.forResult(null);
            List<Task<Void>> deletes = new ArrayList<>();
            for (DataSnapshot ds : task.getResult().getChildren()) deletes.add(ds.getRef().removeValue());
            return Tasks.whenAll(deletes);
        }));

        tareasDeBorrado.add(db.child("foro_respuestas").get().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) return Tasks.forResult(null);
            List<Task<Void>> updates = new ArrayList<>();
            for (DataSnapshot hiloSnapshot : task.getResult().getChildren()) {
                for (DataSnapshot respuestaSnapshot : hiloSnapshot.getChildren()) {
                    String autorId = respuestaSnapshot.child("idAutor").getValue(String.class);
                    if (uid.equals(autorId)) {
                        updates.add(respuestaSnapshot.getRef().child("eliminado").setValue(true));
                        updates.add(respuestaSnapshot.getRef().child("texto").setValue("🚫 Este mensaje ha sido eliminado por privacidad."));
                        updates.add(respuestaSnapshot.getRef().child("idAutor").setValue("deleted"));
                    }
                }
            }
            return Tasks.whenAll(updates);
        }));

        Tasks.whenAll(tareasDeBorrado).addOnCompleteListener(tareaGlobal -> {
            db.child("usuarios").child(uid).removeValue().addOnCompleteListener(tUser -> {
                user.delete().addOnCompleteListener(tAuth -> {
                    if (tAuth.isSuccessful()) {
                        Toast.makeText(AjustesActivity.this, "Cuenta eliminada de forma segura", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                        limpiarYSalir();
                    } else {
                        Toast.makeText(AjustesActivity.this, "Por seguridad, cierra sesión, vuelve a entrar y repite la acción.", Toast.LENGTH_LONG).show();
                    }
                });
            });
        });
    }

    /**
     * Limpia la pila de navegación de la aplicación y redirige a la pantalla de Login.
     */
    private void limpiarYSalir() {
        Intent intent = new Intent(AjustesActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void irALogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}