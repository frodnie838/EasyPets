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

public class MascotaRepository {

    private final DatabaseReference mDatabase;
    private DatabaseReference mascotasRef;
    private ValueEventListener mascotasListener;

    public MascotaRepository() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    // --- INTERFACES (Para comunicarse con las pantallas) ---
    public interface AccionCallback {
        void onExito();
        void onError(String error);
    }

    public interface LeerMascotasCallback {
        void onResultado(List<Mascota> listaMascotas);
        void onError(String error);
    }

    // --- 1. GUARDAR UNA MASCOTA ---
    public void guardarMascota(String uid, Mascota mascota, AccionCallback callback) {
        String idMascota = mDatabase.child("mascotas").child(uid).push().getKey();
        if (idMascota != null) {
            mascota.setIdMascota(idMascota); // Le asignamos el ID generado

            // Fíjate qué limpio: guardamos el OBJETO entero en vez de ir dato a dato
            mDatabase.child("mascotas").child(uid).child(idMascota).setValue(mascota)
                    .addOnSuccessListener(aVoid -> callback.onExito())
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
        } else {
            callback.onError("No se pudo generar un ID para la mascota");
        }
    }

    // --- 2. LEER MASCOTAS EN TIEMPO REAL ---
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
                callback.onResultado(listaTemporal); // Le devolvemos la lista lista a la pantalla
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        };

        mascotasRef.addValueEventListener(mascotasListener);
    }

    // --- 3. MATAR AL ZOMBIE ---
    public void detenerEscucha() {
        if (mascotasRef != null && mascotasListener != null) {
            mascotasRef.removeEventListener(mascotasListener);
        }
    }
}