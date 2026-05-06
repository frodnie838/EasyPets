package com.easypets.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Modelo de datos que representa una publicación en la galería o "feed" social de la comunidad.
 * Actúa como entidad (POJO) para la serialización y deserialización automática con Firebase.
 * Estructura la información multimedia de la mascota, los datos del autor y
 * gestiona las métricas de interacción social (recuento de Likes y Comentarios).
 */
public class PublicacionMascota {

    private String id;
    private String idAutor;
    private String autorNick;
    private String nombreMascota;
    private String descripcion;
    private String fotoBase64;
    private long timestamp;
    private Map<String, Boolean> likes;
    private int comentariosCount;

    /**
     * Constructor sin argumentos.
     * Requerido obligatoriamente por el SDK de Firebase para llevar a cabo la instanciación
     * automática del objeto al procesar los nodos (DataSnapshot) mediante reflexión.
     * Inicializa el mapa de "likes" de forma defensiva para evitar excepciones de referencias nulas.
     */
    public PublicacionMascota() {
        likes = new HashMap<>();
    }

    /**
     * Constructor parametrizado para la inicialización integral de una nueva publicación
     * antes de ser enviada y persistida en la base de datos.
     *
     * @param id Identificador único de la publicación.
     * @param idAutor UID del usuario que realiza la publicación.
     * @param autorNick Alias (nickname) del creador para visualización rápida.
     * @param nombreMascota Nombre de la mascota protagonista de la publicación.
     * @param descripcion Texto explicativo o pie de foto (caption) introducido por el usuario.
     * @param fotoBase64 Cadena en formato Base64 o URL remota (Storage) de la fotografía publicada.
     * @param timestamp Marca temporal (UNIX epoch) del momento exacto de la publicación.
     */
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