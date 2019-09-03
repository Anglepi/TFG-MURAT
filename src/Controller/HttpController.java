package Controller;

import Models.Constantes;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpController {
    /*
    Se autentica en el servidor.
     */
    private static String login(){
        HttpURLConnection conexion = null;
        String token = "";

        try{
            URL url = new URL(Constantes.URL+"usuario/autenticar");
            conexion = (HttpURLConnection) url.openConnection();
            conexion.setRequestMethod("POST");
            conexion.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conexion.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(conexion.getOutputStream());
            String parametros = "usuario=angel&clave=angel";

            wr.writeBytes(parametros);
            wr.flush();
            wr.close();

            BufferedReader in = new BufferedReader(new InputStreamReader(conexion.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while((inputLine = in.readLine()) != null){
                response.append(inputLine);
            }
            in.close();

            JsonObject json = Json.parse(response.toString()).asObject();
            token = json.get("token").asString();
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }

        return token;
    }
    /*
    Obtiene un listado concreto especificado por 'listado' indicando los parametros contenidos en 'parametros'
     */
    public static JsonArray obtenerListado(String listado, String parametros){
        HttpURLConnection conexion = null;
        String token = login();
        JsonArray json = new JsonArray();


        try{
            URL url = new URL(Constantes.URL+listado);
            conexion = (HttpURLConnection) url.openConnection();
            conexion.setRequestMethod("POST");
            conexion.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conexion.setRequestProperty("Authorization", "Bearer "+token);
            conexion.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(conexion.getOutputStream());

            wr.writeBytes(parametros);
            wr.flush();
            wr.close();

            BufferedReader in = new BufferedReader(new InputStreamReader(conexion.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while((inputLine = in.readLine()) != null){
                response.append(inputLine);
            }
            in.close();

            json = Json.parse(response.toString()).asObject().get("lista").asArray();

        } catch (Exception e){
            e.printStackTrace();
        }
        return json;

    }
}
