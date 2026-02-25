package com.easypets.models;

public class Veterinario {
    private String nombre;
    private String direccion;
    private String telefono;
    private double latitud;
    private double longitud;

    public Veterinario(String nombre, String direccion, String telefono, double latitud, double longitud) {
        this.nombre = nombre;
        this.direccion = direccion;
        this.telefono = telefono;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public String getNombre() { return nombre; }
    public String getDireccion() { return direccion; }
    public String getTelefono() { return telefono; }
    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }
}