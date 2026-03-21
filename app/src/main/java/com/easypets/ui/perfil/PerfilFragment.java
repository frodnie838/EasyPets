package com.easypets.ui.perfil;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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
    private Uri uriImagenPerfil = null; // ✨ NUEVO: Variable para Storage
    private ImageView ivFotoDialogoTemporal;

    private final ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest> photoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    uriImagenPerfil = uri;
                    if (ivFotoDialogoTemporal != null) {
                        com.bumptech.glide.Glide.with(requireContext()).load(uri).into(ivFotoDialogoTemporal);
                        ivFotoDialogoTemporal.setPadding(0, 0, 0, 0);
                        ivFotoDialogoTemporal.setImageTintList(null);
                    }
                    fotoBase64Temporal = ""; // Limpiamos el texto antiguo
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
            if (ivFotoPerfil.getDrawable() != null) mostrarFotoGrande();
        });
        btnCerrarSesion.setOnClickListener(v -> cerrarSesion());
    }

    private void configurarPerfilUsuario(FirebaseUser currentUser) {
        tvCorreo.setText(currentUser.getEmail());
        userRef = FirebaseDatabase.getInstance().getReference().child("usuarios").child(currentUser.getUid());

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && isAdded()) {
                    nombreActual = snapshot.child("nombre").getValue(String.class);
                    apellidosActuales = snapshot.child("apellidos").getValue(String.class);
                    nickActual = snapshot.child("nick").getValue(String.class);

                    String nombreCompleto = (nombreActual != null ? nombreActual : "") + " " + (apellidosActuales != null ? apellidosActuales : "");
                    tvNombre.setText(nombreCompleto.trim().isEmpty() ? "Usuario EasyPets" : nombreCompleto.trim());

                    if (nickActual != null && !nickActual.trim().isEmpty()) {
                        tvNick.setText("@" + nickActual);
                        tvNick.setVisibility(View.VISIBLE);
                    } else {
                        tvNick.setText("@usuario");
                        tvNick.setVisibility(View.VISIBLE);
                    }

                    fotoBase64Actual = snapshot.child("fotoPerfil").getValue(String.class);

                    if (fotoBase64Actual != null && !fotoBase64Actual.isEmpty()) {
                        if (fotoBase64Actual.startsWith("http")) {
                            com.bumptech.glide.Glide.with(requireContext()).load(fotoBase64Actual).into(ivFotoPerfil);
                            ivFotoPerfil.setPadding(0, 0, 0, 0);
                            ivFotoPerfil.setImageTintList(null);
                        } else {
                            cargarFotoDesdeBase64(fotoBase64Actual, ivFotoPerfil);
                        }
                    } else if (currentUser.getPhotoUrl() != null) {
                        com.bumptech.glide.Glide.with(requireContext()).load(currentUser.getPhotoUrl()).into(ivFotoPerfil);
                        ivFotoPerfil.setPadding(0, 0, 0, 0);
                        ivFotoPerfil.setImageTintList(null);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        btnEditarPerfil.setOnClickListener(v -> mostrarDialogoEditar());
        btnAjustes.setOnClickListener(v -> startActivity(new Intent(getActivity(), AjustesActivity.class)));
    }

    private void mostrarDialogoEditar() {
        if (getContext() == null) return;

        uriImagenPerfil = null; // Reiniciar
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
            if (fotoBase64Temporal.startsWith("http")) {
                com.bumptech.glide.Glide.with(requireContext()).load(fotoBase64Temporal).into(ivFotoDialogoTemporal);
                ivFotoDialogoTemporal.setPadding(0, 0, 0, 0);
                ivFotoDialogoTemporal.setImageTintList(null);
            } else {
                cargarFotoDesdeBase64(fotoBase64Temporal, ivFotoDialogoTemporal);
            }
        } else if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getPhotoUrl() != null) {
            com.bumptech.glide.Glide.with(requireContext()).load(mAuth.getCurrentUser().getPhotoUrl()).into(ivFotoDialogoTemporal);
            ivFotoDialogoTemporal.setPadding(0, 0, 0, 0);
            ivFotoDialogoTemporal.setImageTintList(null);
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
            String nuevoNick = etNick.getText().toString().trim().replace("@", "").replace(" ", "_");

            if (nuevoNombre.isEmpty()) { etNombre.setError("El nombre es obligatorio"); return; }
            if (nuevoNick.isEmpty()) { etNick.setError("El nick es obligatorio"); return; }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Guardando...");

            if (nuevoNick.equals(nickActual)) {
                subirFotoYGuardarPerfil(nuevoNombre, nuevosApellidos, nuevoNick, dialog);
            } else {
                DatabaseReference usuariosRef = FirebaseDatabase.getInstance().getReference("usuarios");
                usuariosRef.orderByChild("nick").equalTo(nuevoNick).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            etNick.setError("Este nick ya está en uso.");
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Guardar");
                        } else {
                            subirFotoYGuardarPerfil(nuevoNombre, nuevosApellidos, nuevoNick, dialog);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    }
                });
            }
        });
    }

    private void subirFotoYGuardarPerfil(String nombre, String apellidos, String nick, AlertDialog dialog) {
        if (uriImagenPerfil != null && mAuth.getCurrentUser() != null) {
            StorageReference ref = FirebaseStorage.getInstance().getReference("perfiles").child(mAuth.getCurrentUser().getUid() + ".jpg");
            ref.putFile(uriImagenPerfil).addOnSuccessListener(taskSnapshot -> {
                ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    fotoBase64Temporal = uri.toString();
                    guardarCambiosPerfilDB(nombre, apellidos, nick, dialog);
                });
            }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Error subiendo foto", Toast.LENGTH_SHORT).show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Guardar");
            });
        } else {
            guardarCambiosPerfilDB(nombre, apellidos, nick, dialog);
        }
    }

    private void guardarCambiosPerfilDB(String nombre, String apellidos, String nick, AlertDialog dialog) {
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

    private void cargarFotoDesdeBase64(String base64, ImageView imageView) {
        try {
            byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            imageView.setImageBitmap(bitmap);
            imageView.setPadding(0, 0, 0, 0);
            imageView.setImageTintList(null);
        } catch (Exception e) { e.printStackTrace(); }
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
}