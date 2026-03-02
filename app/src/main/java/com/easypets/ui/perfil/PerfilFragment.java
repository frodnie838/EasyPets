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
import android.widget.LinearLayout;
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
    private LinearLayout layoutRegistrado, layoutInvitado;
    private ImageView ivFotoPerfil;
    private TextView tvNombre, tvCorreo, tvNick;
    private MaterialButton btnCerrarSesion, btnEditarPerfil, btnAjustes;

    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    private String nombreActual = "";
    private String apellidosActuales = "";
    private String nickActual = "";
    private String fotoBase64Actual = "";
    private String fotoBase64Temporal = "";
    private ImageView ivFotoDialogoTemporal;

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
        tvNick = view.findViewById(R.id.tvNickPerfilUsuario);
        tvCorreo = view.findViewById(R.id.tvCorreoPerfilUsuario);
        btnCerrarSesion = view.findViewById(R.id.btnCerrarSesion);
        btnEditarPerfil = view.findViewById(R.id.btnEditarPerfil);
        btnAjustes = view.findViewById(R.id.btnAjustes);

        layoutRegistrado = view.findViewById(R.id.layoutUsuarioRegistrado);
        layoutInvitado = view.findViewById(R.id.layoutModoInvitado);
        MaterialButton btnIrALogin = view.findViewById(R.id.btnIrALogin);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            layoutRegistrado.setVisibility(View.VISIBLE);
            layoutInvitado.setVisibility(View.GONE);
            configurarPerfilUsuario(currentUser);
        } else {
            layoutRegistrado.setVisibility(View.GONE);
            layoutInvitado.setVisibility(View.VISIBLE);
            btnIrALogin.setOnClickListener(v -> irALogin());
        }

        ivFotoPerfil.setOnClickListener(v -> {
            if (ivFotoPerfil.getDrawable() != null) {
                mostrarFotoGrande();
            }
        });
        btnCerrarSesion.setOnClickListener(v -> cerrarSesion());
    }

    private void configurarPerfilUsuario(FirebaseUser currentUser) {
        tvCorreo.setText(currentUser.getEmail());

        userRef = FirebaseDatabase.getInstance().getReference().child("usuarios").child(currentUser.getUid());

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    nombreActual = snapshot.child("nombre").getValue(String.class);
                    apellidosActuales = snapshot.child("apellidos").getValue(String.class);
                    nickActual = snapshot.child("nick").getValue(String.class); // ✨ Leemos el Nick

                    String nombreCompleto = (nombreActual != null ? nombreActual : "") + " " + (apellidosActuales != null ? apellidosActuales : "");
                    tvNombre.setText(nombreCompleto.trim().isEmpty() ? "Usuario EasyPets" : nombreCompleto.trim());

                    // ✨ Mostramos el Nick
                    if (nickActual != null && !nickActual.trim().isEmpty()) {
                        tvNick.setText("@" + nickActual);
                        tvNick.setVisibility(View.VISIBLE);
                    } else {
                        tvNick.setText("@usuario_sin_nick");
                        tvNick.setVisibility(View.VISIBLE);
                    }

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
            Intent intent = new Intent(getActivity(), com.easypets.ui.perfil.AjustesActivity.class);
            startActivity(intent);
        });
    }

    private void mostrarDialogoEditar() {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_editar_cuenta, null);
        builder.setView(dialogView);

        ivFotoDialogoTemporal = dialogView.findViewById(R.id.ivDialogFoto);
        EditText etNombre = dialogView.findViewById(R.id.etDialogNombre);
        EditText etApellidos = dialogView.findViewById(R.id.etDialogApellidos);
        EditText etNick = dialogView.findViewById(R.id.etDialogNick);

        etNombre.setText(nombreActual != null ? nombreActual : "");
        etApellidos.setText(apellidosActuales != null ? apellidosActuales : "");
        etNick.setText(nickActual != null ? nickActual : "");

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

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nuevoNombre = etNombre.getText().toString().trim();
            String nuevosApellidos = etApellidos.getText().toString().trim();

            // Limpiamos el nick: quitamos espacios y arrobas accidentales
            String nuevoNick = etNick.getText().toString().trim()
                    .replace("@", "")
                    .replace(" ", "_");

            if (nuevoNombre.isEmpty()) {
                etNombre.setError("El nombre es obligatorio");
                etNombre.requestFocus();
                return;
            }

            if (nuevoNick.isEmpty()) {
                etNick.setError("El nick es obligatorio");
                etNick.requestFocus();
                return;
            }

            // Bloqueamos el botón mientras Firebase comprueba para evitar doble clic
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

            // Si el nick es el mismo que ya tenía, guardamos directamente
            if (nuevoNick.equals(nickActual)) {
                guardarCambiosPerfil(nuevoNombre, nuevosApellidos, nuevoNick, dialog);
            } else {
                // Comprobamos si el nick ya está pillado por OTRO usuario
                DatabaseReference usuariosRef = FirebaseDatabase.getInstance().getReference("usuarios");
                usuariosRef.orderByChild("nick").equalTo(nuevoNick).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // ¡Vaya! El nick ya existe
                            etNick.setError("Este nick ya está en uso. Elige otro.");
                            etNick.requestFocus();
                            // Volvemos a activar el botón para que pruebe otro
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        } else {
                            // El nick está libre, ¡guardamos!
                            guardarCambiosPerfil(nuevoNombre, nuevosApellidos, nuevoNick, dialog);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Error al comprobar el nick", Toast.LENGTH_SHORT).show();
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    }
                });
            }
        });
    }

    private void procesarFotoTemporal(android.net.Uri uri) {
        if (getContext() == null) return;
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            Bitmap bitmapOriginal = BitmapFactory.decodeStream(inputStream);

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

    private void cargarFotoDesdeBase64(String base64, ImageView imageView) {
        try {
            byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            imageView.setImageBitmap(bitmap);
            imageView.setPadding(0, 0, 0, 0);
            imageView.setImageTintList(null);
        } catch (Exception e) { e.printStackTrace(); }
    }

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

    private void cerrarSesion() {
        if (mAuth.getCurrentUser() != null) {
            Toast.makeText(getContext(), "Cerrando sesión...", Toast.LENGTH_SHORT).show();
            mAuth.signOut();
            limpiarCredencialesYSalir();
        } else {
            irALogin();
        }
    }

    private void irALogin() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }

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

    private void mostrarFotoGrande() {
        if (getContext() == null) return;

        android.app.Dialog dialog = new android.app.Dialog(getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_ver_foto);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        ImageView ivGrande = dialog.findViewById(R.id.ivFotoGrande);
        android.widget.ImageButton btnCerrar = dialog.findViewById(R.id.btnCerrarFoto);

        ivGrande.setImageDrawable(ivFotoPerfil.getDrawable());

        btnCerrar.setOnClickListener(v -> dialog.dismiss());
        ivGrande.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
    private void guardarCambiosPerfil(String nombre, String apellidos, String nick, AlertDialog dialog) {
        Map<String, Object> actualizaciones = new HashMap<>();
        actualizaciones.put("nombre", nombre);
        actualizaciones.put("apellidos", apellidos);
        actualizaciones.put("nick", nick);

        if (fotoBase64Temporal != null && !fotoBase64Temporal.isEmpty()) {
            actualizaciones.put("fotoPerfil", fotoBase64Temporal);
        }

        userRef.updateChildren(actualizaciones).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Cuenta actualizada", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Error al guardar", Toast.LENGTH_SHORT).show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
        });
    }
}