package com.easypets.repositories;

import androidx.annotation.NonNull;

import com.easypets.models.Evento;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class EventoRepository {

    private final DatabaseReference mDatabase;

    public EventoRepository() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    // --- INTERFACES (Para comunicarse con CalendarioFragment) ---
    public interface AccionCallback {
        void onExito();
        void onError(String error);
    }

    public interface LeerEventosCallback {
        void onResultado(List<Evento> listaEventos);
        void onError(String error);
    }

    // --- 1. GUARDAR UN EVENTO ---
    public void guardarEvento(String uid, Evento evento, AccionCallback callback) {
        // Generamos el ID único en el nodo "eventos" -> "uid_del_usuario"
        String idEvento = mDatabase.child("eventos").child(uid).push().getKey();

        if (idEvento != null) {
            evento.setId(idEvento); // Le asignamos el ID generado

            // Guardamos el objeto Evento en la base de datos
            mDatabase.child("eventos").child(uid).child(idEvento).setValue(evento)
                    .addOnSuccessListener(aVoid -> callback.onExito())
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
        } else {
            callback.onError("No se pudo generar un ID para el evento");
        }
    }

    // --- 2. OBTENER EVENTOS POR FECHA ESPECÍFICA ---
    public void obtenerEventosPorFecha(String uid, String fecha, LeerEventosCallback callback) {
        // Buscamos en los eventos del usuario aquellos cuya "fecha" coincida con la que le pasamos
        mDatabase.child("eventos").child(uid).orderByChild("fecha").equalTo(fecha)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Evento> listaTemporal = new ArrayList<>();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Evento e = data.getValue(Evento.class);
                            if (e != null) {
                                listaTemporal.add(e);
                            }
                        }
                        callback.onResultado(listaTemporal); // Devolvemos la lista de eventos de ese día
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }

    // --- OBTENER TODOS LOS EVENTOS ---
    public void obtenerTodosLosEventos(String uid, LeerEventosCallback callback) {
        mDatabase.child("eventos").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Evento> listaTemporal = new ArrayList<>();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Evento e = data.getValue(Evento.class);
                            if (e != null) {
                                listaTemporal.add(e);
                            }
                        }
                        callback.onResultado(listaTemporal);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }
}