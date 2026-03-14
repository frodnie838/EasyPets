package com.easypets.models;

public class LocalServicio {
    private String nombre;
    private String direccion;
    private String fotoUrl;
    private double rating;
    private int totalResenas;
    private double latitud;
    private double longitud;

    public LocalServicio(String nombre, String direccion, String fotoUrl, double rating, int totalResenas, double latitud, double longitud) {
        this.nombre = nombre;
        this.direccion = direccion;
        this.fotoUrl = fotoUrl;
        this.rating = rating;
        this.totalResenas = totalResenas;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public String getNombre() { return nombre; }
    public String getDireccion() { return direccion; }
    public String getFotoUrl() { return fotoUrl; }
    public double getRating() { return rating; }
    public int getTotalResenas() { return totalResenas; }
    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }
}