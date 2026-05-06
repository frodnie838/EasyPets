package com.easypets.models;

/**
 * Modelo de datos que representa una alerta o notificación dirigida al usuario.
 * Actúa como entidad (POJO) para la serialización y deserialización con Firebase.
 * Almacena tanto los detalles de la alerta en sí como la metainformación del
 * elemento que la originó (por ejemplo, un hilo del foro) para facilitar la navegación.
 */
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

    /**
     * Constructor sin argumentos.
     * Requerido obligatoriamente por el SDK de Firebase para llevar a cabo la instanciación
     * automática del objeto al procesar los nodos (DataSnapshot) mediante reflexión.
     */
    public Notificacion() {
    }

    /**
     * Constructor parametrizado para la inicialización integral de una notificación
     * antes de ser persistida en la base de datos o enviada al usuario.
     *
     * @param id Identificador único de la notificación.
     * @param titulo Título principal o encabezado de la alerta.
     * @param mensaje Cuerpo de texto con el detalle de la notificación.
     * @param tipo Categoría o contexto de la notificación (ej. "NUEVA_RESPUESTA", "SISTEMA").
     * @param hiloId Identificador (UID) del hilo del foro asociado a esta alerta.
     * @param hiloTitulo Título original del hilo para proveer contexto en la UI.
     * @param hiloDescripcion Extracto o descripción del hilo asociado.
     * @param hiloAutor Identificador o nickname del autor original del hilo.
     * @param hiloTimestamp Marca temporal (UNIX epoch) de la creación de la notificación o del hilo.
     */
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