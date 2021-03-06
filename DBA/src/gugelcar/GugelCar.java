/* Grupo C
*  Práctica 2 de DBA
*/
package gugelcar;

import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.AgentsConnection;
import es.upv.dsic.gti_ia.core.SingleAgent;
import java.util.ArrayList;

/**
 * @brief Clase con la funcionalidad del agente que simula un coche de google
 */

public class GugelCar extends SingleAgent {

private static final String HOST = "isg2.ugr.es";
private static final String USER = "Boyero";
private static final String PASSWORD = "Parra";
private String clave_acceso;
private final String mapa;
private static final int PORT = 6000;
private static final String VIRTUAL_HOST = "Cerastes";
private int bateria;
private int pos_x; //posicion en server
private int pos_y; //posicion en server
private ArrayList<Float> lectura_escaner;
private ArrayList<Integer> lectura_radar;
private Estados estado_actual;
private final JSON json;
private int pasos;
boolean obj = false;
/*
    Gestion del mapa interno (map)
    si en server estamos en la posicion (20,20)
    en map estaremos en la posicion (20+MARGIN_X, 20+MARGIN_Y)
    se han definido funciones privadas para el manejo de las coordenadas

*/
private static final int TAM_X = 2000; //mapa x
private static final int TAM_Y = 2000; //mapa y
private static final int MARGIN_X = 100; //margen del map x
private static final int MARGIN_Y = 100; //margen del map y
private int[][] map; //Para la versión 2
private int mpos_x; //posicion en map
private int mpos_y; //posicion en map

/*
    Gestion de la traza para detectar mapas sin solución
*/
private int[][] sol_trace;
/**
 * @author Javier Bejar Mendez
 * @brief Actualiza el vector sol_trace con los valores leidos por el radar, y 
 * actualiza los valores de las casillas visitadas
 */
private void SolTracer(){
    
    int size = 9;
    int[] auxpos;
    for(int i = 0; i < size; ++i){ 
        auxpos = this.vector_to_map_pos(i, size);
       if(sol_trace[auxpos[0]][auxpos[1]] == -1){
                sol_trace[auxpos[0]][auxpos[1]] = this.lectura_radar.get(i+(2*(3 +(i/3)))); 
        }
    }
    
    sol_trace[this.mpos_x][this.mpos_y] = 1;
}

/**
 * @author Javier Bejar Mendez
 * @brief transforma la posicion xy del vector a la posicion en map, se puede 
 * usar para obtener la poscion xy del radar o scaner, ejemplo: 
 *  int size = this.lectura_radar.size(); //el radar es un vector de tamaño 25, es una matriz 5x5
 *  int[] auxpos;
 *  auxpos = this.vector_to_map_pos(xy, size);
 *  auxpos[0] equivale a la posicion x en el mapa de la posicion xy del radar
 * @param xy posicion en el vector
 * @param size tamaño del vector que representa una matriz cuadrada;
 * @return int[] pos de 2 posiciones, pos[0] contiene la posicion x en map y pos[1] la posicion y en map
 */
private int[] vector_to_map_pos(int xy, int size){
    int tam = (int)Math.sqrt(size);
    int mid = tam/2;
    int[] pos = new int[2];
    pos[0] = this.mpos_x - mid + xy%tam;
    pos[1] = this.mpos_y - mid + xy/tam;
    
    return pos;
}
/**
 * @author Javier Bejar Mendez
 */
private int[] vector_to_map_pos(int xy, int size, int[] posp){
    int tam = (int)Math.sqrt(size);
    int mid = tam/2;
    int[] pos = new int[2];
    pos[0] = posp[0] - mid + xy%tam;
    pos[1] = posp[1] - mid + xy/tam;
    
    return pos;
}
/**
 * @author Javier Bejar Mendez
 */
private Integer[] vector_to_map_pos(int xy, int size, Integer[] posp){
    int tam = (int)Math.sqrt(size);
    int mid = tam/2;
    Integer[] pos = new Integer[2];
    pos[0] = posp[0] - mid + xy%tam;
    pos[1] = posp[1] - mid + xy/tam;
    
    return pos;
}

/**
 * @author Javier Bejar Mendez
 * @brief transforma la posicion xy del vector a la posicion en server
 * @param xy posicion en el vector
 * @param size tamaño del vector que representa una matriz cuadrada;
 * @return int[] pos de 2 posiciones, pos[0] contiene la posicion x en server y pos[1] la posicion y en server
 */
private int[] vector_to_server_pos(int xy, int size){
    int[] pos = new int[2];
    int tam = (int)Math.sqrt(size);
    int mid = tam/2;
    pos[0] = this.pos_x - mid + xy%tam;
    pos[1] = this.pos_y - mid + xy/tam;
    
    return pos;
}

/**
 * @author Javier Bejar Mendez
 * @brief transforma la posicion (x,y) del server a la posicion en map
 * @param x posicion x en server
 * @param y posicion y en server
 * @return int[] pos de 2 posiciones, pos[0] contiene la posicion x en map y pos[1] la posicion y en map
 */
private int[] server_to_map_pos(int x, int y){
    int[] pos = new int[2];
    pos[0] = x - MARGIN_X;
    pos[1] = y - MARGIN_Y;
    
    return pos;
}

/**
 * @author Javier Bejar Mendez
 * @brief transforma la posicion (x,y) del map a la posicion en server
 * @param x posicion x en map
 * @param y posicion y en map
 * @return int[] pos de 2 posiciones, pos[0] contiene la posicion x en server y pos[1] la posicion y en server
 */
private int[] map_to_server_pos(int x, int y){
    int[] pos = new int[2];
    pos[0] = x + MARGIN_X;
    pos[1] = y + MARGIN_Y;
    
    return pos;
}

/**
 * @author Javier Bejar Mendez
 * @brief actualiza la posicion actual del map en funcion de la posicion actual del server
 */
private void actualiza_mpos(){
    this.mpos_x = this.pos_x + MARGIN_X;
    this.mpos_y = this.pos_y + MARGIN_Y;
}

/**
 * @author Javier Bejar Mendez
 * @brief de vuelve el movimiento que hay que realizar para ir a pos desde la posicion actual en map
 *          ¡¡¡No en server ojo!!!
 * @param pos posicion en formato pos[2] a la que queremos desplazarnos
 * @return move, movimiento ha realizar para ir a pos
 */
private Movimientos pos_to_move(int[] pos){
    if(pos == null) System.out.println("Error posicion vacia en pos_to_move parametro");
    Movimientos move = null;
    
    String cardinal = "";
    
    
    System.out.println("current_pos:["+this.mpos_x+","+this.mpos_y+"]\npos_to_move:["+pos[0]+","+pos[1]+"]");
    //System.out.println("fpos:"+fpos+",cpos:"+cpos+",fmpos:"+fmpos+",cmpos:"+cmpos);
    //comprobamos que a donde queremos ir esta al norte o al sur
    //comprobamos que a donde queremos ir esta al oeste o al este
    if(pos[1] < this.mpos_y){
        cardinal += "n";
    }else if(pos[1] > this.mpos_y){
        cardinal += "s";
    }
    
    if(pos[0] < this.mpos_x){
        cardinal += "o";
    }else if(pos[0] > this.mpos_x){
        cardinal += "e";
    }
    
    
    System.out.println(cardinal);
    switch(cardinal){
        case "n":
            move=Movimientos.moveN;
            break;
        case "s"  :
            move=Movimientos.moveS;
            break;
        case "o":
            move=Movimientos.moveW;
            break;
        case "e":
            move=Movimientos.moveE;
            break;
        case "no":
            move=Movimientos.moveNW;
            break;
        case "ne":
            move=Movimientos.moveNE;
            break;
        case "so"  :
            move=Movimientos.moveSW;
            break;
        case "se"  :
            move=Movimientos.moveSE;
            break;
        default:
            System.out.println("Se ha recibido la misma posicion en pos_to_move()");
            break;
    }
    
    System.out.println(move.toString());
    return move;
}

/**
 * @author Javier Bejar Mendez
 */
private void imprimir_matriz(int longitud, Integer[] pos){
     for(int i = pos[0]-longitud; i < longitud+pos[0]; ++i){
        System.out.print("\nrow: "+ i+":");
        for(int j = pos[1]-longitud; j < longitud+pos[1]; ++j){
            System.out.print("col:"+j+",v:"+map[i][j]+" ");
        }
    }
}
/**
 * @author Javier Bejar Mendez
 */
private void imprimir_matriz(int longitud, int[] pos){
    for(int i = pos[0]-longitud; i < longitud+pos[0]; ++i){
        System.out.print("\nrow: "+ i+":");
        for(int j = pos[1]-longitud; j < longitud+pos[1]; ++j){
            System.out.print("col:"+j+",v:"+map[i][j]+" ");
        }
    }
}



    /**
     * @brief Constructor
     * @param aid ID del agente
     * @param mapa Mapa al que se conectará el agente
     * @throws java.lang.Exception
     * @autor <ul>
     *              <li>Jorge Echevarria Tello: cabecera</li>
     *              <li>Daniel Díaz Pareja: implementación </li>
     *         </ul>
     */
public GugelCar(AgentID aid, String mapa) throws Exception{
    super(aid);
    this.mapa = mapa;
    lectura_escaner = new ArrayList();
    json = new JSON();
    pasos = 0;
    map = new int[TAM_X][TAM_Y]; //Inicializo el mapa a 0, indicando las veces que se ha pasado por la posicion i j
    sol_trace= new int[TAM_X][TAM_Y];
    for (int i = 0; i < TAM_X; i++)
       for (int j = 0; j < TAM_Y; j++){
           map[i][j]=0;
           sol_trace[i][j]=-1;
       }
           
}

    /**
     * @brief Lo que hará el agente al crearse
     * @autor <ul>
     * 			<li>Emilien: implementación inicial </li>
     * 			<li>Daniel Díaz Pareja: implementación final </li>
     *         </ul>
     */
@Override
public void execute(){
    String gps;
    login();
    
    do {// Mientras no estemos en el objetivo, lo buscamos:
        
        // Recibimos los mensajes de los sensores
        //radar = recibirMensajeControlador();
        //scanner = recibirMensajeControlador();
        
        //battery = recibirMensajeControlador();
        
        // Recibimos y decodificamos los mensajes de los sensores
        this.lectura_radar = json.decodeRadar(recibirMensajeControlador());
        this.lectura_escaner = json.decodeScanner(recibirMensajeControlador());
        gps = recibirMensajeControlador();
        this.pos_x = json.decodeGPS(gps).x;
        this.pos_y = json.decodeGPS(gps).y;
        this.bateria = json.decodeBattery(recibirMensajeControlador());
        
        //Version usada en v2
        /*map[pos_x][pos_y] = map[pos_x][pos_y]+1; //Incremento en 1 indicando que se ha pasado una vez más por esa posición
        decidir_v2();
        */
        
        //Version v3
       decidir_v3();
        
        // Recibimos y decodificamos el estado
        this.estado_actual = json.decodeEstado(recibirMensajeControlador());
        pasos++; // Contador de los pasos que da el agente para resolver el mapa 
             //(por tener algo de feedback además de la traza
        
        
    } while (!estoyEnObjetivo());

    logout();
}

/**
     * @brief Crea la conexión con el servidor. Es static para poder utilizarla
     * sin tener que instanciar la clase.
     * @autor <ul>
     *              <li>Jorge: cabecera</li>
     *              <li>Daniel Díaz Pareja: implementación </li>
     *         </ul>
     */
public static void connect(){
    AgentsConnection.connect("isg2.ugr.es",PORT,VIRTUAL_HOST,USER,PASSWORD,false);
}
 /**
     * @brief Hace el login del agente en el mapa dado en el constructor. Este
     * agente tendrá todos los sensores (hemos decidido hacerlo así).
     * @autor <ul>
     *              <li>Jorge: cabecera</li>
     *              <li>Daniel Díaz Pareja :implementación </li>
     *         </ul>
     */
private void login(){
    String nombre = this.getAid().getLocalName();
    String mensaje = json.encodeLoginControlador(mapa, nombre, nombre, nombre, nombre);
    enviarMensajeControlador(mensaje);
    String respuesta = recibirMensajeControlador();
    if (respuesta.contains("trace"))
        respuesta = recibirMensajeControlador();
    
    clave_acceso = json.decodeClave(respuesta);
}

/**
 * @author Daniel Díaz Pareja
 * @brief Envía un mensaje al controlador
 * @param mensaje Mensaje a enviar al controlador
 */
private void enviarMensajeControlador(String mensaje){
    ACLMessage outbox = new ACLMessage();
    outbox.setSender(this.getAid());
    outbox.setReceiver(new AgentID(VIRTUAL_HOST));
    outbox.setContent(mensaje);
    this.send(outbox);
}

 /**
     * @brief Recibe un mensaje del controlador y lo imprime por pantalla.
     * @autor <ul>
     * 			<li>Jorge : cabecera</li>
     * 			<li>Daniel Díaz Pareja: implementación </li>
     *         </ul>
     * @return Mensaje del controlador
     */
private String recibirMensajeControlador(){
    String mensaje = "vacio";
    try {
        ACLMessage inbox=this.receiveACLMessage();
        mensaje=inbox.getContent();
        System.out.println("\nRecibido mensaje "
            +inbox.getContent()+" de "+inbox.getSender().getLocalName());
    } catch (InterruptedException ex) {
        System.out.println("Error al recibir mensaje");
    }
    return mensaje;
}
 /**
     * @brief Método para hacer logout del servidor.
     * @autor <ul>
     * 			<li>Jorge: cabecera</li>
     * 			<li>Daniel Díaz Pareja: implementación</li>
     *         </ul>
     */
private void logout(){
    String mensaje = json.encodeLogout(clave_acceso);
    enviarMensajeControlador(mensaje);
    
    // Cuando se hace el logout se quedan estos mensajes encolados, los recibimos
    // todos para poder conseguir el último mensaje: la traza
    recibirMensajeControlador(); // radar
    recibirMensajeControlador(); // scanner
    recibirMensajeControlador(); // gps
    recibirMensajeControlador(); // battery
    this.estado_actual = json.decodeEstado(recibirMensajeControlador());
    
    recibirMensajeControlador(); // radar
    recibirMensajeControlador(); // scanner
    recibirMensajeControlador(); // gps
    recibirMensajeControlador(); // battery
    json.guardarTraza(recibirMensajeControlador(), mapa+".png"); // se guarda
        // en la carpeta del proyecto netbeans
}


 /**
     * @brief Envía un mensaje al controlador para recargar la batería del agente.
     * @autor <ul>
     *              <li>Jorge: cabecera</li>
     *              <li>Daniel Díaz Pareja: implementación </li>
     *         </ul>
     */
private void refuel(){
    this.enviarMensajeControlador(json.encodeRefuel(clave_acceso));
}
 /**
     * @brief Actualiza el mapa con las lecturas del radar e incrementa las posiciones no visitadas
     * @autor <ul>
     * 			<li>Jorge : cabecera</li>
     *                  <li>Javier bejar : implementacion</li>
     *         </ul>
     */
private void actualizarMapa(){ //Recorremos toda la matriz incrementando cada posición del mapa que no sea obstaculo
    //Actualización de obstaculos y objetivo
    int size = this.lectura_radar.size();
    int[] auxpos;
    for(int i = 0; i < size; ++i){ //recorremos todo el vector donde esta el radar
        auxpos = this.vector_to_map_pos(i, size);
       if(map[auxpos[0]][auxpos[1]] != -1){ //Si nuestro mapa no es obstaculo (!=-1)
           if(this.lectura_radar.get(i)== 1){//i+(2*(3 +(i/3)))) == 1){ //Si el radar  es obstaculo (=1)
                map[auxpos[0]][auxpos[1]] = -1; //añadimos el obstaculo a nuestro mapa
           }
           else if(this.lectura_radar.get(i)== 2){//i+(2*(3 +(i/3)))) == 2){//Si el radar es el objetivo
               map[auxpos[0]][auxpos[1]] = -2; //añadimos el objetivo a nuestro mapa
           }
       }
    }
    //Ponemos la casilla donde estamos a 0, acabamos de visitarla
    map[this.mpos_x][this.mpos_y] = 0;
    //Incrementamos todas las casillas que no sean ni obstaculo ni objetivo
    for(int i = 0; i < TAM_X; ++i){
        for(int j = 0; j < TAM_Y; ++j){
            if(map[i][j] >= 0)
                ++map[i][j];
        }
    }
}

/**
     * @brief selecciona como movimiento la casilla de alrededor que mas tiempo lleve sin visitar
     * @autor <ul>
     *                  <li>Javier bejar: esqueleto</li>
     *         </ul>
     * @return movimiento, el movimiento seleccionado
     */
private Movimientos menos_reciente(){
    
    int mas_viejo = 0;
    int[] auxpos;
    int size = 9;
    int posmapvalue;
    int[] move_to = null;
    boolean no_goal = true;
    
    for(int i = 0; i < size && no_goal; ++i){
        auxpos = this.vector_to_map_pos(i, size);
        posmapvalue = map[auxpos[0]][auxpos[1]];
        //System.out.println("["+auxpos[0]+","+auxpos[1]+"]: "+posmapvalue+"   mas_viejo="+mas_viejo);
        if(posmapvalue == -2){
            //Tenemos el objetivo
            no_goal = false;
            move_to = auxpos;
        }
        else if(posmapvalue > 0){ //Es decir no es un obstaculo
            if(mas_viejo < posmapvalue){ //Esta casilla lleva mas tiempo sin visitarse
                mas_viejo = posmapvalue; //Actualizamos el valor
                move_to = auxpos; //Seleccionamos esta casilla como objetivo
            }
        }
    }
   
    Movimientos move = pos_to_move(move_to);
    return move;
}
/**
     * @brief decide en función de decidir_v2() y llama al metodo menos_reciente en caso de bucle
     * @autor <ul>
     *                  <li>Javier bejar: esqueleto</li>
     *         </ul>
     */
private void decidir_v3(){
    //System.out.println("decidiendo");
    //System.out.println("posicion serever:\nx: "+this.pos_x+"\ny: "+this.pos_y);
    this.actualiza_mpos();
    //System.out.println("posicion actualizada mapa: \nx: "+this.mpos_x+"\ny: "+this.mpos_y);
    
    
    this.actualizarMapa();
    this.SolTracer();
    
    Movimientos mover = null;
    int[] move_to = null;
    if(bateria == 1){
        mover = Movimientos.refuel;
        System.out.println("refuel-------------");
    }
    else{
        move_to = this.greedy_v3();
        if(move_to == null){
            System.out.println("Ya visitado==========================================================");
            if(this.no_solucion()){
                System.out.println("El mapa no tiene solución\nSaliendo");
                obj = true;
            }else{
                mover = this.menos_reciente();
            }
        }
        else{
            mover = this.pos_to_move(move_to);
        }
        
    }
    this.enviarMensajeControlador(json.encodeMove(mover,this.clave_acceso));
}
/**
     * @brief comprobar que no se pueden visitar casillas nuevas
     * @author Javier Bejar Mendez
     * @return true si el mapa no tiene solucion, false si no se sabe
     */ 
private boolean no_solucion(){//No funciona
    boolean no_sol = false;
    
    Integer[] sol = new Integer[2];
    
    for(int i = 0; i < TAM_X && !no_sol; ++i){
        for(int j = 0; j < TAM_Y && !no_sol; ++j){
            if(map[i][j] == -2){
                sol[0] = i;
                sol[1] = j;
            }
        }
    }
    
    no_sol = is_rodeada(sol);
  
    return no_sol;
}
/**
 * @author Javier Bejar Mendez
 */
private boolean is_rodeada(Integer[] sol){
    boolean rodeada = false;
    
    ArrayList<Integer[]> camino_pos = new ArrayList();
    ArrayList<String> camino_card = new ArrayList();
    
    Integer[] pos_init = new Integer[2];
    Integer[] pos_sig = new Integer[2];
    
    for(int i = sol[0]-1; i >= MARGIN_X && !rodeada; --i){
        
        if(map[i][sol[1]] == -1){
            
            camino_pos.clear();
            camino_card.clear();
        
            pos_init[0] = i;
            pos_init[1] = sol[1];
            
            camino_pos.add(pos_init);
            camino_card.add(card_pos(pos_init, sol));
            
            for(int j = 1; j < 9 && !rodeada; j+=2){
                
                pos_sig = this.vector_to_map_pos(j, 9, pos_init);
                
                if(!(pos_init[0] == pos_sig[0] && pos_init[1] == pos_sig[1]) &&
                  map[pos_sig[0]][pos_sig[1]] == -1){
                    
                    camino_pos.add(pos_sig);
                    camino_card.add(card_pos(pos_sig, sol));
                    
                    rodeada = contenido_camino(sol, camino_pos, camino_card);
                    
                    camino_pos.remove(1);
                    camino_card.remove(1);
                }
            }
        }
    }
    
    return rodeada;
}

/**
 * @author Javier Bejar Mendez
 */
private boolean contenido_camino(Integer[] sol, ArrayList<Integer[]> camino_pos,
                                ArrayList<String> camino_card){
    if(camino_pos.get(0)[0] == camino_pos.get(camino_pos.size()-1)[0] &&
            camino_pos.get(0)[1] == camino_pos.get(camino_pos.size()-1)[1]){
        System.out.println("SE HA ENCONTRADO UN CAMINO ===========><><=========<><>==========>>>>>>>");
        
        return camino_contiene_pos(camino_card);
    }
    else{
        Integer[] pos_sig = new Integer[2];
        
        for(int i = 1; i < 9; i+=2){
            pos_sig = this.vector_to_map_pos(i, 9, camino_pos.get(camino_pos.size()-1));
            
            if(!(pos_sig[0] == camino_pos.get(camino_pos.size()-1)[0] &&
                    pos_sig[1] == camino_pos.get(camino_pos.size()-1)[1]) &&
                        map[pos_sig[0]][pos_sig[1]] == -1){
                
                camino_pos.add(pos_sig);
                camino_card.add(card_pos(pos_sig, sol));
                
                if(contenido_camino(sol, camino_pos, camino_card)){
                    return true;
                }else{
                    camino_pos.remove(camino_pos.size()-1);
                    camino_card.remove(camino_card.size()-1);
                }
            }
        }
        return false;
    }
}
/**
 * @author Javier Bejar Mendez
 */
private boolean camino_contiene_pos(ArrayList<String> camino_card){
    boolean contiene = false;
    int size = camino_card.size();
    int c_r = 0;
    int c_l = 0;
    String card_r, card_l;
    card_r = camino_card.get(0);
    card_l = camino_card.get(size-1);
    for(int i = 1; i < size; ++i){
        
        if(camino_card.get(i) == next_card(card_r)){
            ++c_r;
        }else if(card_r != camino_card.get(i)){
            --c_r; 
        }
        card_r = camino_card.get(i);
        if(camino_card.get(size-1-i) == next_card(card_l)){
            ++c_l;
        }else if(card_l != camino_card.get(size-1-i)){
            --c_l; 
        }
        card_r = camino_card.get(i);
        card_l = camino_card.get(size-1-i);
    }
    return (c_r == 8 || c_l == 8);
}
/**
 * @author Javier Bejar Mendez
 */
private String next_card(String card){
    switch(card){
        case "n":
            card = "ne";
            break;
        case "s"  :
            card = "so";
            break;
        case "o":
           card = "no";
            break;
        case "e":
            card = "se";
            break;
        case "no":
            card = "n";
            break;
        case "ne":
            card = "e";
            break;
        case "so"  :
            card = "o";
            break;
        case "se"  :
            card = "s";
            break;
        default:
            System.out.println("Se ha recibido un cardinal incorrecto");
            break;
    }
    return card;
}
/**
 * @author Javier Bejar Mendez
 */
private String card_pos(Integer[] pos1, Integer[] pos2){
    String cardinal = "";
    if(pos1[1] < pos2[1]){
        cardinal += "n";
    }else if(pos1[1] > pos2[1]){
        cardinal += "s";
    }
    
    if(pos1[0] < pos2[0]){
        cardinal += "o";
    }else if(pos1[0] > pos2[0]){
        cardinal += "e";
    }
    
    
    return cardinal;
}
/**
     * @brief greedy para v3
     * @author Javier Bejar Mendez
     * @return 
     */ 
private int[] greedy_v3(){
    float mas_cercano = 999999;
    int[] auxpos;
    int size = 9;
    int posmapvalue;
    int[] move_to = null;
    boolean no_goal = true;
    
    for(int i = 0; i < size && no_goal; ++i){
        auxpos = this.vector_to_map_pos(i, size);
        posmapvalue = map[auxpos[0]][auxpos[1]];
        
        if(posmapvalue == -2){
            //Tenemos el objetivo
            no_goal = false;
            move_to = auxpos;
        }
        else if(posmapvalue > 0 && posmapvalue > pasos){ //Es decir no es un obstaculo y no la ha visitado
            if(mas_cercano > this.lectura_escaner.get(i+(2*(3 +(i/3))))){ //Esta casilla es mas prometedora
                mas_cercano = this.lectura_escaner.get(i+(2*(3 +(i/3)))); //Actualizamos el valor
                move_to = auxpos; //Seleccionamos esta casilla como objetivo
            }
        }
    }
   
    
    return move_to;
}
 /**
     * @brief El metodo decide que movimiento realiza, simplemente teniendo en 
     *  cuenta la menor distancia siempre y cuando no sea una pared
     * @autor <ul>
     * 			<li>jorge: cabecera</li>
     * 			<li>@donas11 y Jorge: implementación </li>
     * 			<li>Javier y Jorge :idea inicial</li>
     *         </ul>
     */
private void decidir(){
    Movimientos mover = null;
    
  if(bateria == 1){
      mover = Movimientos.refuel;
  }else{
      float menor = 9999;
      int movimiento=0;
      for (int i = 6; i < 9; i++) {
              if(!(lectura_radar.get(i).equals(1))){
                if (lectura_escaner.get(i) <= menor) {
                    menor = lectura_escaner.get(i);
                    movimiento = i;
              }
            }           
      }
      for (int i = 11; i < 14; i++) {
              if(!(lectura_radar.get(i).equals(1))){
                if (lectura_escaner.get(i) <= menor && i!=12) {
        
                    menor = lectura_escaner.get(i);
                    movimiento = i;

              }
            }           
      }
      for (int i = 16; i < 19; i++) {
              if(!(lectura_radar.get(i).equals(1))){
                if (lectura_escaner.get(i) <= menor) {
                    menor = lectura_escaner.get(i);
                    movimiento = i;

              }
            }           
      }
      
      switch(movimiento){
        case (8): mover = Movimientos.moveNE;
        break;
        case (7): mover = Movimientos.moveN;
        break;
        case (6): mover = Movimientos.moveNW;
        break;
        case (13): mover=Movimientos.moveE;
        break;
        case (12): System.out.println("\n\nMe quedo quieto");
        break;
        case (11): mover=Movimientos.moveW;
        break;
        case (18): mover= Movimientos.moveSE;
        break;
        case (17): mover=Movimientos.moveS;
        break;
        case (16): mover=Movimientos.moveSW;
        break;
      
   }
  }
  this.enviarMensajeControlador(json.encodeMove(mover,this.clave_acceso));

}

/**
     * @brief El metodo decide que movimiento realiza, teniendo en 
     *  cuenta la menor distancia siempre y cuando no sea una pared, y si ha pasado o no antes
     * @autor <ul>
     * 			<li>jorge: cabecera</li>
     * 			<li>Jorge: implementación </li>
     * 			<li>Javier y Jorge: idea inicial</li>
     *         </ul>
     */
private void decidir_v2(){
    Movimientos mover = null;
  if(bateria == 1){
      mover = Movimientos.refuel;
  }else{
      float menor = 9999;
      int movimiento=0;
      for (int i = 6; i < 9; i++) {
              if(!(lectura_radar.get(i).equals(1))){ //Si no hay una pared..
                if ((lectura_escaner.get(i) <= menor) && !he_pasado(i)) { //Si la posición tiene un valor menor de scanner y no he pasado
                    menor = lectura_escaner.get(i); //Guardo la menor distancia hasta el momento
                    movimiento = i; //Guardo el mejor movimiento hasta el momento
              }
            }           
      }
      for (int i = 11; i < 14; i++) {
              if(!(lectura_radar.get(i).equals(1)) && (i!=12)){//Si no hay una pared y no me quedo quieto
                if((lectura_escaner.get(i) <= menor) && !he_pasado(i)){//Si la posición tiene un valor menor de scanner y no he pasado
                    menor = lectura_escaner.get(i);//Guardo la menor distancia hasta el momento
                    movimiento = i;//Guardo el mejor movimiento hasta el momento
              }
            }
      }
      for (int i = 16; i < 19; i++) {
              if(!(lectura_radar.get(i).equals(1))){//Si no hay una pared..
                if ((lectura_escaner.get(i) <= menor) && !he_pasado(i)) {//Si la posición tiene un valor menor de scanner y no he pasado
                    menor = lectura_escaner.get(i);//Guardo la menor distancia hasta el momento
                    movimiento = i;//Guardo el mejor movimiento hasta el momento
              }
            }           
      }
        
    switch(movimiento){ //Transformo el int al movimiento equivalente
      case (8): mover = Movimientos.moveNE;
      break;
      case (7): mover = Movimientos.moveN;
      break;
      case (6): mover = Movimientos.moveNW;
      break;
      case (13): mover=Movimientos.moveE;
      break;
      case (12): System.out.println("\n\nMe quedo quieto");
      break;
      case (11): mover=Movimientos.moveW;
      break;
      case (18): mover= Movimientos.moveSE;
      break;
      case (17): mover=Movimientos.moveS;
      break;
      case (16): mover=Movimientos.moveSW;
      break;
     }
  }
    System.out.println("\n\nMe muevo a "+mover);

  this.enviarMensajeControlador(json.encodeMove(mover,this.clave_acceso));

}

 /**
     * @return True si ha pasado, false en caso contrario
     * @brief El metodo comprueba si ya ha pasado por ese camino
     * @param movimiento, Se trata del movimiento que queremos ver si podemos realizar
     * @autor <ul>
     * 			<li>jorge: cabecera</li>
     * 			<li>@jorge: implementación </li>
     *         </ul>
     */
private boolean he_pasado(int movimiento){
    boolean pasado = true;
    
    switch(movimiento){ //Dependiendo del movimiento que corresponda
            case (8): if((pos_x+1 > TAM_X )|| (pos_y-1 < 0)){ //Compruebo si existe esa posición del mapa
                        pasado = true;
                        }else if(map[pos_x+1][pos_y-1] == 0) pasado = false; //Si no he pasado..
            break;
            case (7):  if(pos_y-1 < 0 ){
                        pasado = true;
                        }else if(this.map[pos_x][pos_y-1] == 0) pasado = false;
            break;
            case (6):  if(pos_x-1 < 0 || pos_y-1 < 0){
                        pasado = true;
                        }else if(map[pos_x-1][pos_y-1] == 0) pasado = false;
            break;
            case (13):  if(pos_x+1 > TAM_X){
                        pasado = true;
                        }else if(map[pos_x+1][pos_y] == 0) pasado = false;
            break;
            case (11): if(pos_x-1 < 0){
                        pasado = true;
                        }else if(map[pos_x-1][pos_y] == 0)pasado = false;
            break;
            case (18):  if((pos_x+1 > TAM_X) || (pos_y+1 > TAM_Y)){
                        pasado = true;
                        }else if(map[pos_x+1][pos_y+1] == 0) pasado = false;
            break;
            case (17):  if(pos_y+1 > TAM_Y){
                        pasado = true;
                        }else if(map[pos_x][pos_y+1] == 0) pasado = false;
            break;
            case (16):  
                if((pos_x-1 < 0) || (pos_y+1 > TAM_Y)){
                        pasado = true;
                        }else if(map[pos_x-1][pos_y+1] == 0) pasado = false;
            break;
            case (12):  pasado = true;
            break;
       }
    return pasado; //Devuelvo un boolean indicando si he pasado o no (true/false)
}
 /**
     * @brief El metodo comprueba si estoy en el objetivo
     * @autor <ul>
     * 			<li>jorge: cabecera</li>
     * 			<li>@donas11: implementación </li>
     *         </ul>
     */
private boolean estoyEnObjetivo(){
   
    if(lectura_radar.get(12) == 2){
        obj=true;
        System.out.println("Estoy en objetivo. Pasos dados: "+pasos);
    }

    return obj;
}

}
