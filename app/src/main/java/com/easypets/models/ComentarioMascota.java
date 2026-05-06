package com.easypets.models;

/**
 * Modelo de datos que representa un comentario realizado sobre la publicación o perfil de una mascota.
 * Actúa como una entidad (POJO) para facilitar la lectura y escritura automática
 * (serialización y deserialización) en la base de datos en tiempo real de Firebase.
 */
public class ComentarioMascota {
    private String id;
    private String idAutor;
    private String autorNick;
    private String texto;
    private long timestamp;
    private String autorFoto;

    /**
     * Constructor sin argumentos.
     * Requerido obligatoriamente por el SDK de Firebase para permitir la instanciación
     * automática del objeto al recuperar los datos (DataSnapshot) mediante reflexión.
     */
    public ComentarioMascota() { }

    /**
     * Constructor parametrizado para inicializar un nuevo comentario antes de su persistencia en el servidor.
     *
     * @param id Identificador único del comentario.
     * @param idAutor UID del usuario que publica el comentario.
     * @param autorNick Alias (nickname) del autor almacenado para visualización rápida.
     * @param texto Contenido textual del mensaje publicado.
     * @param timestamp Marca de tiempo (UNIX epoch) del momento exacto de la creación.
     * @param autorFoto Cadena en Base64 o URL remota de la fotografía de perfil del autor.
     */
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