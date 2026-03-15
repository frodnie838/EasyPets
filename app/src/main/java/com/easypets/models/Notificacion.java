package com.easypets.models;

public class Notificacion {
    public String id;
    public String titulo;
    public String mensaje;

    public String tipo;
    public String hiloId;
    public String hiloTitulo;
    public String hiloDescripcion;
    public String hiloAutor;
    public long hiloTimestamp;

    public Notificacion(String id, String titulo, String mensaje, String tipo,
                        String hiloId, String hiloTitulo, String hiloDescripcion,
                        String hiloAutor, long hiloTimestamp) {
        this.id = id;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.tipo = tipo;
        this.hiloId = hiloId;
        this.hiloTitulo = hiloTitulo;
        this.hiloDescripcion = hiloDescripcion;
        this.hiloAutor = hiloAutor;
        this.hiloTimestamp = hiloTimestamp;
    }
}