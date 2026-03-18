package com.easypets.models;

public class ComentarioMascota {
    private String id;
    private String idAutor;
    private String autorNick;
    private String texto;
    private long timestamp;
    private String autorFoto;

    public ComentarioMascota() { }

    public ComentarioMascota(String id, String idAutor, String autorNick, String texto, long timestamp, String autorFoto) {
        this.id = id;
        this.idAutor = idAutor;
        this.autorNick = autorNick;
        this.texto = texto;
        this.timestamp = timestamp;
        this.autorFoto = autorFoto;
    }

    public String getId() { return id; }
    public String getIdAutor() { return idAutor; }
    public String getAutorNick() { return autorNick; }
    public String getTexto() { return texto; }
    public long getTimestamp() { return timestamp; }
    public String getAutorFoto() { return autorFoto; }
}