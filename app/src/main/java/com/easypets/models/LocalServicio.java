package com.easypets.models;

/**
 * Modelo de datos que representa un establecimiento o servicio local (veterinarios, parques, tiendas, etc.).
 * Actúa como un contenedor de información (POJO) poblado dinámicamente a partir de
 * las respuestas de la API de Google Places, almacenando datos de geolocalización,
 * métricas de reputación y el estado actual de apertura del negocio.
 */
public class LocalServicio {

    private String nombre;
    private String direccion;
    private String fotoUrl;
    private double rating;
    private int totalResenas;
    private double latitud;
    private double longitud;
    private boolean abiertoAhora;
    private boolean tieneHorario;

    /**
     * Constructor parametrizado para la instanciación de un nuevo servicio local.
     *
     * @param nombre Nombre comercial del establecimiento o ubicación pública.
     * @param direccion Dirección postal formateada.
     * @param fotoUrl URL de la fotografía de referencia (Google Places Photo o fallback genérico).
     * @param rating Puntuación media del establecimiento (habitualmente en escala de 0.0 a 5.0).
     * @param totalResenas Número total de valoraciones emitidas por los usuarios.
     * @param latitud Coordenada de latitud para su representación en mapas.
     * @param longitud Coordenada de longitud para su representación en mapas.
     * @param abiertoAhora Bandera que indica si el negocio está operando en el momento de la consulta.
     * @param tieneHorario Bandera que indica si la API provee información estructural sobre horarios comerciales.
     */
    public LocalServicio(String nombre, String direccion, String fotoUrl, double rating, int totalResenas, double latitud, double longitud, boolean abiertoAhora, boolean tieneHorario) {
        this.nombre = nombre;
        this.direccion = direccion;
        this.fotoUrl = fotoUrl;
        this.rating = rating;
        this.totalResenas = totalResenas;
        this.latitud = latitud;
        this.longitud = longitud;
        this.abiertoAhora = abiertoAhora;
        this.tieneHorario = tieneHorario;
    }

    public String getNombre() { return nombre; }
    public String getDireccion() { return direccion; }
    public String getFotoUrl() { return fotoUrl; }
    public double getRating() { return rating; }
    public int getTotalResenas() { return totalResenas; }
    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }
    public boolean isAbiertoAhora() { return abiertoAhora; }
    public boolean isTieneHorario() { return tieneHorario; }
}