package Agents;

import Models.Constantes;
import Models.ModeloCruce;
import Simulation.Simulacion;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;
import javafx.util.Pair;

import java.util.ArrayList;

import static java.lang.Math.abs;

public class SmartCity extends SingleAgent {
    private ArrayList<ModeloCruce> cruces;
    private int status;
    private int totalCruces;
    private int periodo_generacion_informes;
    private int iteracion_informe;
    private int total_iteraciones;
    private ArrayList<Pair<Integer, Integer>> vehiculos_horas;

    public SmartCity(AgentID aid, int totalCruces) throws Exception{
        super(aid);
        cruces = new ArrayList<>();
        this.totalCruces = totalCruces;
        this.periodo_generacion_informes = 15;
        this.iteracion_informe=0;
        this.total_iteraciones=96;
        inicializaVehiculosHoras();
    }

    /*
    Inicializa SmartCity en modo escuchando.
     */
    public void init(){
        System.out.println("SMARTCITY - Gestionando suscripciones");
        status = Constantes.SMARTCITY_GESTIONANDO_SUSCRIPCIONES;
    }

    public void execute(){
        boolean ejecutando = true;

        while(ejecutando){
            switch (status){
                case Constantes.SMARTCITY_GESTIONANDO_SUSCRIPCIONES:
                    gestionarSuscripciones();
                    break;
                case Constantes.SMARTCITY_GENERANDO_INFORMES:
                    generarInforme();
                    break;
                case Constantes.SMARTCITY_FINALIZANDO:
                    finalizarAgentes();
                    ejecutando = false;
                    break;
            }
        }
    }


    ////////////GESTIONANDO SUSCRIPCIONES////////////
    /*
    Recibe un mensaje: si es una suscripcion de cruce, procesa el mensaje, almacena el cruce y le responde con AGREE
    Si todos los cruces se han suscrito, manda un INFORM a cada uno y cambia a GENERANDO INFORMES
     */
    private void gestionarSuscripciones(){
        ACLMessage msg = receiveMessage();
        String senderAgentType = msg.getSender().name.split("_")[0];
        JsonObject ok = new JsonObject();
        ok.add("Resultado", "OK");

        if(senderAgentType.equals("cruce") &&
                msg.getPerformativeInt() == ACLMessage.SUBSCRIBE &&
                msg.getReplyWith().equals("suscripcion")){
            JsonObject contenido = Json.parse(msg.getContent()).asObject();
            String idCruce = contenido.getString("Id", "0");
            String cid = msg.getSender().name + generarRandomString();
            int tDespeje = contenido.getInt("TDespeje", 0);
            ModeloCruce nuevoCruce = new ModeloCruce(idCruce, msg.getSender().name, cid, tDespeje);
            cruces.add(nuevoCruce);
            ACLMessage agree = construirMensajeSimple(msg.getSender(), nuevoCruce.getConversationID(), ok.toString(), ACLMessage.AGREE);
            agree.setInReplyTo(msg.getReplyWith());
            send(agree);
        }

        if(cruces.size() == totalCruces){
            for(ModeloCruce mc : cruces){
                ACLMessage inform = construirMensajeSimple(mc.getAgent(), mc.getConversationID(), ok.toString(), ACLMessage.INFORM);
                inform.setInReplyTo("suscripcion");
                send(inform);
            }
            System.out.println("SMARTCITY - Generando informes");
            status = Constantes.SMARTCITY_GENERANDO_INFORMES;
        }

    }
    ////////////FIN GESTIONANDO SUSCRIPCIONES////////////

    ////////////GENERANDO INFORMES////////////
    /*
    Espera el periodo de generacion de informes, tras lo cual manda un REQUEST a cada cruce,
    tras lo cual espera el INFORM de cada cruce. Para cada INFORM recibido añade una linea al csv.

    Si era el ultimo informe, cambia a FINALIZANDO
     */
    private void generarInforme(){
        System.out.println("SmartCity - Esperando para generar informe "+this.iteracion_informe);
        actualizaVehiculos();
        try{
            Thread.sleep((this.periodo_generacion_informes*60*1000)/Constantes.TRANSFORMADOR_TIEMPO);
        }catch(InterruptedException e){
            System.out.println("EXCEPCION - SmartCity interrumpida en la espera de generacion de informe");
        }

        ACLMessage orden;
        JsonObject content = new JsonObject();
        content.add("Orden", "Informe");

        int informes_recibidos = 0;
        for(ModeloCruce mc : cruces){
            orden = construirMensajeSimple(mc.getAgent(), mc.getConversationID(), content.toString(), ACLMessage.REQUEST);
            orden.setReplyWith("informe");
            send(orden);
        }

        ACLMessage informe;
        String senderAgentType;
        while(informes_recibidos < cruces.size()){
            informe = receiveMessage();
            senderAgentType = informe.getSender().name.split("_")[0];
            if(senderAgentType.equals("cruce") &&
                    informe.getPerformativeInt() == ACLMessage.INFORM &&
                    informe.getInReplyTo().equals("informe")){
                JsonObject datos = Json.parse(informe.getContent()).asObject();
                int nVehiculos = datos.getInt("NVehiculos", 0);
                long ocupancia = datos.getLong("Ocupancia", 0);

                generarInforme(informe.getSender().name, nVehiculos, ocupancia);
                informes_recibidos++;
            }
        }
        iteracion_informe++;
        if(iteracion_informe > total_iteraciones){
            System.out.println("SmartCity - Finalizando");
            status = Constantes.SMARTCITY_FINALIZANDO;
        }
    }

    /*
    A partir del nombre de cruce, un numero de vehiculos y una ocupancia,
    busca al cruce correspondiente y añade a su informe una linea
     */
    private void generarInforme(String cruce, int vehiculos, long ocupancia){
        for(ModeloCruce mc : cruces){
            if(mc.getNombre().equals(cruce)){
                try {
                    // A-Iteracion;B-Vehiculos Simulados;C-TotalVehiculos
                    int total_minutos = iteracion_informe*periodo_generacion_informes;
                    int horas = total_minutos/60;
                    int minutos = total_minutos%60;
                    mc.getCSV().append(horas+":"+minutos+";"+ Simulacion.vehiculosCirculando+";"+vehiculos);
                    // D-Vehiculos (15 min);E-Ocupancia total
                    mc.getCSV().append(";=C"+(iteracion_informe+2)+"-C"+(iteracion_informe+1)+";=("+ocupancia+"/1000)*"+Constantes.TRANSFORMADOR_TIEMPO);
                    // F-Ocupancia (15 min)
                    mc.getCSV().append(";=E"+(iteracion_informe+2)+"-E"+(iteracion_informe+1)+"\n");
                } catch(Exception e){
                    System.out.println("Error al escribir en el informe");
                }
            }
        }
    }

    ////////////FIN GENERANDO INFORMES////////////

    ////////////FINALIZANDO////////////
    /*
    Manda una REQUEST con orden finalizar a cada cruce, y espera a recibir los INFORM de cada cruce, tras lo
    cual finaliza la ejecucion del agente
     */
    private void finalizarAgentes(){
        ACLMessage orden;
        JsonObject contenido = new JsonObject();
        contenido.add("Orden", "Finalizar");
        for(ModeloCruce mc : cruces){
            orden = construirMensajeSimple(mc.getAgent(), mc.getConversationID(), contenido.toString(), ACLMessage.REQUEST);
            send(orden);
            mc.cerrarInforme();
        }

        ACLMessage informe;
        int informes_recibidos = 0;
        while(informes_recibidos < cruces.size()){
            informe = receiveMessage();
            String senderAgentType = informe.getSender().name.split("_")[0];
            String resultado = Json.parse(informe.getContent()).asObject().getString("Resultado", "ERROR");
            if(senderAgentType.equals("cruce") && resultado.equals("OK")){
                informes_recibidos++;
                //System.out.println("Smartcity recibe fin de "+informe.getSender());
            }
        }
    }
    ////////////FIN FINALIZANDO////////////

    ////////////UTILIDADES////////////
    /*
    Construye un mensaje ACL simple con parámetros básicos.
     */
    private ACLMessage construirMensajeSimple(AgentID destino, String conversationid, String contenido, int performativa){
        ACLMessage mensaje = new ACLMessage();
        mensaje.setSender(this.getAid());
        mensaje.setReceiver(destino);
        mensaje.setPerformative(performativa);
        mensaje.setContent(contenido);
        mensaje.setConversationId(conversationid);

        return mensaje;
    }

    /*
    Genera un String aleatorio (usado para no tener conversationID repetidos)
     */
    private String generarRandomString(){
        String caracteres = "ABCDEFGHIJKLMNÑOPQRSTUVWXYZabcdefghijklmnñopqrstuvxyz0123456789";

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 7; i++) {
            int indice = (int)(caracteres.length() * Math.random());
            sb.append(caracteres.charAt(indice));
        }

        return sb.toString();
    }

    /*
    Recibe un mensaje ACL
     */
    private ACLMessage receiveMessage(){
        ACLMessage inbox = null;

        try {
            inbox = receiveACLMessage();

        } catch (InterruptedException e) {
            System.err.println("Error al recibir mensaje en receiveMessage de smartcity");
        }

        return inbox;
    }

    private void inicializaVehiculosHoras(){
        this.vehiculos_horas = new ArrayList<>();
        /*//Puede valer (picos mas altos)
        this.vehiculos_horas.add(new Pair<>(5,  8)); //0145 - 8
        this.vehiculos_horas.add(new Pair<>(2, 17)); //0400 - 17
        this.vehiculos_horas.add(new Pair<>(8, 21)); //0500 - 21
        this.vehiculos_horas.add(new Pair<>(20, 27)); //0630 - 27
        this.vehiculos_horas.add(new Pair<>(95, 33)); //0800 - 37
        this.vehiculos_horas.add(new Pair<>(150, 37)); //0900 - 37
        this.vehiculos_horas.add(new Pair<>(110, 41)); //1000 - 41
        this.vehiculos_horas.add(new Pair<>(70, 45)); //1100 - 41
        this.vehiculos_horas.add(new Pair<>(65, 51)); //1230 - 51
        this.vehiculos_horas.add(new Pair<>(55, 53)); //1300 - 53
        this.vehiculos_horas.add(new Pair<>(80, 57)); //1400 - 57
        this.vehiculos_horas.add(new Pair<>(130, 64)); //1545 - 64
        this.vehiculos_horas.add(new Pair<>(70, 69)); //1700 - 69
        this.vehiculos_horas.add(new Pair<>(55, 72)); //1745 - 72
        this.vehiculos_horas.add(new Pair<>(60, 75)); //1830 - 75
        this.vehiculos_horas.add(new Pair<>(70, 80)); //1930 - 79
        this.vehiculos_horas.add(new Pair<>(80, 82)); //2000 - 81
        this.vehiculos_horas.add(new Pair<>(100, 84)); //2030 - 81
        this.vehiculos_horas.add(new Pair<>(70, 82)); //2100 - 81
        this.vehiculos_horas.add(new Pair<>(35, 87)); //2130 - 87
        this.vehiculos_horas.add(new Pair<>(25, 95)); //2330 - 95
        this.vehiculos_horas.add(new Pair<>(15, 97)); //0000 - 97
*/
         //Modelo: Puede valer
        this.vehiculos_horas.add(new Pair<>(5,  8)); //0145 - 8
        this.vehiculos_horas.add(new Pair<>(2, 17)); //0400 - 17
        this.vehiculos_horas.add(new Pair<>(8, 21)); //0500 - 21
        this.vehiculos_horas.add(new Pair<>(20, 27)); //0630 - 27
        this.vehiculos_horas.add(new Pair<>(95, 33)); //0800 - 37
        this.vehiculos_horas.add(new Pair<>(110, 37)); //0900 - 37
        this.vehiculos_horas.add(new Pair<>(100, 41)); //1000 - 41
        this.vehiculos_horas.add(new Pair<>(70, 45)); //1100 - 41
        this.vehiculos_horas.add(new Pair<>(65, 51)); //1230 - 51
        this.vehiculos_horas.add(new Pair<>(55, 53)); //1300 - 53
        this.vehiculos_horas.add(new Pair<>(80, 57)); //1400 - 57
        this.vehiculos_horas.add(new Pair<>(105, 64)); //1545 - 64
        this.vehiculos_horas.add(new Pair<>(65, 69)); //1700 - 69
        this.vehiculos_horas.add(new Pair<>(55, 72)); //1745 - 72
        this.vehiculos_horas.add(new Pair<>(60, 75)); //1830 - 75
        this.vehiculos_horas.add(new Pair<>(70, 80)); //1930 - 79
        this.vehiculos_horas.add(new Pair<>(75, 82)); //2000 - 81
        this.vehiculos_horas.add(new Pair<>(80, 84)); //2030 - 81
        this.vehiculos_horas.add(new Pair<>(60, 82)); //2100 - 81
        this.vehiculos_horas.add(new Pair<>(35, 87)); //2130 - 87
        this.vehiculos_horas.add(new Pair<>(25, 95)); //2330 - 95
        this.vehiculos_horas.add(new Pair<>(15, 97)); //0000 - 97

        /*//Modelo: fin de semana
        this.vehiculos_horas.add(new Pair<>(13,  8)); //0145 - 8
        this.vehiculos_horas.add(new Pair<>(5, 17)); //0400 - 17
        this.vehiculos_horas.add(new Pair<>(5, 21)); //0500 - 21
        this.vehiculos_horas.add(new Pair<>(20, 27)); //0630 - 27
        this.vehiculos_horas.add(new Pair<>(25, 33)); //0800 - 37
        this.vehiculos_horas.add(new Pair<>(30, 37)); //0900 - 37
        this.vehiculos_horas.add(new Pair<>(28, 41)); //1000 - 41
        this.vehiculos_horas.add(new Pair<>(20, 45)); //1100 - 41
        this.vehiculos_horas.add(new Pair<>(35, 51)); //1230 - 51
        this.vehiculos_horas.add(new Pair<>(30, 53)); //1300 - 53
        this.vehiculos_horas.add(new Pair<>(18, 57)); //1400 - 57
        this.vehiculos_horas.add(new Pair<>(15, 64)); //1545 - 64
        this.vehiculos_horas.add(new Pair<>(26, 69)); //1700 - 69
        this.vehiculos_horas.add(new Pair<>(30, 72)); //1745 - 72
        this.vehiculos_horas.add(new Pair<>(35, 75)); //1830 - 75
        this.vehiculos_horas.add(new Pair<>(30, 80)); //1930 - 79
        this.vehiculos_horas.add(new Pair<>(35, 82)); //2000 - 81
        this.vehiculos_horas.add(new Pair<>(40, 84)); //2030 - 81
        this.vehiculos_horas.add(new Pair<>(35, 82)); //2100 - 81
        this.vehiculos_horas.add(new Pair<>(30, 87)); //2130 - 87
        this.vehiculos_horas.add(new Pair<>(20, 95)); //2330 - 95
        this.vehiculos_horas.add(new Pair<>(15, 97)); //0000 - 97
        */
    }

    private void actualizaVehiculos(){
        boolean siguiente_encontrado = false;
        for(int i=0 ; i<vehiculos_horas.size() && !siguiente_encontrado ; i++){
            Pair<Integer, Integer> actual = vehiculos_horas.get(i);
            if(actual.getValue() > iteracion_informe){
                int iteraciones_reestantes = actual.getValue() - iteracion_informe;
                //System.out.println("Iteraciones reestantes: "+iteraciones_reestantes);
                int nVehiculos_objetivo = (int) (actual.getKey());
                //System.out.println("vehiculos objetivo: "+nVehiculos_objetivo);
                int nVehiculos_actual = Simulacion.maxVehiculos;
                //System.out.println("vehiculos actual: "+nVehiculos_actual);
                int cambio_nVehiculos = (nVehiculos_objetivo-nVehiculos_actual)/iteraciones_reestantes;
                Simulacion.setTotalVehiculos(nVehiculos_actual+cambio_nVehiculos);
                System.out.println("Estableciendo maximo: "+Simulacion.maxVehiculos+" - Hay circulando: "+Simulacion.vehiculosCirculando);
                siguiente_encontrado = true;
            }
        }
    }

}


