package com.easypets.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Modelo de datos que representa un hilo de debate o tema principal de discusión
 * dentro del foro de la comunidad. Actúa como entidad (POJO) para facilitar la
 * serialización y deserialización automática con Firebase Realtime Database.
 * Incluye soporte para el seguimiento del recuento de interacciones de los usuarios (Likes).
 */
public class HiloForo {

    private String id;
    private String titulo;
    private String descripcion;
    private String idAutor;
    private String nombreAutor;
    private long timestampCreacion;
    private Map<String, Boolean> likes = new HashMap<>();

    /**
     * Constructor sin argumentos.
     * Requerido obligatoriamente por el SDK de Firebase para permitir la instanciación
     * automática del objeto al recuperar los datos (DataSnapshot) mediante reflexión.
     */
    public HiloForo() {}

    /**
     * Constructor parametrizado para la inicialización de un nuevo hilo de debate
     * antes de su persistencia en la base de datos.
     *
     * @param id Identificador único del hilo.
     * @param titulo Título principal o asunto del debate.
     * @param descripcion Cuerpo de texto explicativo o mensaje inicial del hilo.
     * @param idAutor Identificador único (UID) del usuario creador del hilo.
     * @param nombreAutor Alias (nickname) o nombre del autor para visualización rápida.
     * @param timestampCreacion Marca de tiempo (UNIX epoch) de la creación del hilo.
     */
    public HiloForo(String id, String titulo, String descripcion, String idAutor, String nombreAutor, long timestampCreacion) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.idAutor = idAutor;
        this.nombreAutor = nombreAutor;
        this.timestampCreacion = timestampCreacion;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getIdAutor() { return idAutor; }
    public void setIdAutor(String idAutor) { this.idAutor = idAutor; }

    public String getNombreAutor() { return nombreAutor; }
    public void setNombreAutor(String nombreAutor) { this.nombreAutor = nombreAutor; }

    public long getTimestampCreacion() { return timestampCreacion; }
    public void setTimestampCreacion(long timestampCreacion) { this.timestampCreacion = timestampCreacion; }

    public Map<String, Boolean> getLikes() { return likes; }
    public void setLikes(Map<String, Boolean> likes) { this.likes = likes; }
}