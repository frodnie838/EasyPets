package com.easypets.models;

public class Articulo {
    private String id;
    private String titulo;
    private String descripcionCorta;
    private String contenidoCompleto;
    private String autor;
    private long timestampCreacion; // Usamos 'long' (milisegundos) porque es perfecto para ordenar listas cronológicamente
    private String imagenPortadaBase64; // Para que los usuarios puedan subir fotos (igual que en las mascotas)
    private String urlEnlace; // Para enlaces a YouTube o webs externas (opcional)
    private boolean esOficial; // true = creado por ti (EasyPets) | false = creado por la comunidad

    // 1. Constructor vacío (Obligatorio para que Firebase no dé error al leer)
    public Articulo() {}

    // 2. Constructor Completo
    public Articulo(String id, String titulo, String descripcionCorta, String contenidoCompleto,
                    String autor, long timestampCreacion, String imagenPortadaBase64,
                    String urlEnlace, boolean esOficial) {
        this.id = id;
        this.titulo = titulo;
        this.descripcionCorta = descripcionCorta;
        this.contenidoCompleto = contenidoCompleto;
        this.autor = autor;
        this.timestampCreacion = timestampCreacion;
        this.imagenPortadaBase64 = imagenPortadaBase64;
        this.urlEnlace = urlEnlace;
        this.esOficial = esOficial;
    }

    // Getters
    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescripcionCorta() { return descripcionCorta; }
    public String getContenidoCompleto() { return contenidoCompleto; }
    public String getAutor() { return autor; }
    public long getTimestampCreacion() { return timestampCreacion; }
    public String getImagenPortadaBase64() { return imagenPortadaBase64; }
    public String getUrlEnlace() { return urlEnlace; }
    public boolean isEsOficial() { return esOficial; }

    public void setId(String id) { this.id = id; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public void setDescripcionCorta(String descripcionCorta) { this.descripcionCorta = descripcionCorta; }
    public void setContenidoCompleto(String contenidoCompleto) { this.contenidoCompleto = contenidoCompleto; }
    public void setAutor(String autor) { this.autor = autor; }
    public void setTimestampCreacion(long timestampCreacion) { this.timestampCreacion = timestampCreacion; }
    public void setImagenPortadaBase64(String imagenPortadaBase64) { this.imagenPortadaBase64 = imagenPortadaBase64; }
    public void setUrlEnlace(String urlEnlace) { this.urlEnlace = urlEnlace; }
    public void setEsOficial(boolean esOficial) { this.esOficial = esOficial; }
}