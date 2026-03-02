package com.easypets.models;

public class RespuestaForo {
    private String id;
    private String texto;
    private String idAutor;
    private String nombreAutor;
    private long timestampCreacion;
    private boolean editado = false;
    private boolean eliminado = false;
    public RespuestaForo() {}

    public RespuestaForo(String id, String texto, String idAutor, String nombreAutor, long timestampCreacion, boolean editado) {
        this.id = id;
        this.texto = texto;
        this.idAutor = idAutor;
        this.nombreAutor = nombreAutor;
        this.timestampCreacion = timestampCreacion;
        this.editado = editado;
        this.eliminado = false;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public boolean isEditado() { return editado; }
    public void setEditado(boolean editado) { this.editado = editado; }

    public String getIdAutor() { return idAutor; }
    public void setIdAutor(String idAutor) { this.idAutor = idAutor; }

    public String getNombreAutor() { return nombreAutor; }
    public void setNombreAutor(String nombreAutor) { this.nombreAutor = nombreAutor; }

    public long getTimestampCreacion() { return timestampCreacion; }
    public void setTimestampCreacion(long timestampCreacion) { this.timestampCreacion = timestampCreacion; }
    public boolean isEliminado() { return eliminado; }
    public void setEliminado(boolean eliminado) { this.eliminado = eliminado; }
}