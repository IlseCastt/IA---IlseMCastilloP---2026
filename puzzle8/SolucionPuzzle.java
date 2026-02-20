package puzzle8;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

public class SolucionPuzzle {
    public String estadoI;
    public String estadoF;

    public SolucionPuzzle(String estadoI, String estadoF) {
        this.estadoI = estadoI;
        this.estadoF = estadoF;
    }

    public void busquedaAnchura() {
        Queue<Nodo> cola = new LinkedList<>();
        Set<String> visitados = new HashSet<>();

        //se crea el nodo raiz, el cual no tiene movimiento previo y su nivel es 0
        Nodo inicial = new Nodo(estadoI, null, "Inicio", 0);

        //se mete a la cola y se registra su estado como ya visitado, esto para no repetir movimientos y entrar en un ciclo sin fin
        cola.add(inicial);
        visitados.add(estadoI);

        //mientras la cola no este vacia, se sigue explorando nodos
        while(!cola.isEmpty()){
            Nodo nodoActual = cola.poll();

            if(nodoActual.estado.equals(estadoF)){
                //si se encuentra el estado final, se imprime la solucion
                imprimirSolucion(nodoActual);
                System.out.println("Solución encontrada en " + nodoActual.nivel + " movimientos:");
                return;
            } else { //si aun no se encuentra el estado final, se generan mas nodos
                List<Nodo> hijos = generarHijos(nodoActual);

                for(int i = 0; i < hijos.size(); i++){
                    if(!visitados.contains(hijos.get(i).estado)){ //con el hijos.get(i) se saca el nodo que esta en la posicion i
                        visitados.add(hijos.get(i).estado);  
                        cola.add(hijos.get(i));
                    }
                }
            }
        }

        System.out.println("No se encontró solución"); //esto si el while termina y nunca se hizo return
    }
    
    public void busquedaProfundidad() {
        Stack<Nodo> pila = new Stack<>();
        Set<String> visitados = new HashSet<>();

        Nodo inicial = new Nodo(estadoI, null, "Inicio", 0);

        pila.push(inicial);
        visitados.add(estadoI);

        //mientras la pila no este vacia, se sigue explorando nodos
        while(!pila.isEmpty()){
            Nodo nodoActual = pila.pop();

            if(nodoActual.estado.equals(estadoF)){
                //si se encuentra el estado final, se imprime la solucion
                imprimirSolucion(nodoActual);
                System.out.println("Solución encontrada en " + nodoActual.nivel + " movimientos:");
                return;
            } else { //si aun no se encuentra el estado final, se generan mas nodos
                List<Nodo> hijos = generarHijos(nodoActual);

                for(int i = 0; i < hijos.size(); i++){
                    if(!visitados.contains(hijos.get(i).estado)){ 
                        visitados.add(hijos.get(i).estado);  
                        pila.push(hijos.get(i));
                    }
                }
            }
        }

        System.out.println("No se encontró solución"); //esto si el while termina y nunca se hizo return

    }

    //heurística en forma de cruz (columna y fila central)
    private int calcularHeuristica(String estadoActual) {
        int costo = 0;

        int[] indicesCruz = {1, 3, 4, 5, 7}; //índices de la cruz central

        for (int i = 0; i < indicesCruz.length; i++) {
            if (estadoActual.charAt(indicesCruz[i]) != estadoF.charAt(indicesCruz[i])) {
                costo++;
            }
        }

        return costo;
    }

    public void busquedaHeuristica() {
        PriorityQueue<Nodo> colaPrioridad = new PriorityQueue<>(); //PriorityQueue los ordena automáticamente
        Set<String> visitados = new HashSet<>();

        int hInicial = calcularHeuristica(estadoI); //se calcula la heurística del estado inicial
        Nodo inicial = new Nodo(estadoI, null, "Inicio", 0, 0 + hInicial);

        colaPrioridad.add(inicial);
        visitados.add(estadoI);

        while (!colaPrioridad.isEmpty()) {
            Nodo nodoActual = colaPrioridad.poll(); //.poll() sacará automáticamente al nodo con el menor costoF

            if (nodoActual.estado.equals(estadoF)) {
                imprimirSolucion(nodoActual);
                System.out.println("Solución encontrada por Heurística en " + nodoActual.nivel + " movimientos:");
                return;
            } 
            
            List<Nodo> hijos = generarHijos(nodoActual);

            for(int i = 0; i < hijos.size(); i++){
                if(!visitados.contains(hijos.get(i).estado)){ 
                    visitados.add(hijos.get(i).estado);  

                    int g = hijos.get(i).nivel; // se calcula g(n) que es el nivel del nodo hijo
                    int h = calcularHeuristica(hijos.get(i).estado); // se calcula h(n) para el nodo hijo

                    hijos.get(i).costoF = g + h; // se asigna el costo total f(n) al nodo hijo

                    colaPrioridad.add(hijos.get(i)); // se agrega el nodo hijo a la cola de prioridad
                }
            }
        }
        
        System.out.println("Búsqueda Heurística terminada: Este tablero NO tiene solución");
    }

    public void busquedaCostoUniforme() {  
            PriorityQueue<Nodo> cola = new PriorityQueue<>();
            Set<String> visitados = new HashSet<>();

            Nodo inicial = new Nodo(estadoI, null, "Inicio", 0, 0);
            
            cola.add(inicial);
            visitados.add(estadoI);
            int nodosExplorados = 0;

            while (!cola.isEmpty()) {
                Nodo nodoActual = cola.poll();
                nodosExplorados++; 

                if (nodoActual.estado.equals(estadoF)) {
                    imprimirSolucion(nodoActual);
                    System.out.println("Solucion encontrada en " + nodoActual.nivel + " movimientos!");
                    System.out.println("Nodos explorados: " + nodosExplorados);
                    return;
                } else {
                    List<Nodo> hijos = generarHijos(nodoActual);

                    for (int i = 0; i < hijos.size(); i++) {
                        if (!visitados.contains(hijos.get(i).estado)) {
                            visitados.add(hijos.get(i).estado);
                            
                            hijos.get(i).costoF = hijos.get(i).nivel;
                            
                            cola.add(hijos.get(i));
                        }
                    }
                }
            }
            System.out.println("Este tablero NO tiene solución");
        }

    //metodo auxiliar para intercambiar posiciones en el estado
    private String intercambiarPos(String estado, int pos1, int pos2){
        char[] caracteres = estado.toCharArray(); //se convierte el estado a un arreglo de caracteres para facilitar el intercambio
        char temporal = caracteres[pos1];
        caracteres[pos1] = caracteres[pos2];
        caracteres[pos2] = temporal;
        return new String(caracteres);
    }

    private List<Nodo> generarHijos(Nodo nodoActual) {
        List<Nodo> hijos = new ArrayList<>();

        String estado = nodoActual.estado;
        int nivelAct = nodoActual.nivel;

        int posVacia = estado.indexOf(" ");

        if(posVacia >= 3){ //mover hacia arriba
            String estadoArriba = intercambiarPos(estado, posVacia, posVacia - 3);
            hijos.add(new Nodo(estadoArriba, nodoActual, "Arriba", nivelAct + 1));
        }

        if(posVacia <= 5){ //mover hacia abajo
            String estadoAbajo = intercambiarPos(estado, posVacia, posVacia + 3);
            hijos.add(new Nodo(estadoAbajo, nodoActual, "Abajo", nivelAct + 1));
        }
        
        if(posVacia % 3 != 0){ //mover a izq
            String estadoIzq = intercambiarPos(estado, posVacia, posVacia - 1);
            hijos.add(new Nodo(estadoIzq, nodoActual, "Izquierda", nivelAct + 1));
        }

        if(posVacia % 3 != 2){ //mover a der
            String estadoDer = intercambiarPos(estado, posVacia, posVacia + 1);
            hijos.add(new Nodo(estadoDer, nodoActual, "Derecha", nivelAct + 1));
        }

        return hijos;
    }

    private void imprimirSolucion(Nodo nodo) {
        List<Nodo> movimientos = new ArrayList<>();

        Nodo nodoAct = nodo;

        //recolectar los movimientos desde el nodo final hasta el nodo inicial, esto se hace retrocediendo por los nodos padres
        while(nodoAct != null){
            movimientos.add(nodoAct); //se agrega al inicio de la lista para mantener el orden correcto
            nodoAct = nodoAct.padre; //se retrocede al nodo padre para seguir construyendo la ruta
        }

        System.out.println("Pasos para resolver el puzzle8:");
        for (int i = movimientos.size() - 1; i >= 0; i--) {
            Nodo paso = movimientos.get(i);
            System.out.println("Movimiento: " + paso.movimiento);
            imprimirTablero(paso.estado);
            System.out.println("---------");
        }
    }

    // Método auxiliar para imprimir el estado como una matriz 3x3
    private void imprimirTablero(String estado) {
        for (int i = 0; i < 9; i++) {
            // Imprimimos el caracter actual entre corchetes
            System.out.print("[" + estado.charAt(i) + "] ");
            
            // Si ya imprimimos 3 caracteres (índices 2, 5, 8), damos un salto de línea
            if ((i + 1) % 3 == 0) {
                System.out.println();
            }
        }
    }
}
