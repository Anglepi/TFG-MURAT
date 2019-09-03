package Agents;

import Models.ConfiguracionCruce;
import Models.Constantes;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import Controller.HttpController;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Cruce extends SingleAgent{
    private String idCruce, conversationID;
    private ArrayList<ConfiguracionCruce> configuraciones;
    private int tiempoRepartible;
    private ArrayList<AgentID> semaforos_suscritos;
    private int current_config_id;
    private int status;
    private int tDespeje;
    private AgentID smartCity;
    private Map<String, Pair<Integer, Long>> datos_semaforo;
    private int tiempoMinimo;
    public Cruce(AgentID aid, String id, String tDespeje) throws Exception{
        super(aid);
        this.idCruce = id;
        this.current_config_id = 0;
        this.tiempoRepartible = 0;
        this.tDespeje = Integer.parseInt(tDespeje);
        conversationID = "";
        semaforos_suscritos = new ArrayList<>();
        smartCity = new AgentID(Constantes.SMARTCITY_NOMBRE);
        datos_semaforo = new HashMap<>();
        tiempoMinimo = 8;
    }

    //Inicializa el Cruce en estado SUSCRIBIENDOSE
    public void init(){
        status = Constantes.CRUCE_INICIALIZANDO_CONFIGURACIONES;
        //System.out.println("CRUCE "+this.idCruce+" - Inicializando configuraciones");
    }

    public void execute(){
        boolean ejecutando = true;
        while(ejecutando) {
            switch (status) {
                case Constantes.CRUCE_INICIALIZANDO_CONFIGURACIONES:
                    inicializarConfiguraciones();
                    break;
                case Constantes.CRUCE_ESPERANDO_SEMAFOROS:
                    esperarSemaforos();
                    break;
                case Constantes.CRUCE_SUSCRIBIENDO_A_SMARTCITY:
                    suscribirse();
                    break;
                case Constantes.CRUCE_GESTIONANDO_SEMAFOROS:
                    rotaConfiguracion();
                    break;
                case Constantes.CRUCE_ANALIZANDO_DATOS:
                    analizarDatos();
                    break;
                case Constantes.CRUCE_FINALIZANDO:
                    finalizar();
                    ejecutando = false;
                    break;
            }
        }

    }

    //////////////INICIALIZANDO CONFIGURACIONES//////////////
    /*
    Realiza petición web al servidor indicando su ID de cruce para obtener todas sus configuraciones.
    Pasa a estado ESPERANDO_SEMAFOROS
     */
    private void inicializarConfiguraciones(){
        JsonArray jsonConfiguraciones = HttpController.obtenerListado(Constantes.LISTADO_CONFIGURACIONES,"idCruce="+this.idCruce);
        this.configuraciones = ConfiguracionCruce.inicializadorMultiple(jsonConfiguraciones);
        for(ConfiguracionCruce config: configuraciones){
            this.tiempoRepartible += (config.getTiempoEspera()-this.tiempoMinimo);
        }
        status = Constantes.CRUCE_ESPERANDO_SEMAFOROS;
    }
    //////////////FIN INICIALIZANDO CONFIGURACIONES//////////////

    //////////////ESPERANDO SEMAFOROS//////////////
    /*
    Espera las suscripciones de todos sus semáforos y los almacena. Cambia a SUSCRIBIENDO A SMARTCITY
     */
    private void esperarSemaforos(){
        int nSemaforos = configuraciones.get(0).getNumeroSemaforos();
        int semaforos_suscritos = 0;
        ACLMessage suscripcion;
        while(semaforos_suscritos < nSemaforos){
            suscripcion = receiveMessage();
            String senderAgentType = suscripcion.getSender().name.split("_")[0];
            if(senderAgentType.equals("semaforo") &&
                    suscripcion.getPerformativeInt() == ACLMessage.SUBSCRIBE){
                this.semaforos_suscritos.add(suscripcion.getSender());
                semaforos_suscritos++;
                iniciaSemaforo(suscripcion);
            }
        }
        //System.out.println("CRUCE "+this.idCruce+" - Suscribiendo a SmartCity");
        status = Constantes.CRUCE_SUSCRIBIENDO_A_SMARTCITY;
    }

    /*
    A partir del mensaje de suscripción de un semáforo, elabora una respuesta
    incluyendo el color inicial del semáforo
     */
    private void iniciaSemaforo(ACLMessage suscripcion){
        JsonObject content = new JsonObject();
        content.add("Resultado", "OK");
        content.add("Color", configuraciones.get(0).getConfig().get(suscripcion.getSender().name));
        content.add("Id", this.idCruce);
        content.add("TDespeje", this.tDespeje);
        ACLMessage confirmacion = construirMensajeSimple(suscripcion.getSender(), content.toString(), ACLMessage.INFORM);
        confirmacion.setInReplyTo(suscripcion.getReplyWith());
        send(confirmacion);
    }
    //////////////FIN ESPERANDO SEMAFOROS//////////////

    //////////////SUSCRIBIENDO A SMARTCITY//////////////
    /*
    Envia una suscripcion con su ID a la Smartcity, y espera su respuesta
    almacenando el conversationID proporcionado por ésta. Finalmente cambia
    a GESTIONANDO SEMAFOROS
     */
    private void suscribirse(){
        JsonObject contenido = new JsonObject();
        contenido.add("Id",this.idCruce);
        contenido.add("TDespeje", this.tDespeje);

        ACLMessage suscripcion = construirMensajeSimple(smartCity,contenido.toString(),ACLMessage.SUBSCRIBE);
        String replyWith = "suscripcion";
        suscripcion.setReplyWith(replyWith);
        send(suscripcion);

        boolean suscrito = false;
        ACLMessage respuesta;
        while(!suscrito){
            respuesta = receiveMessage();
            if(respuesta.getSender().name.equals(smartCity.name) &&
                    respuesta.getPerformativeInt() == ACLMessage.INFORM &&
                    respuesta.getInReplyTo().equals(replyWith)){
                this.conversationID = respuesta.getConversationId();
                suscrito = true;
            }
        }
        System.out.println("CRUCE "+this.idCruce+" - Gestionando semaforos");
        status = Constantes.CRUCE_GESTIONANDO_SEMAFOROS;
    }
    //////////////FIN SUSCRIBIENDO A SMARTCITY

    //////////////GESTIONANDO SEMAFOROS//////////////
    /*
    Tras esperar la cantidad de tiempo adecuada, cambia a la siguiente fase.
    Si durante este momento recibe una orden de informe de smartcity, pasa a
    ANALIZANDO DATOSy aprovecha para aplicar la heuristica de ajuste de tiempo de semaforos.
    Si recibe orden de finalizar, pasa directamente a estado FINALIZANDO
     */
    private void rotaConfiguracion(){
       int t = (int) Math.floor(configuraciones.get(current_config_id).getTiempoEspera());
       try{
           Thread.sleep((t*1000)/Constantes.TRANSFORMADOR_TIEMPO);
       }catch (InterruptedException e){
           System.out.println("EXCEPCION - Interrumpida la espera de cruce para cambiar fase");
       }
       current_config_id = (current_config_id+1)%configuraciones.size();
       Map<String, Integer> nueva_fase = configuraciones.get(current_config_id).getConfig();

       ACLMessage orden;
       JsonObject content;
       String replyWith = "cambio color";
       for(Map.Entry<String, Integer> semaforo_color : nueva_fase.entrySet()){
           content = new JsonObject();
           content.add("Color", semaforo_color.getValue());
           content.add("Orden", "Color");
           orden = construirMensajeSimple(new AgentID(semaforo_color.getKey()), content.toString(), ACLMessage.REQUEST);
           orden.setReplyWith(replyWith);
           send(orden);
       }

       int respuestas = 0;
       int totalSemaforos = semaforos_suscritos.size();
       boolean enviar_datos = false;
       ACLMessage respuesta;
       while(respuestas < totalSemaforos){
           respuesta = receiveMessage();
           String senderAgentType = respuesta.getSender().name.split("_")[0];
           if(senderAgentType.equals("semaforo") &&
                   respuesta.getPerformativeInt() == ACLMessage.INFORM &&
                   respuesta.getInReplyTo().equals(replyWith)){
               respuestas++;
           } else if(respuesta.getSender().name.equals(smartCity.name)){
               JsonObject contenido = Json.parse(respuesta.getContent()).asObject();
               String ordenSmartCity = contenido.getString("Orden", "ERROR");
               if(ordenSmartCity.equals("Informe") && status != Constantes.CRUCE_FINALIZANDO){
                   enviar_datos = true;
               } else if(ordenSmartCity.equals("Finalizar")){
                   enviar_datos = false;
                   System.out.println("CRUCE "+this.idCruce+" - Finalizando");
                   status = Constantes.CRUCE_FINALIZANDO;
                   break;
               }
           }
       }

       if(enviar_datos){
           ACLMessage agree = construirMensajeSimple(smartCity, "", ACLMessage.AGREE);
           send(agree);
           System.out.println("CRUCE "+this.idCruce+" - Analizando datos");
           status = Constantes.CRUCE_ANALIZANDO_DATOS;
       }
    }
    //////////////FIN GESTIONANDO SEMAFOROS//////////////

    //////////////ANALIZANDO DATOS//////////////
    /*
    Manda petición de datos a cada semaforo, después los recoge tanto a nivel
    de semaforo como a nivel de cruce y aprovecha para aplicar la heurística.
    Finalmente informa a SmartCity con los datos a nivel de cruce.
     */
    private void analizarDatos(){
        ACLMessage msg_orden;
        JsonObject orden = new JsonObject();
        orden.add("Orden", "Informe");
        String replyWith = "datos";
        for(AgentID semaforo : semaforos_suscritos){
            msg_orden = construirMensajeSimple(semaforo, orden.toString(), ACLMessage.REQUEST);
            msg_orden.setReplyWith(replyWith);
            send(msg_orden);
        }

        int n_datos_recibidos = 0;
        int n_semaforos = semaforos_suscritos.size();
        ACLMessage msg_datos;
        int totalVehiculos = 0;
        long totalOcupancia = 0;
        int vehiculos_semaforo = 0;
        long ocupancia_semaforo = 0;
        JsonObject respuesta;
        while(n_datos_recibidos < n_semaforos){
            msg_datos = receiveMessage();
            String senderAgentType = msg_datos.getSender().name.split("_")[0];
            if(senderAgentType.equals("semaforo") &&
                    msg_datos.getPerformativeInt() == ACLMessage.INFORM &&
                    msg_datos.getInReplyTo().equals(replyWith)){
                respuesta = Json.parse(msg_datos.getContent()).asObject();
                vehiculos_semaforo = respuesta.getInt("NVehiculos", 0);
                ocupancia_semaforo = respuesta.getLong("Ocupancia", 0);
                datos_semaforo.put(msg_datos.getSender().name, new Pair<Integer, Long>(vehiculos_semaforo, ocupancia_semaforo));
                totalVehiculos += vehiculos_semaforo;
                totalOcupancia += ocupancia_semaforo;
                n_datos_recibidos++;
            }
        }

        asignarTiemposCiclo(totalVehiculos, totalOcupancia);

        respuesta = new JsonObject();
        respuesta.add("NVehiculos", totalVehiculos);
        respuesta.add("Ocupancia", totalOcupancia);
        ACLMessage informe_smartcity = construirMensajeSimple(smartCity,respuesta.toString(),ACLMessage.INFORM);
        informe_smartcity.setInReplyTo("informe");
        send(informe_smartcity);
        System.out.println("CRUCE "+this.idCruce+" - Gestionando semaforos");
        status = Constantes.CRUCE_GESTIONANDO_SEMAFOROS;
    }

    /*
    Utilizando los datos calculados en analizarDatos, en funcion del total de vehiculos
    y total de ocupancia de cada semaforo, calcula unas puntuaciones a cada semaforo, y a partir
    de dichas puntuaciones, asigna un tiempo a cada fase
     */
    private void asignarTiemposCiclo(int total_vehiculos, long total_ocupancia){
        float cteVehiculos = 0.3f;
        float cteOcupancia = 0.7f;
        double puntuacionTotal = 0;
        Map<String, Double> puntuaciones_semaforos = new HashMap<>();
        Map<ConfiguracionCruce, Double> puntuaciones_fases = new HashMap<>();

        for(AgentID sem: semaforos_suscritos){
            int vehiculos = datos_semaforo.get(sem.name).getKey();
            float ocupancia = datos_semaforo.get(sem.name).getValue();
            double puntuacion = cteVehiculos*vehiculos+cteOcupancia*ocupancia;
            puntuaciones_semaforos.put(sem.name, puntuacion);
        }

        double puntuacion;
        for(ConfiguracionCruce fase : configuraciones){
            puntuacion = 0;
            for(Map.Entry<String, Integer> sem_est : fase.getConfig().entrySet()){
                if(sem_est.getValue() == Constantes.VERDE || sem_est.getValue() == Constantes.AMBAR_INTERMITENTE){
                    puntuacion += puntuaciones_semaforos.get(sem_est.getKey());
                }
            }
            puntuaciones_fases.put(fase, puntuacion);
            puntuacionTotal += puntuacion;
        }

        for(ConfiguracionCruce fase : configuraciones){
            double t = ((puntuaciones_fases.get(fase)/puntuacionTotal)*this.tiempoRepartible)+this.tiempoMinimo;
            if(this.idCruce.equals("2")) System.out.println("Asignados "+t+" segundos a "+fase.getNombreConfiguracion());
            fase.setTiempoEspera(t);
        }

        datos_semaforo = new HashMap<>();
    }

    //////////////FIN ANALIZANDO DATOS//////////////

    //////////////FINALIZANDO//////////////
    /*
    Envia una orden de finalizar a todos sus semaforos y espera sus respuestas
     */
    private void finalizar(){
        ACLMessage orden;
        JsonObject content = new JsonObject();
        content.add("Orden","Finalizar");
        String replyWith = "finalizar";
        System.out.println("Cruce " +this.idCruce+" finalizando");
        for(AgentID semaforo : semaforos_suscritos){
            orden = construirMensajeSimple(semaforo,content.toString(),ACLMessage.REQUEST);
            orden.setReplyWith(replyWith);
            send(orden);
        }

        System.out.println("Cruce " +this.idCruce+" fin de envios, esperando respuestas");

        int finalizados = 0;
        int n_semaforos = semaforos_suscritos.size();
        ACLMessage informe;
        while(finalizados < n_semaforos){
            informe = receiveMessage();
            String senderAgentType = informe.getSender().name.split("_")[0];
            if(senderAgentType.equals("semaforo") &&
                    informe.getPerformativeInt() == ACLMessage.INFORM &&
                    informe.getInReplyTo().equals(replyWith)){
                finalizados++;
            }
        }

        System.out.println("Cruce " +this.idCruce+" respuestas recibidas, avisando a smartcity");

        content = new JsonObject();
        content.add("Resultado", "OK");
        informe = construirMensajeSimple(smartCity,content.toString(),ACLMessage.INFORM);
        send(informe);
    }
    //////////////FIN FINALIZANDO//////////////

    //////////////UTILIDADES//////////////
    /*
    Construye un mensaje ACL con la configuración y parámetros básicos
     */
    private ACLMessage construirMensajeSimple(AgentID destino, String contenido, int performativa){
        ACLMessage mensaje = new ACLMessage();
        mensaje.setSender(this.getAid());
        mensaje.setReceiver(destino);
        mensaje.setPerformative(performativa);
        mensaje.setContent(contenido);
        mensaje.setConversationId(this.conversationID);

        return mensaje;
    }

    /*
    Recibe un mensaje ACL.
     */
    private ACLMessage receiveMessage(){
        ACLMessage inbox = null;

        try {
            inbox = receiveACLMessage();

        } catch (InterruptedException e) {
            //System.err.println("Error al recibir mensaje en receiveMessage de cruce");
        }

        return inbox;
    }


}
