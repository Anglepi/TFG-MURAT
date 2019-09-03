package Models;

import es.upv.dsic.gti_ia.core.AgentID;

import java.io.FileWriter;

public class ModeloCruce {
    private String id;
    private String nombre;
    private String conversationID;
    private AgentID cruce;
    private int tDespeje;
    private FileWriter csv;

    public ModeloCruce(String id, String nombre, String conversationID, int tDespeje){
        this.id = id;
        this.nombre = nombre;
        this.conversationID = conversationID;
        this.cruce = new AgentID(this.nombre);
        this.tDespeje = tDespeje;
        try{
            this.csv = new FileWriter("informes/"+nombre+".csv");
            this.csv.append("Tiempo (minutos);Vehiculos simulados;Total Vehiculos;Vehiculos;Total Ocupancia (s);Ocupancia (s)\n");
        }catch (Exception e){
            System.out.println("Error apertura de informe");
        }
    }

    public String getNombre(){
        return nombre;
    }

    public String getConversationID(){
        return conversationID;
    }

    public AgentID getAgent(){
        return this.cruce;
    }

    public int getTiempoDespeje() { return this.tDespeje; }

    public FileWriter getCSV(){
        return csv;
    }

    public void cerrarInforme(){
        try{
            this.csv.flush();
            this.csv.close();
        }catch (Exception e){
            System.out.println("Excepción al cerrar informes");
        }
    }
}
