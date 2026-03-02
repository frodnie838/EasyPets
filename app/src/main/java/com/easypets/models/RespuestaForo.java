package com.easypets.models;

public class RespuestaForo {
    private String id;
    private String texto;
    private String idAutor;
    private String nombreAutor;
    private long timestampCreacion;

    // Constructor vacío para Firebase
    public RespuestaForo() {}

    public RespuestaForo(String id, String texto, String idAutor, String nombreAutor, long timestampCreacion) {
        this.id = id;
        this.texto = texto;
        this.idAutor = idAutor;
        this.nombreAutor = nombreAutor;
        this.timestampCreacion = timestampCreacion;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public String getIdAutor() { return idAutor; }
    public void setIdAutor(String idAutor) { this.idAutor = idAutor; }

    public String getNombreAutor() { return nombreAutor; }
    public void setNombreAutor(String nombreAutor) { this.nombreAutor = nombreAutor; }

    public long getTimestampCreacion() { return timestampCreacion; }
    public void setTimestampCreacion(long timestampCreacion) { this.timestampCreacion = timestampCreacion; }
}