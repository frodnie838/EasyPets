package com.easypets.models;

import java.util.HashMap;
import java.util.Map;

public class PublicacionMascota {
    private String id;
    private String idAutor;
    private String autorNick;
    private String nombreMascota;
    private String descripcion;
    private String fotoBase64;
    private long timestamp;
    private Map<String, Boolean> likes;
    private int comentariosCount; // ✨ NUEVO CONTADOR

    public PublicacionMascota() {
        likes = new HashMap<>();
    }

    public PublicacionMascota(String id, String idAutor, String autorNick, String nombreMascota, String descripcion, String fotoBase64, long timestamp) {
        this.id = id;
        this.idAutor = idAutor;
        this.autorNick = autorNick;
        this.nombreMascota = nombreMascota;
        this.descripcion = descripcion;
        this.fotoBase64 = fotoBase64;
        this.timestamp = timestamp;
        this.likes = new HashMap<>();
        this.comentariosCount = 0;
    }

    public String getId() { return id; }
    public String getIdAutor() { return idAutor; }
    public String getAutorNick() { return autorNick; }
    public String getNombreMascota() { return nombreMascota; }
    public String getDescripcion() { return descripcion; }
    public String getFotoBase64() { return fotoBase64; }
    public long getTimestamp() { return timestamp; }
    public Map<String, Boolean> getLikes() { return likes; }
    public void setLikes(Map<String, Boolean> likes) { this.likes = likes; }
    public int getComentariosCount() { return comentariosCount; }
    public void setComentariosCount(int comentariosCount) { this.comentariosCount = comentariosCount; }
}