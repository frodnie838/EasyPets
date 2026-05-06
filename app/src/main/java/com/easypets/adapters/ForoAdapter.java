package com.easypets.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.easypets.R;
import com.easypets.models.HiloForo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.List;

/**
 * Adaptador para el RecyclerView encargado de renderizar la lista de hilos de debate en el foro.
 * Gestiona la visualización de la metainformación del hilo, la consulta asíncrona de los datos
 * del autor (avatar y nickname) desde Firebase Realtime Database, y la interacción bidireccional
 * del sistema de "Me gusta" (Likes).
 */
public class ForoAdapter extends RecyclerView.Adapter<ForoAdapter.ForoViewHolder> {

    private List<HiloForo> hilos;
    private OnHiloAccionListener listener;
    private boolean mostrarOpciones = false;

    /**
     * Interfaz para delegar los eventos de interacción sobre los hilos del foro,
     * permitiendo gestionar la navegación al detalle y la administración (edición/eliminación)
     * por parte del autor.
     */
    public interface OnHiloAccionListener {
        void onHiloClick(HiloForo hilo);
        void onEditarClick(HiloForo hilo);
        void onBorrarClick(HiloForo hilo);
    }

    public ForoAdapter(List<HiloForo> hilos, OnHiloAccionListener listener) {
        this.hilos = hilos;
        this.listener = listener;
    }

    public void setMostrarOpciones(boolean mostrarOpciones) {
        this.mostrarOpciones = mostrarOpciones;
    }

    @NonNull @Override
    public ForoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hilo, parent, false);
        return new ForoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ForoViewHolder holder, int position) {
        HiloForo hilo = hilos.get(position);
        holder.tvTitulo.setText(hilo.getTitulo());

        if (hilo.getDescripcion() != null && !hilo.getDescripcion().isEmpty()) {
            holder.tvDescripcion.setText(hilo.getDescripcion());
            holder.tvDescripcion.setVisibility(View.VISIBLE);
        } else {
            holder.tvDescripcion.setVisibility(View.GONE);
        }

        CharSequence tiempoTranscurrido = android.text.format.DateUtils.getRelativeTimeSpanString(
                hilo.getTimestampCreacion(), System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS);
        holder.tvFecha.setText(" • " + tiempoTranscurrido);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (mostrarOpciones && currentUser != null && hilo.getIdAutor().equals(currentUser.getUid())) {
            holder.btnMenu.setVisibility(View.VISIBLE);
            holder.btnMenu.setOnClickListener(v -> {
                android.widget.PopupMenu popup = new android.widget.PopupMenu(v.getContext(), v);
                popup.getMenu().add("Editar");
                popup.getMenu().add("Eliminar");

                popup.setOnMenuItemClickListener(item -> {
                    if (item.getTitle().equals("Editar")) {
                        listener.onEditarClick(hilo);
                    } else if (item.getTitle().equals("Eliminar")) {
                        listener.onBorrarClick(hilo);
                    }
                    return true;
                });
                popup.show();
            });
        } else {
            holder.btnMenu.setVisibility(View.GONE);
        }

        holder.tvAutor.setText("Cargando...");
        holder.ivAvatar.setImageResource(R.drawable.profile);
        holder.ivAvatar.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#9E9E9E")));
        holder.ivAvatar.setPadding(10, 10, 10, 10);

        FirebaseDatabase.getInstance().getReference("usuarios").child(hilo.getIdAutor())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String nick = snapshot.child("nick").getValue(String.class);
                            if (nick != null && !nick.isEmpty()) {
                                holder.tvAutor.setText("@" + nick);
                            } else {
                                String nombre = snapshot.child("nombre").getValue(String.class);
                                holder.tvAutor.setText(nombre != null ? nombre : "Usuario");
                            }

                            String foto = snapshot.child("fotoPerfil").getValue(String.class);
                            if (foto != null && !foto.isEmpty()) {
                                if (foto.startsWith("http")) {
                                    cargarFotoGoogle(foto, holder.ivAvatar);
                                } else {
                                    try {
                                        byte[] decodedString = Base64.decode(foto, Base64.DEFAULT);
                                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                        holder.ivAvatar.setImageBitmap(bitmap);
                                        holder.ivAvatar.setPadding(0, 0, 0, 0);
                                        holder.ivAvatar.setImageTintList(null);
                                    } catch (Exception e) { e.printStackTrace(); }
                                }
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        String currentUserId = (currentUser != null) ? currentUser.getUid() : "";
        int numeroDeLikes = 0;
        boolean isLikedByMe = false;

        if (hilo.getLikes() != null) {
            numeroDeLikes = hilo.getLikes().size();
            isLikedByMe = hilo.getLikes().containsKey(currentUserId);
        }

        if (numeroDeLikes == 0) {
            holder.tvLikeCount.setVisibility(View.GONE);
        } else {
            holder.tvLikeCount.setVisibility(View.VISIBLE);
            holder.tvLikeCount.setText(String.valueOf(numeroDeLikes));
        }

        if (isLikedByMe) {
            holder.btnLike.setImageResource(R.drawable.ic_heart_filled);
            holder.btnLike.setImageTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.color_acento_primario)));
        } else {
            holder.btnLike.setImageResource(R.drawable.ic_heart_outline);
        }

        boolean finalIsLikedByMe = isLikedByMe;
        holder.btnLike.setOnClickListener(v -> {
            if (currentUserId.isEmpty()) return;

            DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference("foro_hilos")
                    .child(hilo.getId()).child("likes").child(currentUserId);

            if (finalIsLikedByMe) {
                likesRef.removeValue();
            } else {
                likesRef.setValue(true);
            }
        });
        holder.itemView.setOnClickListener(v -> listener.onHiloClick(hilo));
    }

    @Override public int getItemCount() { return hilos.size(); }

    /**
     * Ejecuta la descarga de un recurso gráfico en un hilo secundario y asigna
     * el resultado al componente visual en el hilo principal (UI Thread).
     *
     * @param urlImagen URL remota de la imagen a cargar.
     * @param targetImageView Componente ImageView destino.
     */
    private void cargarFotoGoogle(String urlImagen, ImageView targetImageView) {
        new Thread(() -> {
            try {
                java.io.InputStream in = new java.net.URL(urlImagen).openStream();
                Bitmap foto = BitmapFactory.decodeStream(in);
                if (targetImageView != null) {
                    targetImageView.post(() -> {
                        targetImageView.setImageBitmap(foto);
                        targetImageView.setPadding(0, 0, 0, 0);
                        targetImageView.setImageTintList(null);
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    public static class ForoViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvDescripcion, tvAutor, tvFecha, tvLikeCount;
        ImageView ivAvatar;
        ImageButton btnMenu, btnLike;

        public ForoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTituloHilo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionHilo);
            tvAutor = itemView.findViewById(R.id.tvAutorHilo);
            tvFecha = itemView.findViewById(R.id.tvFechaHilo);
            ivAvatar = itemView.findViewById(R.id.ivAvatarHilo);
            btnMenu = itemView.findViewById(R.id.btnMenuHilo);
            btnLike = itemView.findViewById(R.id.btnLikeHilo);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCountHilo);
        }
    }
}