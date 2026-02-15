package com.easypets.models;

public class Mascota {

    // --- 1. Datos Básicos ---
    private String idMascota;
    private String nombre;
    private String especie;
    private String raza;
    private String sexo;
    private String fechaNacimiento;
    private String color;

    // --- 2. Datos Físicos y Legales ---
    private String peso;
    private String microchip;

    // --- 3. Datos Médicos ---
    private boolean esterilizado;
    private String patologias;

    // --- 4. Multimedia ---
    private String fotoPerfilUrl;

    // --- 5. Metadatos ---
    private long timestamp;

    // Constructor vacío OBLIGATORIO para Firebase
    public Mascota() {
    }

    // Constructor completo actualizado
    public Mascota(String idMascota, String nombre, String especie, String raza, String sexo,
                   String fechaNacimiento, String color, String peso, String microchip,
                   boolean esterilizado, String patologias, String fotoPerfilUrl, long timestamp) {
        this.idMascota = idMascota;
        this.nombre = nombre;
        this.especie = especie;
        this.raza = raza;
        this.sexo = sexo;
        this.fechaNacimiento = fechaNacimiento;
        this.color = color;
        this.peso = peso;
        this.microchip = microchip;
        this.esterilizado = esterilizado;
        this.patologias = patologias;
        this.fotoPerfilUrl = fotoPerfilUrl;
        this.timestamp = timestamp;
    }

    // --- GETTERS Y SETTERS ---

    public String getIdMascota() { return idMascota; }
    public void setIdMascota(String idMascota) { this.idMascota = idMascota; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEspecie() { return especie; }
    public void setEspecie(String especie) { this.especie = especie; }

    public String getRaza() { return raza; }
    public void setRaza(String raza) { this.raza = raza; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    public String getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(String fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getPeso() { return peso; }
    public void setPeso(String peso) { this.peso = peso; }

    public String getMicrochip() { return microchip; }
    public void setMicrochip(String microchip) { this.microchip = microchip; }

    public boolean isEsterilizado() { return esterilizado; }
    public void setEsterilizado(boolean esterilizado) { this.esterilizado = esterilizado; }

    public String getPatologias() { return patologias; }
    public void setPatologias(String patologias) { this.patologias = patologias; }

    public String getFotoPerfilUrl() { return fotoPerfilUrl; }
    public void setFotoPerfilUrl(String fotoPerfilUrl) { this.fotoPerfilUrl = fotoPerfilUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}