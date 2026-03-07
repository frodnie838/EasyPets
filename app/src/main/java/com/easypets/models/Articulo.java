package com.easypets.models;

import java.util.HashMap;
import java.util.Map;

public class Articulo {
    private String id;
    private String idAutor;
    private String titulo;
    private String descripcionCorta;
    private String contenidoCompleto;
    private String autor;
    private long timestampCreacion;
    private String imagenPortadaBase64;
    private String urlEnlace;
    private Map<String, Boolean> likes = new HashMap<>();
    private boolean esOficial;

    // 1. Constructor vacío (Obligatorio para que Firebase no dé error al leer)
    public Articulo() {}

    // 2. Constructor Completo
    public Articulo(String id, String idAutor, String titulo, String descripcionCorta, String contenidoCompleto,
                    String autor, long timestampCreacion, String imagenPortadaBase64,
                    String urlEnlace, boolean esOficial) {
        this.id = id;
        this.titulo = titulo;
        this.idAutor = idAutor;
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
    public String getIdAutor() { return idAutor; }
    public String getTitulo() { return titulo; }
    public String getDescripcionCorta() { return descripcionCorta; }
    public String getContenidoCompleto() { return contenidoCompleto; }
    public String getAutor() { return autor; }
    public long getTimestampCreacion() { return timestampCreacion; }
    public String getImagenPortadaBase64() { return imagenPortadaBase64; }
    public String getUrlEnlace() { return urlEnlace; }
    public boolean isEsOficial() { return esOficial; }
    public Map<String, Boolean> getLikes() { return likes; }
    public void setLikes(Map<String, Boolean> likes) { this.likes = likes; }

    public void setId(String id) { this.id = id; }

    public void setIdAutor(String idAutor) { this.idAutor = idAutor; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public void setDescripcionCorta(String descripcionCorta) { this.descripcionCorta = descripcionCorta; }
    public void setContenidoCompleto(String contenidoCompleto) { this.contenidoCompleto = contenidoCompleto; }
    public void setAutor(String autor) { this.autor = autor; }
    public void setTimestampCreacion(long timestampCreacion) { this.timestampCreacion = timestampCreacion; }
    public void setImagenPortadaBase64(String imagenPortadaBase64) { this.imagenPortadaBase64 = imagenPortadaBase64; }
    public void setUrlEnlace(String urlEnlace) { this.urlEnlace = urlEnlace; }
    public void setEsOficial(boolean esOficial) { this.esOficial = esOficial; }
}