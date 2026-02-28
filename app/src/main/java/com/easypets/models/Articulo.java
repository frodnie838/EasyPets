package com.easypets.models;

public class Articulo {
    private String id;
    private String titulo;
    private String descripcionCorta;
    private String contenidoCompleto;
    private int imagenResId;
    private String autor;

    public Articulo() {}

    public Articulo(String titulo, String descripcionCorta, String contenidoCompleto, int imagenResId) {
        this.titulo = titulo;
        this.descripcionCorta = descripcionCorta;
        this.contenidoCompleto = contenidoCompleto;
        this.imagenResId = imagenResId;
        this.autor = "EasyPets Oficial";
    }

    public Articulo(String id, String titulo, String descripcionCorta, String contenidoCompleto, String autor) {
        this.id = id;
        this.titulo = titulo;
        this.descripcionCorta = descripcionCorta;
        this.contenidoCompleto = contenidoCompleto;
        this.autor = autor;
        this.imagenResId = 0;
    }

    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescripcionCorta() { return descripcionCorta; }
    public String getContenidoCompleto() { return contenidoCompleto; }
    public int getImagenResId() { return imagenResId; }
    public String getAutor() { return autor; }
}