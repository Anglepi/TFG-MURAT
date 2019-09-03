package Models;

import com.eclipsesource.json.JsonArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ConfiguracionCruce {
    private Map<String, Integer> semaforos_estados; //<NombreSemaforo, Color>
    private String nombreConfiguracion;
    private double tiempo_espera;

    public ConfiguracionCruce(Map<String, Integer> semaforos_estados, String nombreConfiguracion, String duracion){
        this.semaforos_estados = semaforos_estados;
        this.nombreConfiguracion = nombreConfiguracion;
        this.tiempo_espera = Integer.parseInt(duracion);
    }

    /*
    Devuelve un array de ConfiguracionCruce dado el JsonArray 'listadoJson'
     */
    public static ArrayList<ConfiguracionCruce> inicializadorMultiple(JsonArray listadoJson){
        ArrayList<ConfiguracionCruce> arrayConfiguraciones = new ArrayList<>();

        for(int i=0 ; i<listadoJson.size() ; ){
            String nombreConfig = listadoJson.get(i).asObject().getString("Nombre", "");
            String tiempoConfig = listadoJson.get(i).asObject().getString("Tiempo", "0");
            String nombreConfigActual = listadoJson.get(i).asObject().getString("Nombre", "");
            Map<String, Integer> estados_semaforos = new HashMap<>();

            while(nombreConfig.equals(nombreConfigActual) && i<listadoJson.size()){
                Integer estado = getIdColor(listadoJson.get(i).asObject().getString("EstadoSemaforo", ""));
                String nombre = listadoJson.get(i).asObject().getString("NombreSemaforo", "");
                estados_semaforos.put("semaforo_"+nombre, estado);
                i++;
                if(i<listadoJson.size())
                nombreConfigActual = listadoJson.get(i).asObject().getString("Nombre", "");
            }
            arrayConfiguraciones.add(new ConfiguracionCruce(estados_semaforos, nombreConfig, tiempoConfig));
        }

        return arrayConfiguraciones;
    }

    /*
    Devuelve la configuración concreta semaforos_estados
     */
    public Map<String, Integer> getConfig(){
        return semaforos_estados;
    }

    /*
    Devuelve el número de semáforos que compone la configuración
     */
    public int getNumeroSemaforos(){
        return semaforos_estados.size();
    }

    public void setTiempoEspera(double t){
        this.tiempo_espera = t;
    }

    public double getTiempoEspera(){
        return this.tiempo_espera;
    }

    public String getNombreConfiguracion(){
        return nombreConfiguracion;
    }

    /*
    Imprime la configuración (DEBUG)
     */
    public String toString(){
        String salida = "Cruce "+this.nombreConfiguracion+"\n";
        for(Map.Entry<String, Integer> entry : semaforos_estados.entrySet()){
            salida += "Semaforo "+entry.getKey()+" está "+entry.getValue()+"\n";
        }
        return salida;
    }

    private static Integer getIdColor(String color){
        Integer id = -1;
        if(color.equals("ROJO")){
            id = Constantes.ROJO;
        } else if(color.equals("AMBAR FIJO")){
            id = Constantes.AMBAR_FIJO;
        } else if(color.equals("AMBAR INTERMITENTE") || color.equals("AMBAR")){
            id = Constantes.AMBAR_INTERMITENTE;
        } else if(color.equals("VERDE")){
            id = Constantes.VERDE;
        } else if(color.equals("APAGADO")){
            id = Constantes.APAGADO;
        }
        return id;
    }
}
