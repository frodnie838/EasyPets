package com.easypets.ui.comunidad;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.easypets.R;
import com.easypets.adapters.RespuestaAdapter;
import com.easypets.models.RespuestaForo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HiloDetalleFragment extends Fragment {

    private String hiloId, titulo, descripcion, idAutor;
    private long timestampCreacion;
    private ImageView ivAvatarDetalle;
    private TextView tvAutorDetalle, tvFechaDetalle;
    private View layoutEmptyStateRespuestas;
    private RecyclerView rvRespuestas;
    private EditText etNuevaRespuesta;
    private ImageButton btnEnviarRespuesta;

    private RespuestaAdapter adapter;
    private RespuestaForo respuestaAEditar = null;

    private List<RespuestaForo> listaRespuestas;
    private DatabaseReference respuestasRef;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_hilo_detalle, container, false);

        if (getArguments() != null) {
            hiloId = getArguments().getString("hiloId");
            titulo = getArguments().getString("titulo");
            descripcion = getArguments().getString("descripcion");
            idAutor = getArguments().getString("idAutor");
            timestampCreacion = getArguments().getLong("timestamp");
        }

        // Vincular vistas
        Toolbar toolbar = view.findViewById(R.id.toolbarHilo);
        TextView tvTitulo = view.findViewById(R.id.tvDetalleTitulo);
        TextView tvDescripcion = view.findViewById(R.id.tvDetalleDescripcion);
        rvRespuestas = view.findViewById(R.id.rvRespuestas);
        layoutEmptyStateRespuestas = view.findViewById(R.id.layoutEmptyStateRespuestas);
        etNuevaRespuesta = view.findViewById(R.id.etNuevaRespuesta);
        btnEnviarRespuesta = view.findViewById(R.id.btnEnviarRespuesta);
        ivAvatarDetalle = view.findViewById(R.id.ivAvatarDetalle);
        tvAutorDetalle = view.findViewById(R.id.tvAutorDetalle);
        tvFechaDetalle = view.findViewById(R.id.tvFechaDetalle);

        // Configurar cabecera
        tvTitulo.setText(titulo);
        tvDescripcion.setText(descripcion);
        CharSequence tiempo = android.text.format.DateUtils.getRelativeTimeSpanString(
                timestampCreacion, System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS);
        tvFechaDetalle.setText(tiempo);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.color_acento_primario)
            );
        }

        toolbar.setTitle("");

        cargarDatosAutorPrincipal();

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        respuestasRef = FirebaseDatabase.getInstance().getReference("foro_respuestas").child(hiloId);

        if (currentUser == null) {
            etNuevaRespuesta.setEnabled(false); // Bloqueamos escritura
            etNuevaRespuesta.setHint("Inicia sesión para participar"); // Cambiamos el mensaje
            btnEnviarRespuesta.setEnabled(false); // Apagamos el botón
        } else {
            etNuevaRespuesta.setEnabled(true);
            etNuevaRespuesta.setHint("Escribe una respuesta...");
        }

        listaRespuestas = new ArrayList<>();
        adapter = new RespuestaAdapter(listaRespuestas, new RespuestaAdapter.OnRespuestaAccionListener() {
            @Override
            public void onBorrarClick(RespuestaForo respuesta) {
                confirmarBorrado(respuesta);
            }

            @Override
            public void onEditarClick(RespuestaForo respuesta) {
                respuestaAEditar = respuesta;
                etNuevaRespuesta.setText(respuesta.getTexto());
                etNuevaRespuesta.requestFocus();
                btnEnviarRespuesta.setImageResource(android.R.drawable.ic_menu_save);
            }
        });
        rvRespuestas.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRespuestas.setAdapter(adapter);

        cargarRespuestas();

        btnEnviarRespuesta.setOnClickListener(v -> enviarRespuesta());
        actualizarEstadoBoton(false);

        etNuevaRespuesta.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hayTexto = s.toString().trim().length() > 0;
                actualizarEstadoBoton(hayTexto);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        return view;
    }

    private void cargarRespuestas() {
        respuestasRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaRespuestas.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    RespuestaForo respuesta = data.getValue(RespuestaForo.class);
                    if (respuesta != null) {
                        listaRespuestas.add(respuesta);
                    }
                }
                adapter.notifyDataSetChanged();

                if (listaRespuestas.isEmpty()) {
                    layoutEmptyStateRespuestas.setVisibility(View.VISIBLE);
                    rvRespuestas.setVisibility(View.GONE);
                } else {
                    layoutEmptyStateRespuestas.setVisibility(View.GONE);
                    rvRespuestas.setVisibility(View.VISIBLE);
                    rvRespuestas.scrollToPosition(listaRespuestas.size() - 1);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void enviarRespuesta() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Debes iniciar sesión para responder", Toast.LENGTH_SHORT).show();
            return;
        }

        String texto = etNuevaRespuesta.getText().toString().trim();
        if (texto.isEmpty()) return;

        // Deshabilitar botón temporalmente para evitar doble clic
        actualizarEstadoBoton(false);

        if (respuestaAEditar != null) {
            // --- MODO EDICIÓN ---
            respuestaAEditar.setTexto(texto);
            respuestaAEditar.setEditado(true);

            respuestasRef.child(respuestaAEditar.getId()).setValue(respuestaAEditar)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Respuesta actualizada", Toast.LENGTH_SHORT).show();
                        limpiarPostEnvio();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error al actualizar", Toast.LENGTH_SHORT).show();
                        actualizarEstadoBoton(true);
                    });
        } else {
            String idRespuesta = respuestasRef.push().getKey();
            RespuestaForo nuevaRespuesta = new RespuestaForo(idRespuesta, texto,
                    currentUser.getUid(), "", System.currentTimeMillis(), false);

            if (idRespuesta != null) {
                respuestasRef.child(idRespuesta).setValue(nuevaRespuesta)
                        .addOnSuccessListener(aVoid -> {
                            limpiarPostEnvio();
                            // Pasamos el texto a la notificación del dueño
                            enviarNotificacionAlDueño(texto);

                            // Buscar @menciones en el texto y notificar a los mencionados
                            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("@(\\w+)").matcher(texto);
                            while (matcher.find()) {
                                String nickMencionado = matcher.group(1); // Coge la palabra sin el @
                                notificarMencion(nickMencionado);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Error al enviar", Toast.LENGTH_SHORT).show();
                            actualizarEstadoBoton(true);
                        });
            }
        }
    }

    // Método auxiliar para resetear la interfaz después de enviar o editar
    private void limpiarPostEnvio() {
        respuestaAEditar = null;
        etNuevaRespuesta.setText("");
        btnEnviarRespuesta.setImageResource(android.R.drawable.ic_menu_send);
        actualizarEstadoBoton(false);
    }

    private void cargarDatosAutorPrincipal() {
        if (idAutor == null) return;

        FirebaseDatabase.getInstance().getReference("usuarios").child(idAutor)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String nick = snapshot.child("nick").getValue(String.class);
                            if (nick != null && !nick.isEmpty()) {
                                tvAutorDetalle.setText("@" + nick);
                            } else {
                                String nombre = snapshot.child("nombre").getValue(String.class);
                                tvAutorDetalle.setText(nombre != null ? nombre : "Usuario");
                            }

                            String foto = snapshot.child("fotoPerfil").getValue(String.class);
                            if (foto != null && !foto.isEmpty()) {
                                if (foto.startsWith("http")) {
                                    cargarFotoGoogle(foto, ivAvatarDetalle);
                                } else {
                                    try {
                                        byte[] decodedString = android.util.Base64.decode(foto, android.util.Base64.DEFAULT);
                                        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                        ivAvatarDetalle.setImageBitmap(bitmap);
                                        ivAvatarDetalle.setImageTintList(null);
                                    } catch (Exception e) { e.printStackTrace(); }
                                }
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void cargarFotoGoogle(String urlImagen, ImageView targetImageView) {
        new Thread(() -> {
            try {
                java.io.InputStream in = new java.net.URL(urlImagen).openStream();
                android.graphics.Bitmap foto = android.graphics.BitmapFactory.decodeStream(in);
                if (targetImageView != null && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        targetImageView.setImageBitmap(foto);
                        targetImageView.setImageTintList(null);
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void actualizarEstadoBoton(boolean habilitado) {
        btnEnviarRespuesta.setEnabled(habilitado);
        if (habilitado) {
            btnEnviarRespuesta.setColorFilter(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.color_acento_primario));
        } else {
            btnEnviarRespuesta.setColorFilter(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.grey));
        }
    }

    private void confirmarBorrado(RespuestaForo respuesta) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Eliminar respuesta")
                .setMessage("¿Estás seguro de que quieres eliminar este mensaje? Quedará marcado como eliminado para los demás usuarios.")
                .setPositiveButton("Eliminar", (dialog, which) -> {

                    java.util.Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put("eliminado", true);
                    updates.put("texto", "🚫 Este mensaje ha sido eliminado por el autor.");
                    updates.put("idAutor", "deleted");
                    updates.put("editado", false);

                    respuestasRef.child(respuesta.getId()).updateChildren(updates)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Mensaje eliminado", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // RECIBE EL TEXTO DEL COMENTARIO PARA RECORTARLO
    private void enviarNotificacionAlDueño(String textoComentario) {
        if (idAutor == null || currentUser == null) return;

        // No enviamos notificación si nos respondemos a nosotros mismos
        if (idAutor.equals(currentUser.getUid())) return;

        DatabaseReference buzonRef = FirebaseDatabase.getInstance().getReference()
                .child("notificaciones")
                .child(idAutor);

        String idNotificacion = buzonRef.push().getKey();

        // Recortamos el comentario a un máximo de 50 caracteres para la notificación
        String fragmento = textoComentario;
        if (fragmento.length() > 50) {
            fragmento = fragmento.substring(0, 50) + "...";
        }
        final String fragmentoFinal = fragmento; // Variable final para usar dentro del EventListener

        // Buscamos nuestro propio nombre para enviarlo en la notificación
        FirebaseDatabase.getInstance().getReference("usuarios").child(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String miNombre = "Alguien";
                        if (snapshot.exists()) {
                            String nick = snapshot.child("nick").getValue(String.class);
                            if (nick != null && !nick.isEmpty()) {
                                miNombre = "@" + nick;
                            } else {
                                String nombre = snapshot.child("nombre").getValue(String.class);
                                if (nombre != null) miNombre = nombre;
                            }
                        }

                        // Creamos la "carta" con los datos dinámicos actualizados
                        java.util.HashMap<String, Object> carta = new java.util.HashMap<>();
                        carta.put("titulo", miNombre + " ha comentado \uD83D\uDCAC");
                        carta.put("mensaje", "\"" + fragmentoFinal + "\""); // Metemos el texto entre comillas
                        carta.put("mostrada", false);

                        // Añadimos los datos ocultos para la navegación (Deep Linking)
                        carta.put("tipo", "foro_respuesta");
                        carta.put("hiloId", hiloId);
                        carta.put("hiloTitulo", titulo);
                        carta.put("hiloDescripcion", descripcion);
                        carta.put("hiloAutor", idAutor);
                        carta.put("hiloTimestamp", timestampCreacion);

                        if (idNotificacion != null) {
                            buzonRef.child(idNotificacion).setValue(carta);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
    // MÉTODO PARA ENVIAR LA NOTIFICACIÓN DE MENCIÓN EN EL FORO
    private void notificarMencion(String nickMencionado) {
        if (currentUser == null) return;

        // Buscamos en la base de datos qué usuario tiene ese Nick
        FirebaseDatabase.getInstance().getReference("usuarios")
                .orderByChild("nick").equalTo(nickMencionado)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                String idUsuarioMencionado = userSnapshot.getKey();

                                // No te autodenotifiques si te mencionas a ti mismo
                                if (idUsuarioMencionado != null && !idUsuarioMencionado.equals(currentUser.getUid())) {
                                    DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("notificaciones").child(idUsuarioMencionado);
                                    String idNotif = notifRef.push().getKey();

                                    // Buscamos nuestro propio nick para ponerlo en el mensaje
                                    FirebaseDatabase.getInstance().getReference("usuarios").child(currentUser.getUid())
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot miSnapshot) {
                                                    String miNick = "Alguien";
                                                    if (miSnapshot.exists() && miSnapshot.hasChild("nick")) {
                                                        miNick = miSnapshot.child("nick").getValue(String.class);
                                                    }

                                                    com.easypets.models.Notificacion notificacion = new com.easypets.models.Notificacion(
                                                            idNotif,
                                                            "Mención en el foro",
                                                            "@" + miNick + " te ha mencionado en el hilo: " + titulo,
                                                            "foro_respuesta",
                                                            hiloId,
                                                            titulo,
                                                            descripcion,
                                                            idAutor,
                                                            timestampCreacion
                                                    );
                                                    notifRef.child(idNotif).setValue(notificacion);
                                                }
                                                @Override public void onCancelled(@NonNull DatabaseError error) {}
                                            });
                                }
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}