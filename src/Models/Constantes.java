package Models;

public enum Constantes {
    INSTANCE;
    //Constantes para el controlador HTTP
    public static final String URL = "http://localhost/";
    public static final String LISTADO_CRUCES = "cruce/obtenerCruces";
    public static final String LISTADO_SEMAFOROS = "semaforo/obtenerSemaforos";
    public static final String LISTADO_SEMAFOROS_COMPLETO = "semaforo/obtenerTodosSemaforos";
    public static final String LISTADO_CONFIGURACIONES = "configuracionCruce/obtenerConfiguraciones";
    public static final String LISTADO_ORIGENES_SEMAFORO = "semaforo/obtenerOrigenesSemaforo";
    public static final String LISTADO_DESTINOS_SEMAFORO = "semaforo/obtenerDestinosSemaforo";

    //Constantes globales
    public static final String SMARTCITY_NOMBRE = "Utopia"; //Usada en todos los agentes
    public static final int TRANSFORMADOR_TIEMPO = 150;

    //Estados de SmartCity
    public static final int SMARTCITY_GESTIONANDO_SUSCRIPCIONES = 0;
    public static final int SMARTCITY_GENERANDO_INFORMES = 1;
    public static final int SMARTCITY_FINALIZANDO = -1;

    //Estados del cruce
    public static final int CRUCE_INICIALIZANDO_CONFIGURACIONES = 0;
    public static final int CRUCE_ESPERANDO_SEMAFOROS = 1;
    public static final int CRUCE_SUSCRIBIENDO_A_SMARTCITY = 2;
    public static final int CRUCE_GESTIONANDO_SEMAFOROS = 3;
    public static final int CRUCE_ANALIZANDO_DATOS = 4;
    public static final int CRUCE_FINALIZANDO = -1;

    //Estados del semaforo
    public static final int SEMAFORO_CARGANDO_DESTINOS = 0;
    public static final int SEMAFORO_SUSCRIBIENDO_A_ORIGENES = 1;
    public static final int SEMAFORO_SUSCRIBIENDO_A_CRUCE = 2;
    public static final int SEMAFORO_GESTIONANDO_TRAFICO = 3;
    public static final int SEMAFORO_FINALIZANDO = -1;

    //Colores del semaforo
    public static final int ROJO = 0;
    public static final int AMBAR_FIJO = 1;
    public static final int AMBAR_INTERMITENTE = 2;
    public static final int VERDE = 3;
    public static final int APAGADO = 4;

}
