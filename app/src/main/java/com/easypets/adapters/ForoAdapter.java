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
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.List;

public class ForoAdapter extends RecyclerView.Adapter<ForoAdapter.ForoViewHolder> {
    private List<HiloForo> hilos;
    private OnHiloAccionListener listener;
    private boolean mostrarOpciones = false; // ✨ Interruptor apagado por defecto

    public interface OnHiloAccionListener {
        void onHiloClick(HiloForo hilo);
        void onEditarClick(HiloForo hilo);
        void onBorrarClick(HiloForo hilo);
    }

    public ForoAdapter(List<HiloForo> hilos, OnHiloAccionListener listener) {
        this.hilos = hilos;
        this.listener = listener;
    }

    // ✨ MÉTODO para encender/apagar los puntos desde el Fragmento
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

        // ✨ AQUÍ ESTÁ EL CAMBIO: Comprobamos 'mostrarOpciones' primero
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

        // Carga visual del autor (Foto y nombre)
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

        holder.itemView.setOnClickListener(v -> listener.onHiloClick(hilo));
    }

    @Override public int getItemCount() { return hilos.size(); }

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
        TextView tvTitulo, tvDescripcion, tvAutor, tvFecha;
        ImageView ivAvatar;
        ImageButton btnMenu; // ✨ Nuevo botón

        public ForoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTituloHilo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionHilo);
            tvAutor = itemView.findViewById(R.id.tvAutorHilo);
            tvFecha = itemView.findViewById(R.id.tvFechaHilo);
            ivAvatar = itemView.findViewById(R.id.ivAvatarHilo);
            btnMenu = itemView.findViewById(R.id.btnMenuHilo); // ✨ Lo vinculamos
        }
    }
}