package com.easypets.models;

/**
 * Enumerado que define las categorías tipificadas para los eventos de la agenda.
 * Centraliza la lógica de nomenclatura y clasificación de las citas, permitiendo
 * una gestión estandarizada de los recordatorios en toda la aplicación.
 * Provee métodos de utilidad para la conversión de tipos y la generación de
 * listados compatibles con componentes de selección de la interfaz de usuario.
 */
public enum TipoEvento {

    NOTA("Nota"),
    VETERINARIO("Veterinario"),
    MEDICACION("Medicación"),
    PELUQUERIA("Peluquería"),
    GUARDERIA("Guardería"),
    PASEO("Paseo"),
    COMIDA("Comida");

    private final String nombre;

    /**
     * Constructor del enumerado.
     *
     * @param nombre Representación textual amigable para el usuario.
     */
    TipoEvento(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Recupera el nombre legible asociado a la constante del evento.
     *
     * @return Cadena de texto con el nombre de la categoría.
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Genera un array con las representaciones textuales de todas las constantes.
     * Útil para poblar adaptadores de vistas de selección (Spinners o Listas).
     *
     * @return Matriz de cadenas con todos los nombres de los tipos de eventos.
     */
    public static String[] obtenerTodosLosNombres() {
        TipoEvento[] tipos = values();
        String[] nombres = new String[tipos.length];
        for (int i = 0; i < tipos.length; i++) {
            nombres[i] = tipos[i].getNombre();
        }
        return nombres;
    }

    /**
     * Realiza una búsqueda inversa para encontrar la constante correspondiente
     * a partir de una cadena de texto, ignorando mayúsculas y minúsculas.
     *
     * @param texto El nombre del tipo de evento a buscar.
     * @return La instancia de TipoEvento coincidente o NOTA por defecto en caso de no existir coincidencia.
     */
    public static TipoEvento desdeString(String texto) {
        for (TipoEvento tipo : values()) {
            if (tipo.nombre.equalsIgnoreCase(texto)) {
                return tipo;
            }
        }
        return NOTA;
    }
}