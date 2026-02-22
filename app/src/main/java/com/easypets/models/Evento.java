package com.easypets.models;

public class Evento {
    private String id;
    private String titulo;
    private String fecha; // Formato: "dd/MM/yyyy"
    private String tipo; // Ej: "Nota", "Veterinario", "Peluquería"

    public Evento() {
    }

    public Evento(String id, String titulo, String fecha, String tipo) {
        this.id = id;
        this.titulo = titulo;
        this.fecha = fecha;
        this.tipo = tipo;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}