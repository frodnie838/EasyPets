package com.easypets.models;

public class Mascota {

    private String idMascota;
    private String nombre;
    private String especie;
    private String raza;
    private String peso;
    private long timestamp;

    // Constructor vacío OBLIGATORIO para que Firebase funcione
    public Mascota() {
    }

    // Constructor con todos los datos
    public Mascota(String idMascota, String nombre, String especie, String raza, String peso, long timestamp) {
        this.idMascota = idMascota;
        this.nombre = nombre;
        this.especie = especie;
        this.raza = raza;
        this.peso = peso;
        this.timestamp = timestamp;
    }

    // Getters y Setters (Para leer y escribir los datos)
    public String getIdMascota() { return idMascota; }
    public void setIdMascota(String idMascota) { this.idMascota = idMascota; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEspecie() { return especie; }
    public void setEspecie(String especie) { this.especie = especie; }

    public String getRaza() { return raza; }
    public void setRaza(String raza) { this.raza = raza; }

    public String getPeso() { return peso; }
    public void setPeso(String peso) { this.peso = peso; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}