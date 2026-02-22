package com.easypets.models;

public class Evento {
    private String id;
    private String titulo;
    private String fecha; // Formato: "dd/MM/yyyy"
    private String hora;  // ✨ NUEVO: Formato "HH:mm" (o vacío si no tiene)
    private String tipo;
    private String idMascota;

    public Evento() {
    }

    public Evento(String id, String titulo, String fecha, String hora, String tipo, String idMascota) {
        this.id = id;
        this.titulo = titulo;
        this.fecha = fecha;
        this.hora = hora;
        this.tipo = tipo;
        this.idMascota = idMascota;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getIdMascota() { return idMascota; }
    public void setIdMascota(String idMascota) { this.idMascota = idMascota; }
}