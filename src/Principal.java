import Agents.*;
import Models.Constantes;
import Simulation.Simulacion;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import Controller.HttpController;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.AgentsConnection;
import org.apache.log4j.xml.DOMConfigurator;



public class Principal {
    public static void main(String[] args){
        //Setting the Logger
        JsonArray cruces = HttpController.obtenerListado(Constantes.LISTADO_CRUCES, "");
        JsonArray semaforos = HttpController.obtenerListado(Constantes.LISTADO_SEMAFOROS_COMPLETO, "");
        Simulacion.maxVehiculos = 15;

        DOMConfigurator.configure("configuration/loggin.xml");
        // Logger logger = Logger.getLogger(Principal.class);  /// Yo comentaria esto

        //Conecting to Qpid Broker
        AgentsConnection.connect("localhost", 5672, "test", "guest", "guest", false);
        try {
            SmartCity utopia = new SmartCity(new AgentID(Constantes.SMARTCITY_NOMBRE), cruces.size());
            utopia.start();

            for(JsonValue cruce : cruces){
                String id = cruce.asObject().getString("Id", "0");
                String nombre = cruce.asObject().getString("Nombre", "Cruce desconocido");
                String tDespeje = cruce.asObject().getString("TiempoDespeje", "0");

                Cruce agenteCruce = new Cruce(new AgentID("cruce_" + nombre), id, tDespeje);
                agenteCruce.start();
            }

            for(JsonValue semaforo : semaforos){
                String id = semaforo.asObject().getString("Id", "0");
                String nombre = semaforo.asObject().getString("Nombre", "Semaforo desconocido");
                String tieneSensor = semaforo.asObject().getString("TieneSensor", "0");
                String tamCola = semaforo.asObject().getString("TamCola", "0");
                String nombreCruce = semaforo.asObject().getString("NombreCruce", "");

                Semaforo agenteSemaforo = new Semaforo(new AgentID("semaforo_" + nombre), id, tamCola, "cruce_" + nombreCruce);
                agenteSemaforo.start();
                if (tieneSensor.equals("1")) {
                    Simulacion.addSensor(/*id,*/ agenteSemaforo);
                }

            }

            Simulacion.lanzarVehiculos();
            //sim.simularTrafico();
        } catch (Exception e){
            //logger.error("Error " + e.getMessage());
        }
    }
}
