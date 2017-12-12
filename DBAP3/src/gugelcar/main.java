    /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gugelcar;

import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.AgentsConnection;
/**
 *
 * @author Daniel
 */
public class main {

     /**
     * @param args the command line arguments
     * @author Emilien Giard
     */
    public static void main(String[] args) {
        //Poner datos correctos para la conexion!!!!!!!!!
        String nombreServidor = "Cerastes";
        AgentsConnection.connect("isg2.ugr.es",6000, nombreServidor, "Boyero", "Parra", false);
        try {
            AgenteMapa mapa = new AgenteMapa(
                    new AgentID("agenteMapa"),
                    "map1",
                    new AgentID(nombreServidor),
                    new AgentID("vehiculo1"),
                    new AgentID("vehiculo2"),
                    new AgentID("vehiculo3"),
                    new AgentID("vehiculo4")
            );
            mapa.run();
        } catch (Exception ex) {
            System.out.println("Error: "+ex.getMessage());
        }
    }
}
