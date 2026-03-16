package com.easypets.models;

public enum TipoEvento {
    NOTA("Nota"),
    VETERINARIO("Veterinario"),
    MEDICACION("Medicación"),
    PELUQUERIA("Peluquería"),
    GUARDERIA("Guardería"),
    PASEO("Paseo"),
    COMIDA("Comida");

    private final String nombre;

    TipoEvento(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    public static String[] obtenerTodosLosNombres() {
        TipoEvento[] tipos = values();
        String[] nombres = new String[tipos.length];
        for (int i = 0; i < tipos.length; i++) {
            nombres[i] = tipos[i].getNombre();
        }
        return nombres;
    }

    public static TipoEvento desdeString(String texto) {
        for (TipoEvento tipo : values()) {
            if (tipo.nombre.equalsIgnoreCase(texto)) {
                return tipo;
            }
        }
        return NOTA; // Por defecto devolvemos Nota si algo falla
    }
}