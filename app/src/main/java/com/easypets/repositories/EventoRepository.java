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

/**
 * Repositorio encargado de gestionar las operaciones CRUD de la entidad Evento
 * en Firebase Realtime Database.
 */
public class EventoRepository {

    private final DatabaseReference mDatabase;

    public EventoRepository() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    // =========================================================================
    // INTERFACES CALLBACK
    // =========================================================================

    public interface AccionCallback {
        void onExito();
        void onError(String error);
    }

    public interface LeerEventosCallback {
        void onResultado(List<Evento> listaEventos);
        void onError(String error);
    }

    // =========================================================================
    // MÉTODOS DE OPERACIÓN (CRUD)
    // =========================================================================

    /**
     * Guarda un nuevo evento o actualiza uno existente (Upsert).
     * Si el objeto Evento ya contiene un ID, sobrescribe el nodo. Si no, genera un nuevo identificador.
     *
     * @param uid      Identificador único del usuario.
     * @param evento   Objeto Evento a guardar o actualizar.
     * @param callback Interfaz de respuesta para notificar éxito o error.
     */
    public void guardarEvento(String uid, Evento evento, AccionCallback callback) {
        String idEvento;

        // Comprobamos si es una actualización (ID existente) o una creación (ID nulo)
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
            callback.onError("Error de sistema: No se pudo generar el identificador del evento.");
        }
    }

    /**
     * Recupera una lista de eventos filtrados estrictamente por una fecha específica.
     *
     * @param uid      Identificador único del usuario.
     * @param fecha    Fecha en formato String (ej. "dd/MM/yyyy") a consultar.
     * @param callback Interfaz que retorna la lista de eventos coincidentes.
     */
    public void obtenerEventosPorFecha(String uid, String fecha, LeerEventosCallback callback) {
        mDatabase.child("eventos").child(uid).orderByChild("fecha").equalTo(fecha)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Evento> listaTemporal = new ArrayList<>();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Evento e = data.getValue(Evento.class);
                            if (e != null) {
                                e.setId(data.getKey());
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

    /**
     * Recupera el historial completo de eventos de un usuario.
     * Diseñado para operaciones de búsqueda global y filtrado en memoria.
     *
     * @param uid      Identificador único del usuario.
     * @param callback Interfaz que retorna la totalidad de los eventos registrados.
     */
    public void obtenerTodosLosEventos(String uid, LeerEventosCallback callback) {
        mDatabase.child("eventos").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Evento> listaTemporal = new ArrayList<>();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Evento e = data.getValue(Evento.class);
                            if (e != null) {
                                e.setId(data.getKey());
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

    /**
     * Elimina de forma permanente un evento del calendario del usuario.
     *
     * @param userId   Identificador único del usuario.
     * @param eventoId Identificador del evento a eliminar.
     * @param callback Interfaz de respuesta para notificar éxito o error.
     */
    public void eliminarEvento(String userId, String eventoId, AccionCallback callback) {
        mDatabase.child("eventos").child(userId).child(eventoId).removeValue()
                .addOnSuccessListener(aVoid -> callback.onExito())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}