package com.easypets.ui.perfil;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.fragment.app.Fragment;

import com.easypets.R;
import com.easypets.ui.auth.LoginActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class PerfilFragment extends Fragment {

    private ImageView ivFotoPerfil;
    private TextView tvNombre, tvCorreo;
    private MaterialButton btnCerrarSesion, btnEditarPerfil, btnAjustes, btnEliminarCuenta;

    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    private String nombreActual = "";
    private String apellidosActuales = "";
    private String fotoBase64Actual = "";
    private String fotoBase64Temporal = "";
    private ImageView ivFotoDialogoTemporal;

    // Lanzador para abrir la galería de fotos
    private final ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest> photoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    procesarFotoTemporal(uri);
                }
            }
    );

    public PerfilFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivFotoPerfil = view.findViewById(R.id.ivFotoPerfilUsuario);
        tvNombre = view.findViewById(R.id.tvNombrePerfilUsuario);
        tvCorreo = view.findViewById(R.id.tvCorreoPerfilUsuario);
        btnCerrarSesion = view.findViewById(R.id.btnCerrarSesion);
        btnEditarPerfil = view.findViewById(R.id.btnEditarPerfil);
        btnAjustes = view.findViewById(R.id.btnAjustes);
        btnEliminarCuenta = view.findViewById(R.id.btnEliminarCuenta);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Verificamos si es un usuario real o un invitado
        if (currentUser != null) {
            configurarPerfilUsuario(currentUser);
        } else {
            configurarModoInvitado();
        }

        btnCerrarSesion.setOnClickListener(v -> cerrarSesion());
    }

    // Configura la pantalla para un usuario con cuenta
    private void configurarPerfilUsuario(FirebaseUser currentUser) {
        tvCorreo.setText(currentUser.getEmail());
        userRef = FirebaseDatabase.getInstance().getReference().child("users").child(currentUser.getUid());

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    nombreActual = snapshot.child("nombre").getValue(String.class);
                    apellidosActuales = snapshot.child("apellidos").getValue(String.class);

                    String nombreCompleto = (nombreActual != null ? nombreActual : "") + " " + (apellidosActuales != null ? apellidosActuales : "");
                    tvNombre.setText(nombreCompleto.trim().isEmpty() ? "Usuario EasyPets" : nombreCompleto.trim());

                    fotoBase64Actual = snapshot.child("fotoPerfil").getValue(String.class);
                    if (fotoBase64Actual != null && !fotoBase64Actual.isEmpty()) {
                        cargarFotoDesdeBase64(fotoBase64Actual, ivFotoPerfil);
                    } else if (currentUser.getPhotoUrl() != null) {
                        cargarFotoGoogle(currentUser.getPhotoUrl().toString(), ivFotoPerfil);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        btnEditarPerfil.setOnClickListener(v -> mostrarDialogoEditar());
        btnAjustes.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AjustesActivity.class);
            startActivity(intent);
        });
        btnEliminarCuenta.setOnClickListener(v -> mostrarDialogoEliminarCuenta());
    }

    // Configura la pantalla para un invitado (sin cuenta)
    private void configurarModoInvitado() {
        tvNombre.setText("Modo Invitado");
        tvCorreo.setText("Inicia sesión para guardar tus datos");
        ivFotoPerfil.setImageResource(R.drawable.profile);
        btnCerrarSesion.setText("Iniciar Sesión / Registrarse");
        btnCerrarSesion.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(getContext(), R.color.color_acento_primario)));

        // Deshabilitamos opciones de edición para invitados
        btnEditarPerfil.setEnabled(false);
        btnEditarPerfil.setAlpha(0.5f);
        btnAjustes.setEnabled(false);
        btnAjustes.setAlpha(0.5f);
        btnEliminarCuenta.setVisibility(View.GONE);
    }

    // Muestra la ventana flotante para editar nombre, apellidos y foto
    private void mostrarDialogoEditar() {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_editar_cuenta, null);
        builder.setView(dialogView);

        ivFotoDialogoTemporal = dialogView.findViewById(R.id.ivDialogFoto);
        EditText etNombre = dialogView.findViewById(R.id.etDialogNombre);
        EditText etApellidos = dialogView.findViewById(R.id.etDialogApellidos);

        etNombre.setText(nombreActual != null ? nombreActual : "");
        etApellidos.setText(apellidosActuales != null ? apellidosActuales : "");
        fotoBase64Temporal = (fotoBase64Actual != null) ? fotoBase64Actual : "";

        if (!fotoBase64Temporal.isEmpty()) {
            cargarFotoDesdeBase64(fotoBase64Temporal, ivFotoDialogoTemporal);
        } else if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getPhotoUrl() != null) {
            cargarFotoGoogle(mAuth.getCurrentUser().getPhotoUrl().toString(), ivFotoDialogoTemporal);
        }

        dialogView.findViewById(R.id.cardDialogFoto).setOnClickListener(v -> {
            photoPickerLauncher.launch(new androidx.activity.result.PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        builder.setPositiveButton("Guardar", null);
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Control manual del botón Guardar para validar antes de cerrar
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nuevoNombre = etNombre.getText().toString().trim();
            String nuevosApellidos = etApellidos.getText().toString().trim();

            if (nuevoNombre.isEmpty()) {
                etNombre.setError("El nombre es obligatorio");
                etNombre.requestFocus();
            } else {
                Map<String, Object> actualizaciones = new HashMap<>();
                actualizaciones.put("nombre", nuevoNombre);
                actualizaciones.put("apellidos", nuevosApellidos);

                if (fotoBase64Temporal != null && !fotoBase64Temporal.isEmpty()) {
                    actualizaciones.put("fotoPerfil", fotoBase64Temporal);
                }

                userRef.updateChildren(actualizaciones).addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Cuenta actualizada", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            }
        });
    }

    // Comprime la imagen seleccionada y la convierte en texto Base64
    private void procesarFotoTemporal(android.net.Uri uri) {
        if (getContext() == null) return;
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            Bitmap bitmapOriginal = BitmapFactory.decodeStream(inputStream);

            // Redimensionado básico para no saturar la base de datos
            int maxResolucion = 400;
            int ancho = bitmapOriginal.getWidth();
            int alto = bitmapOriginal.getHeight();
            float ratio = (float) ancho / alto;
            if (ancho > alto) { ancho = maxResolucion; alto = (int) (ancho / ratio); }
            else { alto = maxResolucion; ancho = (int) (alto * ratio); }
            Bitmap bitmapReducido = Bitmap.createScaledBitmap(bitmapOriginal, ancho, alto, true);

            if (ivFotoDialogoTemporal != null) {
                ivFotoDialogoTemporal.setImageBitmap(bitmapReducido);
                ivFotoDialogoTemporal.setPadding(0, 0, 0, 0);
                ivFotoDialogoTemporal.setImageTintList(null);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmapReducido.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            fotoBase64Temporal = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
        }
    }

    // Decodifica Base64 y lo pone en un ImageView
    private void cargarFotoDesdeBase64(String base64, ImageView imageView) {
        try {
            byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            imageView.setImageBitmap(bitmap);
            imageView.setPadding(0, 0, 0, 0);
            imageView.setImageTintList(null);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Descarga la foto de perfil de Google en un hilo secundario
    private void cargarFotoGoogle(String urlImagen, ImageView targetImageView) {
        new Thread(() -> {
            try {
                InputStream in = new java.net.URL(urlImagen).openStream();
                Bitmap foto = BitmapFactory.decodeStream(in);
                if (getActivity() != null && targetImageView != null) {
                    getActivity().runOnUiThread(() -> {
                        targetImageView.setImageBitmap(foto);
                        targetImageView.setPadding(0,0,0,0);
                        targetImageView.setImageTintList(null);
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    // Gestiona el cierre de sesión de Firebase e Invitados
    private void cerrarSesion() {
        if (mAuth.getCurrentUser() != null) {
            Toast.makeText(getContext(), "Cerrando sesión...", Toast.LENGTH_SHORT).show();
            mAuth.signOut();
            limpiarCredencialesYSalir();
        } else {
            irALogin();
        }
    }

    // Finaliza la actividad y vuelve a la pantalla de Login
    private void irALogin() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }

    // Borra todo el rastro del usuario en la nube
    private void ejecutarBorradoEnCascada() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        Toast.makeText(getContext(), "Eliminando datos...", Toast.LENGTH_LONG).show();

        DatabaseReference mascotasRef = FirebaseDatabase.getInstance().getReference().child("mascotas").child(uid);
        mascotasRef.removeValue().addOnCompleteListener(taskMascotas -> {
            userRef.removeValue().addOnCompleteListener(taskPerfil -> {
                currentUser.delete().addOnCompleteListener(taskAuth -> {
                    if (taskAuth.isSuccessful()) {
                        mAuth.signOut();
                        limpiarCredencialesYSalir();
                    } else {
                        // Error común: requiere re-login reciente para borrar cuenta
                        Toast.makeText(getContext(), "Acción sensible: vuelve a iniciar sesión para borrar la cuenta.", Toast.LENGTH_LONG).show();
                    }
                });
            });
        });
    }

    // Muestra aviso antes de borrar la cuenta
    private void mostrarDialogoEliminarCuenta() {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("⚠️ Eliminar definitivamente")
                .setMessage("¿Estás seguro? Se borrarán todos tus datos y mascotas.")
                .setPositiveButton("Eliminar todo", (dialog, which) -> ejecutarBorradoEnCascada())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Limpia el rastro de Google/Credenciales en el teléfono
    private void limpiarCredencialesYSalir() {
        if (getContext() != null) {
            CredentialManager credentialManager = CredentialManager.create(getContext());
            credentialManager.clearCredentialStateAsync(
                    new ClearCredentialStateRequest(), new android.os.CancellationSignal(),
                    ContextCompat.getMainExecutor(getContext()),
                    new CredentialManagerCallback<Void, ClearCredentialException>() {
                        @Override public void onResult(Void result) { irALogin(); }
                        @Override public void onError(ClearCredentialException e) { irALogin(); }
                    }
            );
        } else { irALogin(); }
    }
}