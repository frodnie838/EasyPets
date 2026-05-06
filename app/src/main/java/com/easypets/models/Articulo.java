package com.easypets.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Modelo de datos que representa un artículo o publicación dentro de la plataforma.
 * Actúa como entidad (POJO) para la serialización y deserialización automática
 * bidireccional con Firebase Realtime Database. Soporta contenido oficial y generado
 * por la comunidad, métricas de interacción (Likes) y referencias multimedia híbridas.
 */
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

    /**
     * Constructor sin argumentos.
     * Es estrictamente requerido por el SDK de Firebase para llevar a cabo
     * la instanciación de la clase a través de reflexión (DataSnapshot.getValue).
     */
    public Articulo() {}

    /**
     * Constructor parametrizado para la inicialización completa de la entidad
     * antes de su persistencia en la base de datos.
     *
     * @param id Identificador único del artículo.
     * @param idAutor UID del usuario que crea la publicación.
     * @param titulo Título principal del artículo.
     * @param descripcionCorta Resumen visualizado en las listas de previsualización.
     * @param contenidoCompleto Cuerpo principal del artículo.
     * @param autor Alias o nombre del creador.
     * @param timestampCreacion Marca de tiempo UNIX de la creación.
     * @param imagenPortadaBase64 Cadena en Base64 o URL remota de la imagen de cabecera.
     * @param urlEnlace Enlace externo opcional de referencia.
     * @param esOficial Flag booleano que determina si el contenido es curado por el sistema o comunitario.
     */
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

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIdAutor() { return idAutor; }
    public void setIdAutor(String idAutor) { this.idAutor = idAutor; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcionCorta() { return descripcionCorta; }
    public void setDescripcionCorta(String descripcionCorta) { this.descripcionCorta = descripcionCorta; }

    public String getContenidoCompleto() { return contenidoCompleto; }
    public void setContenidoCompleto(String contenidoCompleto) { this.contenidoCompleto = contenidoCompleto; }

    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }

    public long getTimestampCreacion() { return timestampCreacion; }
    public void setTimestampCreacion(long timestampCreacion) { this.timestampCreacion = timestampCreacion; }

    public String getImagenPortadaBase64() { return imagenPortadaBase64; }
    public void setImagenPortadaBase64(String imagenPortadaBase64) { this.imagenPortadaBase64 = imagenPortadaBase64; }

    public String getUrlEnlace() { return urlEnlace; }
    public void setUrlEnlace(String urlEnlace) { this.urlEnlace = urlEnlace; }

    public boolean isEsOficial() { return esOficial; }
    public void setEsOficial(boolean esOficial) { this.esOficial = esOficial; }

    public Map<String, Boolean> getLikes() { return likes; }
    public void setLikes(Map<String, Boolean> likes) { this.likes = likes; }
}