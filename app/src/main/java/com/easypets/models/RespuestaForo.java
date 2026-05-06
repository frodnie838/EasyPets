package com.easypets.models;

/**
 * Modelo de datos que representa una respuesta individual dentro de un hilo del foro comunitario.
 * Actúa como entidad (POJO) para la serialización y deserialización automática con Firebase.
 * Incluye soporte nativo para la gestión de estados de la publicación, permitiendo
 * el rastreo de mensajes editados y la aplicación de "Soft Delete" (eliminación lógica)
 * para mantener la integridad de la base de datos.
 */
public class RespuestaForo {

    private String id;
    private String texto;
    private String idAutor;
    private String nombreAutor;
    private long timestampCreacion;
    private boolean editado = false;
    private boolean eliminado = false;

    /**
     * Constructor sin argumentos.
     * Requerido obligatoriamente por el SDK de Firebase para llevar a cabo la instanciación
     * automática del objeto al procesar los nodos (DataSnapshot) mediante reflexión.
     */
    public RespuestaForo() {}

    /**
     * Constructor parametrizado para la inicialización integral de una nueva respuesta
     * antes de ser persistida en la base de datos del foro.
     *
     * @param id Identificador único de la respuesta generada.
     * @param texto Contenido textual del mensaje publicado.
     * @param idAutor UID único del usuario que redacta la respuesta.
     * @param nombreAutor Alias (nickname) o nombre del creador para visualización.
     * @param timestampCreacion Marca temporal (UNIX epoch) del momento exacto de la publicación.
     * @param editado Bandera que indica si el contenido del mensaje original ha sido modificado.
     */
    public RespuestaForo(String id, String texto, String idAutor, String nombreAutor, long timestampCreacion, boolean editado) {
        this.id = id;
        this.texto = texto;
        this.idAutor = idAutor;
        this.nombreAutor = nombreAutor;
        this.timestampCreacion = timestampCreacion;
        this.editado = editado;
        this.eliminado = false;
    }

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