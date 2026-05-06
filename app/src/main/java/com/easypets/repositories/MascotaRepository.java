package com.easypets.repositories;

import androidx.annotation.NonNull;

import com.easypets.models.Mascota;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio encargado de gestionar las operaciones CRUD de la entidad Mascota
 * en Firebase Realtime Database.
 */
public class MascotaRepository {

    private final DatabaseReference mDatabase;
    private DatabaseReference mascotasRef;
    private ValueEventListener mascotasListener;

    public MascotaRepository() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    // =========================================================================
    // INTERFACES CALLBACK
    // =========================================================================

    public interface AccionCallback {
        void onExito();
        void onError(String error);
    }

    public interface LeerMascotasCallback {
        void onResultado(List<Mascota> listaMascotas);
        void onError(String error);
    }

    public interface LeerUnaMascotaCallback {
        void onResultado(Mascota mascota);
        void onError(String error);
    }

    // =========================================================================
    // MÉTODOS DE OPERACIÓN (CRUD)
    // =========================================================================

    /**
     * Registra una nueva mascota en la base de datos generando un identificador único.
     *
     * @param uid      Identificador del usuario propietario.
     * @param mascota  Objeto Mascota a guardar.
     * @param callback Interfaz de respuesta para notificar éxito o error.
     */
    public void guardarMascota(String uid, Mascota mascota, AccionCallback callback) {
        String idMascota = mDatabase.child("mascotas").child(uid).push().getKey();

        if (idMascota != null) {
            mascota.setIdMascota(idMascota);
            mDatabase.child("mascotas").child(uid).child(idMascota).setValue(mascota)
                    .addOnSuccessListener(aVoid -> callback.onExito())
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
        } else {
            callback.onError("Error de sistema: No se pudo generar el identificador de la mascota.");
        }
    }

    /**
     * Establece un listener en tiempo real para obtener y sincronizar
     * la lista de mascotas del usuario.
     *
     * @param uid      Identificador del usuario.
     * @param callback Interfaz que retorna la lista actualizada de mascotas.
     */
    public void escucharMascotas(String uid, LeerMascotasCallback callback) {
        mascotasRef = mDatabase.child("mascotas").child(uid);

        mascotasListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Mascota> listaTemporal = new ArrayList<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Mascota m = data.getValue(Mascota.class);
                    if (m != null) {
                        listaTemporal.add(m);
                    }
                }
                callback.onResultado(listaTemporal);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        };

        mascotasRef.addValueEventListener(mascotasListener);
    }

    /**
     * Elimina el listener activo de mascotas para evitar fugas de memoria (Memory Leaks).
     */
    public void detenerEscucha() {
        if (mascotasRef != null && mascotasListener != null) {
            mascotasRef.removeEventListener(mascotasListener);
        }
    }

    /**
     * Recupera una mascota específica mediante su identificador para consultas únicas.
     *
     * @param uid       Identificador del usuario.
     * @param idMascota Identificador de la mascota buscada.
     * @param callback  Interfaz que retorna el objeto Mascota solicitado.
     */
    public void obtenerMascotaPorId(String uid, String idMascota, LeerUnaMascotaCallback callback) {
        mDatabase.child("mascotas").child(uid).child(idMascota).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                Mascota mascota = task.getResult().getValue(Mascota.class);
                callback.onResultado(mascota);
            } else {
                callback.onError("No se encontraron los datos de la mascota solicitada.");
            }
        });
    }

    /**
     * Elimina de forma permanente una mascota de la base de datos.
     *
     * @param uid       Identificador del usuario.
     * @param idMascota Identificador de la mascota a eliminar.
     * @param callback  Interfaz de respuesta para notificar éxito o error.
     */
    public void eliminarMascota(String uid, String idMascota, AccionCallback callback) {
        mDatabase.child("mascotas").child(uid).child(idMascota).removeValue()
                .addOnSuccessListener(aVoid -> callback.onExito())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Sobrescribe los datos de una mascota existente en la base de datos.
     *
     * @param uid      Identificador del usuario.
     * @param mascota  Objeto Mascota con los datos actualizados.
     * @param callback Interfaz de respuesta para notificar éxito o error.
     */
    public void actualizarMascota(String uid, Mascota mascota, AccionCallback callback) {
        if (mascota.getIdMascota() == null || mascota.getIdMascota().isEmpty()) {
            callback.onError("Error de sistema: ID de mascota no válido.");
            return;
        }

        mDatabase.child("mascotas").child(uid).child(mascota.getIdMascota()).setValue(mascota)
                .addOnSuccessListener(aVoid -> callback.onExito())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}