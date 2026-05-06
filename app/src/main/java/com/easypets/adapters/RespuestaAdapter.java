package com.easypets.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.easypets.R;
import com.easypets.models.RespuestaForo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.List;

/**
 * Adaptador para el RecyclerView encargado de renderizar el hilo de respuestas de un debate.
 * Gestiona el formato dinámico de texto (resaltado de menciones vía Regex), los estados
 * visuales condicionales (mensajes editados o eliminados, aplicando Soft Delete visual)
 * y la recuperación asíncrona de la identidad visual del autor mediante Firebase Realtime Database.
 */
public class RespuestaAdapter extends RecyclerView.Adapter<RespuestaAdapter.RespuestaViewHolder> {

    private List<RespuestaForo> respuestas;
    private OnRespuestaAccionListener listener;
    private String currentUserId;

    /**
     * Interfaz de comunicación delegada que permite a la vista contenedora (Fragment/Activity)
     * interceptar e implementar la lógica de negocio para la edición y eliminación de respuestas.
     */
    public interface OnRespuestaAccionListener {
        void onBorrarClick(RespuestaForo respuesta);
        void onEditarClick(RespuestaForo respuesta);
    }

    public RespuestaAdapter(List<RespuestaForo> respuestas, OnRespuestaAccionListener listener) {
        this.respuestas = respuestas;
        this.listener = listener;
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    @NonNull @Override
    public RespuestaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_respuesta, parent, false);
        return new RespuestaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RespuestaViewHolder holder, int position) {
        RespuestaForo respuesta = respuestas.get(position);

        String texto = respuesta.getTexto();
        android.text.SpannableString spannable = new android.text.SpannableString(texto);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@\\w+");
        java.util.regex.Matcher matcher = pattern.matcher(texto);

        int colorPrimario = androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.color_acento_primario);

        while (matcher.find()) {
            spannable.setSpan(new android.text.style.ForegroundColorSpan(colorPrimario), matcher.start(), matcher.end(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), matcher.start(), matcher.end(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        holder.tvTexto.setText(spannable);
        CharSequence tiempo = android.text.format.DateUtils.getRelativeTimeSpanString(
                respuesta.getTimestampCreacion(), System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS);
        holder.tvFecha.setText(" • " + tiempo);

        if (respuesta.isEliminado()) {
            holder.tvTexto.setTypeface(null, android.graphics.Typeface.ITALIC);
            holder.tvTexto.setTextColor(android.graphics.Color.parseColor("#9E9E9E"));
            holder.tvAutor.setText("Mensaje eliminado");
            holder.tvEditado.setVisibility(View.GONE);
            holder.btnMenu.setVisibility(View.GONE);

            holder.ivAvatar.setImageResource(R.drawable.profile);
            holder.ivAvatar.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E0E0E0")));
            return;
        } else {
            holder.tvTexto.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.tvTexto.setTextColor(android.graphics.Color.BLACK);
            holder.tvEditado.setVisibility(respuesta.isEditado() ? View.VISIBLE : View.GONE);
        }

        holder.tvAutor.setText("Cargando...");
        holder.ivAvatar.setImageResource(R.drawable.profile);
        holder.ivAvatar.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#9E9E9E")));

        if (currentUserId != null && respuesta.getIdAutor().equals(currentUserId)) {
            holder.btnMenu.setVisibility(View.VISIBLE);
            holder.btnMenu.setOnClickListener(v -> {
                android.widget.PopupMenu popup = new android.widget.PopupMenu(v.getContext(), v);
                popup.getMenu().add("Editar");
                popup.getMenu().add("Eliminar");

                popup.setOnMenuItemClickListener(item -> {
                    if (item.getTitle().equals("Editar")) {
                        listener.onEditarClick(respuesta);
                    } else if (item.getTitle().equals("Eliminar")) {
                        listener.onBorrarClick(respuesta);
                    }
                    return true;
                });
                popup.show();
            });
        } else {
            holder.btnMenu.setVisibility(View.GONE);
        }

        FirebaseDatabase.getInstance().getReference("usuarios").child(respuesta.getIdAutor())
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
                                    com.bumptech.glide.Glide.with(holder.itemView.getContext())
                                            .load(foto)
                                            .circleCrop()
                                            .into(holder.ivAvatar);
                                    holder.ivAvatar.setPadding(0, 0, 0, 0);
                                    holder.ivAvatar.setImageTintList(null);
                                } else {
                                    try {
                                        byte[] decodedString = Base64.decode(foto, Base64.DEFAULT);
                                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                        holder.ivAvatar.setImageBitmap(bitmap);
                                        holder.ivAvatar.setPadding(0, 0, 0, 0);
                                        holder.ivAvatar.setImageTintList(null);
                                    } catch (Exception e) {
                                        holder.ivAvatar.setImageResource(R.drawable.profile);
                                    }
                                }
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    @Override public int getItemCount() { return respuestas.size(); }

    public static class RespuestaViewHolder extends RecyclerView.ViewHolder {
        TextView tvAutor, tvFecha, tvTexto, tvEditado;
        ImageView ivAvatar;
        ImageButton btnMenu;

        public RespuestaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAutor = itemView.findViewById(R.id.tvAutorRespuesta);
            tvFecha = itemView.findViewById(R.id.tvFechaRespuesta);
            tvTexto = itemView.findViewById(R.id.tvTextoRespuesta);
            tvEditado = itemView.findViewById(R.id.tvEditado);
            ivAvatar = itemView.findViewById(R.id.ivAvatarRespuesta);
            btnMenu = itemView.findViewById(R.id.btnMenuRespuesta);
        }
    }
}