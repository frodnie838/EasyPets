package com.easypets.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.easypets.R;
import com.easypets.models.ComentarioMascota;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ComentarioMascotaAdapter extends RecyclerView.Adapter<ComentarioMascotaAdapter.ViewHolder> {

    private List<ComentarioMascota> listaComentarios;

    public ComentarioMascotaAdapter(List<ComentarioMascota> listaComentarios) {
        this.listaComentarios = listaComentarios;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comentario_mascota, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ComentarioMascota comentario = listaComentarios.get(position);

        holder.tvNick.setText(comentario.getAutorNick());
        String texto = comentario.getTexto();
        android.text.SpannableString spannable = new android.text.SpannableString(texto);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@\\w+"); // Busca palabras que empiecen por @
        java.util.regex.Matcher matcher = pattern.matcher(texto);

        // Obtenemos el color primario de tu app
        int colorPrimario = androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.color_acento_primario);

        while (matcher.find()) {
            // Ponemos el color y negrita a la mención
            spannable.setSpan(new android.text.style.ForegroundColorSpan(colorPrimario), matcher.start(), matcher.end(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), matcher.start(), matcher.end(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        holder.tvTexto.setText(spannable);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH:mm", Locale.getDefault());
        String fechaStr = sdf.format(new Date(comentario.getTimestamp()));
        holder.tvFecha.setText(fechaStr);

        String foto = comentario.getAutorFoto();
        if (foto != null && !foto.isEmpty()) {
            if (foto.startsWith("http")) {
                // Es un enlace de Google
                new Thread(() -> {
                    try {
                        java.io.InputStream in = new java.net.URL(foto).openStream();
                        android.graphics.Bitmap fotoBitmap = android.graphics.BitmapFactory.decodeStream(in);
                        // Volvemos al hilo principal para pintar la imagen
                        holder.ivPerfil.post(() -> holder.ivPerfil.setImageBitmap(fotoBitmap));
                    } catch (Exception e) { e.printStackTrace(); }
                }).start();
            } else {
                // Es una foto de la galería (Base64)
                try {
                    byte[] decodedString = Base64.decode(foto, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    holder.ivPerfil.setImageBitmap(bitmap);
                } catch (Exception e) { e.printStackTrace(); }
            }
        } else {
            // Si no tiene foto, ponemos la de por defecto
            holder.ivPerfil.setImageResource(R.drawable.profile);
        }
    }

    @Override
    public int getItemCount() {
        return listaComentarios.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNick, tvTexto, tvFecha;
        ImageView ivPerfil;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNick = itemView.findViewById(R.id.tvComentarioNick);
            tvTexto = itemView.findViewById(R.id.tvComentarioTexto);
            tvFecha = itemView.findViewById(R.id.tvComentarioFecha);
            ivPerfil = itemView.findViewById(R.id.ivComentarioPerfil);
        }
    }
}