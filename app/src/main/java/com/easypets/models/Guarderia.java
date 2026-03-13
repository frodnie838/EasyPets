package com.easypets.models;

public class Guarderia {
    private String nombre;
    private String imageUrl;
    private double rating;
    private String ubicacion;
    private String telefono;
    private double latitud;
    private double longitud;

    public Guarderia(String nombre, String imageUrl, double rating, String ubicacion, String telefono, double latitud, double longitud) {
        this.nombre = nombre;
        this.imageUrl = imageUrl;
        this.rating = rating;
        this.ubicacion = ubicacion;
        this.telefono = telefono;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public String getNombre() { return nombre; }
    public String getImageUrl() { return imageUrl; }
    public double getRating() { return rating; }
    public String getUbicacion() { return ubicacion; }
    public String getTelefono() { return telefono; }
    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }
}