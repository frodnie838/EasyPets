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

    public interface AccionCallback {
        void onExito();
        void onError(String error);
    }

    public interface LeerEventosCallback {
        void onResultado(List<Evento> listaEventos);
        void onError(String error);
    }

    public void guardarEvento(String uid, Evento evento, AccionCallback callback) {
        String idEvento;

        if (evento.getId() != null && !evento.getId().isEmpty()) {
            idEvento = evento.getId();
        } else {
            idEvento = mDatabase.child("eventos").child(uid).push().getKey();
        }

        if (idEvento != null) {
            evento.setId(idEvento);
            mDatabase.child("eventos").child(uid).child(idEvento).setValue(evento)
                    .addOnSuccessListener(aVoid -> callback.onExito())
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
        } else {
            callback.onError("Error al generar ID");
        }
    }

    public void obtenerEventosPorFecha(String uid, String fecha, LeerEventosCallback callback) {
        mDatabase.child("eventos").child(uid).orderByChild("fecha").equalTo(fecha)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Evento> listaTemporal = new ArrayList<>();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Evento e = data.getValue(Evento.class);
                            if (e != null) {
                                e.setId(data.getKey()); // ✨ VITAL: Asignar el ID de Firebase al objeto
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

    public void obtenerTodosLosEventos(String uid, LeerEventosCallback callback) {
        mDatabase.child("eventos").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Evento> listaTemporal = new ArrayList<>();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Evento e = data.getValue(Evento.class);
                            if (e != null) {
                                e.setId(data.getKey()); // ✨ VITAL: Asignar el ID de Firebase al objeto
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

    public void eliminarEvento(String userId, String eventoId, AccionCallback callback) {
        mDatabase.child("eventos").child(userId).child(eventoId).removeValue()
                .addOnSuccessListener(aVoid -> callback.onExito())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}