package com.easypets.ui.servicios;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class BusquedaViewModel extends ViewModel {
    private final MutableLiveData<String> ciudadBuscada = new MutableLiveData<>();

    public void buscarCiudad(String ciudad) {
        ciudadBuscada.setValue(ciudad);
    }

    public LiveData<String> getCiudadBuscada() {
        return ciudadBuscada;
    }
}