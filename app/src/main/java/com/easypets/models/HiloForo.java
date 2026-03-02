package com.easypets.models;

public class HiloForo {
    private String id;
    private String titulo;
    private String descripcion;
    private String idAutor;
    private String nombreAutor;
    private long timestampCreacion;

    // Constructor vacío obligatorio para Firebase
    public HiloForo() {}

    public HiloForo(String id, String titulo, String descripcion, String idAutor, String nombreAutor, long timestampCreacion) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.idAutor = idAutor;
        this.nombreAutor = nombreAutor;
        this.timestampCreacion = timestampCreacion;
    }

    // Getters
    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public String getIdAutor() { return idAutor; }
    public String getNombreAutor() { return nombreAutor; }
    public long getTimestampCreacion() { return timestampCreacion; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setIdAutor(String idAutor) { this.idAutor = idAutor; }
    public void setNombreAutor(String nombreAutor) { this.nombreAutor = nombreAutor; }
    public void setTimestampCreacion(long timestampCreacion) { this.timestampCreacion = timestampCreacion; }
}