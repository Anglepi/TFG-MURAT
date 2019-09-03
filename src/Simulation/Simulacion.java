package Simulation;

import Interfaces.iDetector;
import Models.Constantes;

import java.util.ArrayList;


public class Simulacion{

    public static int vehiculosCirculando = 0;
    public static int maxVehiculos;
    public static ArrayList<iDetector> detectores = new ArrayList<>();
    public static int iterDetectores = 0;

    public static void addSensor(iDetector sensor){
        detectores.add(sensor);
    }

    public static synchronized void lanzarVehiculos(){
        int nDetectores = detectores.size();
        while(vehiculosCirculando<maxVehiculos){
            try {
                Thread.sleep(1200/Constantes.TRANSFORMADOR_TIEMPO);
            } catch (InterruptedException e){
                System.out.println("SIMULACION - EXCEPCION: Espera interrumpida");
            }

            if(iterDetectores==nDetectores) iterDetectores=0;
            detectores.get(iterDetectores).pasaVehiculo();
            vehiculosCirculando++;
            //System.out.println("Entra vehiculo, hay "+vehiculosCirculando+"/"+maxVehiculos);
            iterDetectores++;
        }
    }

    public static synchronized void salidaVehiculo(){
        vehiculosCirculando--;
        lanzarVehiculos();
    }

    public static void setTotalVehiculos(int cantidad){
        maxVehiculos=cantidad;
        lanzarVehiculos();
    }
}
