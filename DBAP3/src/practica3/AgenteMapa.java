/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package practica3;

import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;
import gugelcar.exceptions.ExceptionBadParam;
import gugelcar.exceptions.ExceptionNonInitialized;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public class AgenteMapa extends SingleAgent{
    
    private final AgentID controlador_id; //ID del agente controlador del servidor
    private ArrayList<AgentID> vehiculos; // IDs de los vehiculos
    
    private String conversation_id; //ID de la conversación de la sesión actual
    
    //Atributos propios del Agente Mapa
    private Mapa mapa;
    private final String nameMap;
    private boolean objetivo_encontrado;
    private int iteracion;
    private int energy; // Batería global
    private final int tam_mapa = 111;
    private boolean turno_ocupado;
    private int numeroCancel;
    private ArrayList<AgentID> vehiculoEsperando;
    private Posicion goal_pos;
    
    //Comunicacion
    private final JSON jsonobj;
    
    /**
     * @param aid del AgenteMapa
     * @param nameMap el nombre de la mapa
     * @param controlador_id el id del servidor
     * @param aids vector de agentes, deben ser 4
     * Constructor
     * @author Javier Bejar Mendez, Emilien Giard, Dani, Jorge
     * @throws java.lang.Exception
     */
    public AgenteMapa(AgentID aid, String nameMap, AgentID controlador_id, 
            ArrayList<AgentID> aids) throws Exception{
        super(aid);
        this.goal_pos = new Posicion();
        this.vehiculos = aids;
        this.nameMap = nameMap;
        this.controlador_id = controlador_id;
        jsonobj = new JSON();
        this.actualizaDatosMapaImportado();
        this.turno_ocupado = false;
        this.numeroCancel = 0;
        this.vehiculoEsperando = new ArrayList<AgentID>();
        
    }
    
   
    /**
     * Metodo que se suscribe y recibe y guarda el id de conversación
     * @author Emilien Giard, Dani
     */
    private void subscribe(){
        String world = jsonobj.encodeWorld(nameMap);
        
        ACLMessage outbox = crearMensaje(getAid(), controlador_id, 
                ACLMessage.SUBSCRIBE, world, "", "");
        send(outbox);
        
        try {
            ACLMessage inbox = receiveACLMessage();
            if (inbox.getPerformative().equals("INFORM")) {
                conversation_id = inbox.getConversationId();
                System.out.println("\nRecibido conversation id "
                +inbox.getConversationId()+" de "+inbox.getSender().getLocalName());
            } else {
                System.out.println("No se ha podido suscribir. Error: "+
                   jsonobj.decodeError(inbox.getContent()));
            }
        } catch (InterruptedException ex) {
            System.out.println("InterruptedException en subscribe(). Error: " + ex.getMessage());
        }
    }
    /**
     * Envía el conversation ID, el mapa y si se ha encontrado el objetivo a los 4 vehículos
     * @author Dani
     */
    private void enviarEstadoInicial() {
        for (AgentID vehiculo : vehiculos)
            this.enviarMapa(vehiculo);
    }
    
    /**
     * Metodo que actualiza el mapa en funcion de las percepciones de un vehiculo
     * @param vision visión actual del vehículo
     * @param pos posicion en el mapa global del vehículo
     * @param obj_enc si se ha encontrado el objetivo
     * @author Emilien Giard, Jorge, Dani, Javier Bejar Mendez
     */
    private void updateMap(JSONArray vision, Posicion pos, boolean obj_enc) {
        try{
            if(!objetivo_encontrado){
                objetivo_encontrado = obj_enc;
            }
            
            int n = vision.length();
            Posicion nueva = new Posicion();
            for(int xy = 0; xy < n; ++xy){
                nueva.asign(mapa.getFromVector(pos, xy, n));
                if(nueva.getX() >= 0 && nueva.getY() >= 0){
                    mapa.set(nueva, vision.getInt(xy));
                }
            }
        
        } catch (ExceptionBadParam | ExceptionNonInitialized ex) {
            System.out.println("Excepción en updateMap(). Mensaje: "+ex.getMessage());
        }
    }
    
    /**
     * Metodo que actualiza el mapa en funcion de las percepciones de un vehiculo
     * @param vision visión actual del vehículo
     * @param pos posicion en el mapa global del vehículo
     * @param obj_enc si se ha encontrado el objetivo
     * @author Emilien Giard, Jorge, Dani
     */
    private void actualizarPosicionVehiculo(String m, Posicion pos) {
        try {
            // set la posicion del vehiculo a vacia
            mapa.set(pos, 0);
            switch (m){
                case "moveN":
                    // set la nueva posicion del vehiculo
                    pos.setY(pos.getY() - 1);
                    break;
                case "moveS":
                    // set la nueva posicion del vehiculo
                    pos.setY(pos.getY() + 1);
                    break;
                case "moveE":
                    // set la nueva posicion del vehiculo
                    pos.setX(pos.getX() + 1);
                    break;
                case "moveW":
                    // set la nueva posicion del vehiculo
                    pos.setX(pos.getX() - 1);
                    break;
                case "moveNE":
                    // set la nueva posicion del vehiculo
                    pos.setY(pos.getY() - 1);
                    pos.setX(pos.getX() + 1);
                    break;
                case "moveNW":
                    // set la nueva posicion del vehiculo
                    pos.setY(pos.getY() - 1);
                    pos.setX(pos.getX() - 1);
                    break;
                case "moveSE":
                    // set la nueva posicion del vehiculo
                    pos.setY(pos.getY() + 1);
                    pos.setX(pos.getX() + 1);
                    break;
                case "moveSW":
                    // set la nueva posicion del vehiculo
                    pos.setY(pos.getY() + 1);
                    pos.setX(pos.getX() - 1);
                    break;
            }
            // set la posicion del vehiculo
            mapa.set(pos, -4);
        } catch (ExceptionBadParam | ExceptionNonInitialized ex) {
            System.out.println("Excepción en updateMap(). Mensaje: "+ex.getMessage());
        }
    }

    /**
     * Metodo que envia el mapa global a un vehiculo (fase de barrido).
     * @param vehiculo AgentID del vehículo a enviar el mapa
     * @author Emilien, Jorge, Dani, Javier Bejar Mendez
     */
    private void enviarMapa(AgentID vehiculo) {
        ACLMessage outbox = crearMensaje(getAid(), vehiculo, ACLMessage.INFORM,
                jsonobj.encodeMapa(mapa, objetivo_encontrado, goal_pos),
                conversation_id, "");
        send(outbox);
    }
    
    /**
     * Metodo que envia si el vehiculo debe cierra la sesion o no.
     * @param vehiculo AgentID del vehículo a enviar el mapa
     * @author Emilien
     */
    private void enviarInformCancel(AgentID vehiculo) {
        ACLMessage outbox = crearMensaje(getAid(), vehiculo, ACLMessage.INFORM,
                jsonobj.encodeCierraSesion(numeroCancel == 4),
                conversation_id, "");
        send(outbox);
    }
    
    /**
     * Envía un mensaje para desloguearse del servidor. 
     * Recibe y guarda la traza de la sesión.
     * @author Javier Bejar Mendez, Dani
     */
    private void logout(){
        
        ACLMessage outbox = this.crearMensaje(getAid(), controlador_id,
                ACLMessage.CANCEL, "", "", "");
        send(outbox);
        try {
            ACLMessage respuesta_agree = receiveACLMessage();
            ACLMessage respuesta_inform = receiveACLMessage();
            System.out.println("Logout completado. Traza guardada en: "+
                    nameMap +".png");
            jsonobj.guardarTraza(respuesta_inform.getContent(), nameMap + ".png");

        } catch (InterruptedException ex) {
            System.out.println("InterruptedException en logOut()."
                + " Error: " + ex.getMessage());
        }
    }
    
    /**
     * Crea un mensaje con los parámetros especificados.
     * @param sender
     * @param receiver
     * @param performative
     * @param content
     * @param conv_id
     * @param in_reply_to
     * @return ACLMessage con los parámetros especificados
     * @author Dani
     */
    private ACLMessage crearMensaje(AgentID sender, AgentID receiver, int performative,
            String content, String conv_id, String in_reply_to){
        
        ACLMessage outbox = new ACLMessage(performative);
        outbox.setSender(sender);
        outbox.setReceiver(receiver);
        outbox.setContent(content);
        outbox.setConversationId(conv_id);
        outbox.setInReplyTo(in_reply_to);
        
        return outbox;
    }

    /**
     * Metodo que crea los vehiculos y recibe los messajes de los vehiculos
     * @author Emilien Giard, Javier Bejar Mendez, Dani, Jorge
     */
     @Override
    public void execute(){
        logout(); // Remove when agents will close the session
        subscribe();
        enviarEstadoInicial();

        // bucle principal: espera los mesajes de los otros agentes
        do {
            try {
                ACLMessage inbox = receiveACLMessage();
                String command = jsonobj.decodeCommandVehiculo(inbox.getContent());
                //System.out.println("\nRecibido command "
                    //+ command +" de "+inbox.getSender().getLocalName());
                switch (command) {
                    case "checked-in":
                        System.out.println("Recibida confirmación de que el vehiculo "
                                + inbox.getSender().getLocalName() +
                                " ha hecho el checkin.\n");
                        break;
                    case "update-map":
                        JSONArray vision = jsonobj.decodeVision(inbox.getContent());
                        Posicion pos_vehiculo = jsonobj.decodePos(inbox.getContent());
                        boolean obj_enc = jsonobj.decodeObjetivoEncontrado(inbox.getContent());
                        updateMap(vision, pos_vehiculo, obj_enc);
                        if(obj_enc){
                            goal_pos.asign(jsonobj.decodeGoalPos(inbox.getContent()));
                        }
                        try{
                        System.out.println("Agente "+inbox.getSender().getLocalName()+" :"
                            +"\nPos: ["+pos_vehiculo.getX()+","+pos_vehiculo.getY()+"]\nobj_enc: "+ obj_enc);
                        if(obj_enc)
                            System.out.println("Goal_Pos: ["+goal_pos.getX()+","+goal_pos.getY());
                        
                        }catch(Exception ex){
                            System.out.println("Error print update-map"+ex.getMessage());
                        }
                        if (turno_ocupado == false) {
                            //first time only
                            turno_ocupado = true;
                            enviarMapa(inbox.getSender());
                        } else {
                            // add the id of the vehicule at the end of the list
                            vehiculoEsperando.add(inbox.getSender());
                        }
                        //System.out.println("Mapa global actualizado. Se envia a "+inbox.getSender().getLocalName());
                        break;
                    case "export-map":
                        exportarMapa();
                        break;
                    case "cancel":
                        numeroCancel ++;
                        enviarInformCancel(inbox.getSender());
                        break;
                    case "end-move":
                        // actualizar la posicion del vehiculo despus su movimiento para evitar colisiones de vehículos
                        String movimiento = jsonobj.decodeMovimiento(inbox.getContent());
                        Posicion pos_vehiculo_movimiento = jsonobj.decodePos(inbox.getContent());
                        actualizarPosicionVehiculo(movimiento, pos_vehiculo_movimiento);
                        
                        // envia la mapa a el primero vehiculo esperando
                        enviarMapa(vehiculoEsperando.get(0));
                        vehiculoEsperando.remove(0);
                        break;
                    default:
                        ACLMessage outbox = crearMensaje(getAid(), inbox.getSender(), 
                            ACLMessage.NOT_UNDERSTOOD, "", "", "");
                        this.send(outbox);
                        System.out.println("Agente "+getName()+
                                " envia NOT UNDERSTOOD a vehiculo "
                                +inbox.getSender().getLocalName());
                        break;
                }
            } catch (InterruptedException ex) {
                System.out.println("Excepción al recibir mensaje en execute(). Mensaje: "+ex.getMessage());
            }
        } while(numeroCancel <= 4);
        
        
        System.out.println("Objetivo encontrado! o recibe 4 cancel de los vehiculos");
        //Guardamos los datos necesarios para las siguientes ejecuciones(mapa interno)
        exportarMapa();
        //Terminamos la sesión y realizamos las comunicaciones en caso de ser necesarias con el resto de agentes
        logout();
    }
    
     /**
     * Exporta el mapa en un archivo llamado "mapa.json"
     * @author Jorge
     */
    private void exportarMapa(){
        jsonobj.exportMapa(mapa, objetivo_encontrado, iteracion+1, nameMap, this.goal_pos);
    }
    
    /**
     * Importa el mapa desde un archivo llamado "mapa.json"
     * @author Jorge
     */
    private JSONObject importarMapa(){
        JSONObject obj = jsonobj.importMapa(nameMap);
        return obj;
    }
    
    /**
     * 
     * @author Jorge, Dani, Javier Bejar Mendez
     */
    private void actualizaDatosMapaImportado(){
        try {
            JSONObject obj = importarMapa();
            iteracion = obj.getInt("iteracion");
            if (iteracion == 0){
                mapa = new Mapa(tam_mapa);
                objetivo_encontrado = false;
            }
            else
            {
                mapa = new Mapa(1);
                int tam = obj.getInt("tamanio");
                mapa.setTam(tam);
                Integer[][] m = new Integer[tam][tam];
                JSONArray array = obj.getJSONArray("mapa");

                for (int fil = 0; fil < tam; fil++)
                    for (int col = 0; col < tam; col++)
                        m[fil][col] = (Integer)array.getInt(fil*tam+col);
                
                mapa.setMapa(m);
                
                objetivo_encontrado = obj.getBoolean("encontrado");
            
                if(objetivo_encontrado){
                    this.goal_pos.setX(obj.getInt("xgoal"));
                    this.goal_pos.setY(obj.getInt("ygoal"));
                }
            }
            
        } catch (ExceptionBadParam ex) {
            System.out.println("Excepcion en actualizaDatosMapaImportado: "+ex.getMessage());
        }
    }
    /**
     * set de turno_ocupado
     * @author Jorge
     */
    private void setTurnoOcupado(boolean t){
        this.turno_ocupado = t;
    }
    /**
     * get de turno_ocupado
     * @author Jorge
     */
    private boolean getTurnoOcupado(){
        return this.turno_ocupado;
    }
    /**
     * Metodo que envia el turno a un vehiculo (fase de barrido).
     * @param vehiculo AgentID del vehículo a enviar el mapa
     * @author Jorge
     */
    private void enviarTurno(AgentID vehiculo) {
        ACLMessage outbox = crearMensaje(getAid(), vehiculo, ACLMessage.INFORM,
                jsonobj.encodeTurno(turno_ocupado),
                conversation_id, "");
        if(!getTurnoOcupado()) setTurnoOcupado(true);
        send(outbox);
    }
    /**
     * Metodo que envia el turno a un vehiculo (fase de barrido).
     * @param vehiculo AgentID del vehículo a enviar el mapac
     * @author Jorge
     */
    private void recibirFinTurno() throws InterruptedException {
        ACLMessage inbox = receiveACLMessage();
        boolean turno = jsonobj.decodeTurno(inbox.getContent());
        this.setTurnoOcupado(turno);
    }
}
