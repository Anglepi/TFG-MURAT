package Agents;

import Controller.HttpController;
import Models.Constantes;
import Models.ModeloCruce;
import Simulation.Simulacion;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;
import Interfaces.iDetector;
import javafx.util.Pair;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Semaforo extends SingleAgent implements iDetector{
    private String id;
    private int color;
    private ModeloCruce cruce;
    private int status;
    private AgentID smartCity;
    private ArrayList<AgentID> semaforos_suscritos = new ArrayList<>();
    private Map<AgentID, Pair<Integer, Integer>> semaforos_destino = new HashMap<>();
    private int totalProbabilidad;
    private int salida;
    private int nVehiculos;
    private int vehiculos_transcurridos;
    private int nMaxVehiculos;
    private boolean atasco;
    private long inicio_atasco;
    private long ocupancia;
    private String nombreCruce;
    private boolean suscrito;
    private Timer timerEnvioVehiculos;
    private boolean mandandoVehiculos;

    public Semaforo(AgentID aid, String id, String nMaxVehiculos, String nombreCruce) throws Exception{
        super(aid);
        this.id = id;
        smartCity = new AgentID(Constantes.SMARTCITY_NOMBRE);
        this.nVehiculos = 0;
        this.nMaxVehiculos = Integer.parseInt(nMaxVehiculos);
        this.salida = 0;
        atasco = false;
        totalProbabilidad = 0;
        this.nombreCruce = nombreCruce;
        this.suscrito = false;
        this.vehiculos_transcurridos = 0;
        this.mandandoVehiculos = false;
    }

    /*
    Inicializa el Semáforo sin color (APAGADO) y en estado DURMIENDO
     */
    public void init(){
        color = Constantes.APAGADO;
        status = Constantes.SEMAFORO_CARGANDO_DESTINOS;
        //System.out.println("SEMAFORO "+this.id+" - Cargando destinos");
    }

    public void execute(){
        boolean ejecutando = true;
        while(ejecutando){
            switch (status){
                case Constantes.SEMAFORO_CARGANDO_DESTINOS:
                    cargarDestinos();
                    break;
                case Constantes.SEMAFORO_SUSCRIBIENDO_A_ORIGENES:
                    suscribirseAOrigenes();
                    break;
                case Constantes.SEMAFORO_SUSCRIBIENDO_A_CRUCE:
                    suscribirseACruce();
                    break;
                case Constantes.SEMAFORO_GESTIONANDO_TRAFICO:
                    gestionarTrafico();
                    break;
                case Constantes.SEMAFORO_FINALIZANDO:
                    ejecutando = false;
                    break;
            }
        }
    }
    ////////////CARGANDO DESTINOS////////////
    private void cargarDestinos(){
        JsonArray semaforos_destino = HttpController.obtenerListado(Constantes.LISTADO_DESTINOS_SEMAFORO, "idSemaforo="+this.id);

        int probabilidad = 0;
        for(JsonValue semaforo : semaforos_destino){
            probabilidad = Integer.parseInt(semaforo.asObject().getString("ProporcionVehiculos", "0"));

            if(semaforo.asObject().getString("Destino", "0").equals("0")){
                salida = probabilidad;
            } else {
                AgentID agenteSemaforo = new AgentID("semaforo_"+semaforo.asObject().getString("Nombre", ""));
                Integer proporcionVehiculos = probabilidad;
                this.semaforos_destino.put(agenteSemaforo, new Pair<>(totalProbabilidad, proporcionVehiculos+totalProbabilidad));
            }

            this.totalProbabilidad += probabilidad;
        }
        //System.out.println("SEMAFORO "+this.id+" - Suscribiendo a origenes");
        status = Constantes.SEMAFORO_SUSCRIBIENDO_A_ORIGENES;
    }
    ////////////FIN CARGANDO DESTINOS////////////

    ////////////SUSCRIBIENDO A ORIGENES////////////
    private void suscribirseAOrigenes(){
        JsonArray semaforos_origen = HttpController.obtenerListado(Constantes.LISTADO_ORIGENES_SEMAFORO, "idSemaforo="+this.id);
        ACLMessage suscripcion;
        String replyWith = "suscripcion";
        for (JsonValue semaforo : semaforos_origen) {
            AgentID agenteSemaforo = new AgentID("semaforo_"+semaforo.asObject().getString("Nombre", ""));
            suscripcion = construirMensajeSimple(agenteSemaforo, "", ACLMessage.SUBSCRIBE);
            suscripcion.setReplyWith(replyWith);
            send(suscripcion);
        }

        int suscripciones = 0;
        int informes = 0;
        int destinos = semaforos_destino.size();
        int origenes = semaforos_origen.size();
        ACLMessage respuesta;

        while((suscripciones < destinos) || (informes < origenes)){
            respuesta = receiveMessage();
            String senderAgentType = respuesta.getSender().name.split("_")[0];
            if(senderAgentType.equals("semaforo")){
                if(respuesta.getPerformativeInt() == ACLMessage.SUBSCRIBE &&
                        respuesta.getReplyWith().equals(replyWith)){
                    semaforos_suscritos.add(respuesta.getSender());
                    suscripciones++;
                    ACLMessage informe = construirMensajeSimple(respuesta.getSender(),new JsonObject().add("Resultado", "OK").toString(), ACLMessage.INFORM);
                    informe.setInReplyTo(respuesta.getReplyWith());
                    send(informe);
                } else if(respuesta.getPerformativeInt() == ACLMessage.INFORM &&
                        respuesta.getInReplyTo().equals(replyWith)){
                    informes++;
                }
            }
        }


        //System.out.println("SEMAFORO "+this.id+" - Suscribiendo a cruce");
        status = Constantes.SEMAFORO_SUSCRIBIENDO_A_CRUCE;
    }
    ////////////FIN SUSCRIBIENDO A ORIGENES////////////

    ////////////SUSCRIBIENDO A CRUCE////////////
    private void suscribirseACruce(){
        ACLMessage suscripcion = construirMensajeSimple(new AgentID(this.nombreCruce), "", ACLMessage.SUBSCRIBE);
        String replyWith = "suscripcion";
        suscripcion.setReplyWith(replyWith);
        send(suscripcion);
        while(!suscrito){
            ACLMessage respuesta = receiveMessage();
            String senderAgentType = respuesta.getSender().name.split("_")[0];
            if(senderAgentType.equals("cruce") &&
                    respuesta.getPerformativeInt() == ACLMessage.INFORM &&
                    respuesta.getInReplyTo().equals(replyWith)){
                JsonObject content = Json.parse(respuesta.getContent()).asObject();
                String resultado = content.getString("Resultado", "ERROR");
                int color = content.getInt("Color", 0);
                String id_cruce = content.getString("Id", "0");
                int t_despeje = content.getInt("TDespeje", 0);
                if(resultado.equals("OK")){
                    this.color = color;
                    this.cruce = new ModeloCruce(id_cruce,this.nombreCruce,respuesta.getConversationId(), t_despeje);
                    suscrito = true;
                }
            }
        }

        //System.out.println("SEMAFORO "+this.id+" - Gestionando trafico");
        gestionaEnvioVehiculos();
        status = Constantes.SEMAFORO_GESTIONANDO_TRAFICO;
    }
    ////////////FIN SUSCRIBIENDO A CRUCE////////////

    ////////////GESTIONANDO TRAFICO////////////
    private void gestionarTrafico(){
        ACLMessage mensaje = receiveMessage();
        String senderAgentType = mensaje.getSender().name.split("_")[0];
        if(senderAgentType.equals("cruce") && mensaje.getPerformativeInt() == ACLMessage.REQUEST){
            JsonObject content = Json.parse(mensaje.getContent()).asObject();
            String orden = content.getString("Orden", "ERROR");
            if(orden.equals("Color")){
                ACLMessage respuesta = construirMensajeSimple(cruce.getAgent(), "", ACLMessage.AGREE);
                respuesta.setInReplyTo(mensaje.getReplyWith());
                send(respuesta);
                int color = content.getInt("Color", 0);
                if(color == Constantes.ROJO){
                    this.color = Constantes.AMBAR_FIJO;
                } else {
                    try{
                        Thread.sleep((cruce.getTiempoDespeje()*1000)/Constantes.TRANSFORMADOR_TIEMPO);
                    }catch (InterruptedException e){
                        System.out.println("EXCEPCION - Interrumpida la espera de tiempo de despeje del semaforo");
                    }
                }
                try{
                    Thread.sleep((3*1000)/Constantes.TRANSFORMADOR_TIEMPO);
                }catch(InterruptedException e){
                    System.out.println("EXCEPCION - Interrumpida la espera de cambio de color del semaforo");
                }

                this.color = color;
                gestionaEnvioVehiculos();
                respuesta.setPerformative(ACLMessage.INFORM);
                send(respuesta);

            } else if(orden.equals("Informe")){
                informaDatos();
            } else if(orden.equals("Finalizar")){
                System.out.println("Semaforo finalizando: "+this.id);
                status = Constantes.SEMAFORO_FINALIZANDO;
                ACLMessage informeFin = construirMensajeSimple(cruce.getAgent(), "", ACLMessage.INFORM);
                informeFin.setInReplyTo(mensaje.getReplyWith());
                pausarEnvioVehiculos();
                send(informeFin);
            }

        } else if(senderAgentType.equals("semaforo")){
            int nVehiculos = Json.parse(mensaje.getContent()).asObject().getInt("NVehiculos", 0);
            this.nVehiculos += nVehiculos;
        }
    }

    private void gestionaEnvioVehiculos(){
        if(this.color == Constantes.VERDE || this.color == Constantes.AMBAR_INTERMITENTE){
            comenzarEnvioVehiculos();
            if(atasco){
                ocupancia += System.currentTimeMillis() - inicio_atasco;
            }
            atasco = false;
        } else if(this.color == Constantes.ROJO){
            pausarEnvioVehiculos();
            if(nVehiculos >= nMaxVehiculos && !atasco){
                inicio_atasco = System.currentTimeMillis();
                atasco = true;
            }
        }
    }

    private void comenzarEnvioVehiculos(){
        if(!mandandoVehiculos) {
            this.timerEnvioVehiculos = new Timer();
            TimerTask tareaEnvioVehiculos = new TimerTask() {
                @Override
                public void run() {
                    enviarVehiculo();
                }
            };
            this.timerEnvioVehiculos.schedule(tareaEnvioVehiculos, 0, 1500 / Constantes.TRANSFORMADOR_TIEMPO);
            mandandoVehiculos = true;
        }
    }

    private void pausarEnvioVehiculos(){
        if(mandandoVehiculos) {
            this.timerEnvioVehiculos.cancel();
            mandandoVehiculos = false;
        }
    }

    private void enviarVehiculo(){
        if(nVehiculos > 0) {
            JsonObject content = new JsonObject();
            content.add("NVehiculos", 1);
            AgentID semaforo = destinoVehiculo();
            if (!semaforo.name.equals("salida")) {
                ACLMessage vehiculo = construirMensajeSimple(semaforo, content.toString(), ACLMessage.INFORM);
                vehiculo.setConversationId("envio vehiculo");
                send(vehiculo);
                if (this.id.equals("3")) System.out.println("Semaforo " + this.id + " envia vehiculo");
            } else {
                Simulacion.salidaVehiculo();
            }
            nVehiculos--;
            vehiculos_transcurridos++;
        }
    }

    private AgentID destinoVehiculo(){
        //System.out.println(this.getName()+" PASO 6");
        AgentID destino = new AgentID("salida");
        int valorAleatorio = ThreadLocalRandom.current().nextInt(0, totalProbabilidad);

        for(Map.Entry<AgentID, Pair<Integer,Integer>> entry : semaforos_destino.entrySet()){

            if(valorAleatorio >= entry.getValue().getKey() && valorAleatorio < entry.getValue().getValue()){
                destino = entry.getKey();
            }
        }

        //if(!semaforos_suscritos.contains(destino)) destino = new AgentID("salida");
        return destino;
    }

    private void informaDatos(){
        JsonObject content = new JsonObject();
        content.add("NVehiculos", vehiculos_transcurridos+(nVehiculos<nMaxVehiculos?nVehiculos:nMaxVehiculos));
        content.add("Ocupancia", ocupancia);
        ACLMessage informe = construirMensajeSimple(cruce.getAgent(),content.toString(),ACLMessage.INFORM);
        informe.setInReplyTo("datos");
        send(informe);
    }
    ////////////FIN GESTIONANDO TRAFICO////////////

    ////////////IDETECTOR////////////
    public void detectorPasoPeatones(){

    }

    public void pasaVehiculo(){
        nVehiculos++;
    }
    ////////////FIN IDETECTOR////////////
    /*
    Construye un mensaje ACL simple con parámetros básicos.
     */
    private ACLMessage construirMensajeSimple(AgentID destino, String contenido, int performativa){
        ACLMessage mensaje = new ACLMessage();
        mensaje.setSender(this.getAid());
        mensaje.setReceiver(destino);
        mensaje.setPerformative(performativa);
        mensaje.setContent(contenido);
        if(suscrito && destino.name.split("_")[0].equals("cruce"))
            mensaje.setConversationId(cruce.getConversationID());

        return mensaje;
    }

    /*
    Recibe un mensaje ACL
     */
    private ACLMessage receiveMessage(){
        ACLMessage inbox = null;

        try {
            inbox = receiveACLMessage();
        } catch (InterruptedException e) {
            System.err.println("Error al recibir mensaje en receiveMessage de cruce");
        }

        return inbox;
    }


}
