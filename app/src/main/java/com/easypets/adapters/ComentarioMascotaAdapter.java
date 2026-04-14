package com.easypets.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.easypets.R;
import com.easypets.models.ComentarioMascota;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComentarioMascotaAdapter extends RecyclerView.Adapter<ComentarioMascotaAdapter.ViewHolder> {

    private List<ComentarioMascota> listaComentarios;
    private OnComentarioLongClickListener listener;

    public interface OnComentarioLongClickListener {
        void onLongClick(ComentarioMascota comentario);
    }

    public ComentarioMascotaAdapter(List<ComentarioMascota> listaComentarios) {
        this.listaComentarios = listaComentarios;
    }

    public void setOnComentarioLongClickListener(OnComentarioLongClickListener listener) {
        this.listener = listener;
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
        SpannableString spannable = new SpannableString(texto);
        Pattern pattern = Pattern.compile("@\\w+");
        Matcher matcher = pattern.matcher(texto);

        int colorPrimario = ContextCompat.getColor(holder.itemView.getContext(), R.color.color_acento_primario);

        while (matcher.find()) {
            spannable.setSpan(new ForegroundColorSpan(colorPrimario), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        holder.tvTexto.setText(spannable);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH:mm", Locale.getDefault());
        String fechaStr = sdf.format(new Date(comentario.getTimestamp()));
        holder.tvFecha.setText(fechaStr);

        String foto = comentario.getAutorFoto();
        if (foto != null && !foto.isEmpty()) {
            if (foto.startsWith("http")) {
                Glide.with(holder.itemView.getContext())
                        .load(foto)
                        .circleCrop()
                        .into(holder.ivPerfil);
                holder.ivPerfil.setPadding(0, 0, 0, 0);
            } else {
                try {
                    byte[] decodedString = Base64.decode(foto, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    holder.ivPerfil.setImageBitmap(bitmap);
                    holder.ivPerfil.setPadding(0, 0, 0, 0);
                } catch (Exception e) {
                    holder.ivPerfil.setImageResource(R.drawable.profile);
                }
            }
        } else {
            holder.ivPerfil.setImageResource(R.drawable.profile);
        }

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onLongClick(comentario);
            }
            return true;
        });
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