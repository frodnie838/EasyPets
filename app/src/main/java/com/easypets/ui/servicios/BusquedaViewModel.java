package com.easypets.ui.servicios;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * ViewModel encargado de gestionar el estado de los criterios de búsqueda
 * dentro del módulo de servicios. Actúa como puente de comunicación reactiva
 * entre la interfaz de usuario (UI) y las fuentes de datos, permitiendo
 * que los componentes observadores actualicen sus resultados en función
 * de la localización geográfica introducida por el usuario.
 */
public class BusquedaViewModel extends ViewModel {

    private final MutableLiveData<String> ciudadBuscada = new MutableLiveData<>();

    /**
     * Actualiza el flujo de datos con el nombre de la nueva ciudad a consultar.
     * Al emitir un nuevo valor, todos los observadores activos vinculados
     * a este ViewModel recibirán la actualización de forma automática.
     *
     * @param ciudad Nombre o cadena representativa de la localidad a buscar.
     */
    public void buscarCiudad(String ciudad) {
        ciudadBuscada.setValue(ciudad);
    }

    /**
     * Provee el acceso al flujo de datos de la ciudad buscada bajo un contrato
     * de solo lectura (LiveData). Esto garantiza la integridad del estado,
     * impidiendo modificaciones accidentales desde las vistas.
     *
     * @return Un objeto LiveData que contiene la cadena de la ciudad actual.
     */
    public LiveData<String> getCiudadBuscada() {
        return ciudadBuscada;
    }
}