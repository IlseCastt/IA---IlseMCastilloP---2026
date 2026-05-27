package puzzle24;

import java.util.ArrayList;
import java.util.List;

public class SolucionPuzzle24 {
    public String estadoI;
    public String estadoF;
    private long nodosExplorados; //contador de nodos explorados para ambas búsquedas

    public SolucionPuzzle24(String estadoI, String estadoF) {
        this.estadoI = estadoI;
        this.estadoF = estadoF;
    }

    //clase auxiliar para el resultado de IDA*
    private class ResultadoIDA{
        boolean encontrado;
        int nuevoUmbral;
        Nodo nodoMeta;

        ResultadoIDA(boolean encontrado, int nuevoUmbral, Nodo nodoMeta) {
            this.encontrado = encontrado;
            this.nuevoUmbral = nuevoUmbral;
            this.nodoMeta = nodoMeta;
        }
    }

    //busqueda con IDA* (Iterative Deepening A*)
    //tipo heuristica: 1 para Manhattan, 2 para Manhattan + conflicto lineal
    public void busquedaIDA(int tipoHeuristica) {
        nodosExplorados = 0;
        int hInicial = calcularHeuristica(estadoI, tipoHeuristica);
        int umbral = hInicial;

        Nodo inicial = new Nodo(estadoI, null, "Inicio", 0, hInicial);

        System.out.println("Iniciando busqueda IDA*");

        long tiempoInicio = System.currentTimeMillis();

        while(true){
            System.out.println("Explorando con umbral: " + umbral);

            //Llamada a la busqueda recursiva con el umbral actual  
            ResultadoIDA resultado = busquedaRecursiva(inicial, 0, umbral, tipoHeuristica);
            
            if(resultado.encontrado){
                long tiempoFin = System.currentTimeMillis();
                imprimirSolucion(resultado.nodoMeta);
                System.out.println("¡SOLUCIÓN ENCONTRADA EN " + resultado.nodoMeta.nivel + " MOVIMIENTOS!");
                System.out.println("Nodos explorados por IDA*: " + nodosExplorados);
                System.out.println("Tiempo de ejecución: " + (tiempoFin - tiempoInicio) + " ms");
                return;
            }
            if(resultado.nuevoUmbral == Integer.MAX_VALUE){ //si el nuevo umbral es infinito, significa que no hay más nodos por explorar y no se encontró solución
                System.out.println("Búsqueda terminada: Este tablero NO tiene solución.");
                return;
            }
            umbral = resultado.nuevoUmbral; //actualizamos el umbral para la siguiente iteración
        }
    }

    //Metodo recursivo para IDA*
    private ResultadoIDA busquedaRecursiva(Nodo nodoActual, int g, int umbral, int tipoHeuristica) {
        nodosExplorados++;  

        int f = g + calcularHeuristica(nodoActual.estado, tipoHeuristica); //calculamos f(n) = g(n) + h(n)

        //Si el costo f(n) es mayor que el umbral actual, retornamos el costo f(n) para actualizar el umbral en la siguiente iteración (podamos la rama)
        if(f > umbral){
            return new ResultadoIDA(false, f, null);
        }

        //Si encontramos la solución, retornamos un resultado indicando que se encontró la solución junto con el nodo meta
        if(nodoActual.estado.equals(estadoF)){
            return new ResultadoIDA(true, f, nodoActual);
        }

        int min = Integer.MAX_VALUE; //variable para almacenar el nuevo umbral mínimo encontrado en esta iteración
        List<Nodo> hijos = generarHijos(nodoActual); //generamos los hijos del nodo actual

        for(int i = 0; i < hijos.size(); i++){
            Nodo hijo = hijos.get(i); //obtenemos el nodo hijo actual

            if(nodoActual.padre != null && hijo.estado.equals(nodoActual.padre.estado)){
                continue; //evitamos volver al estado del padre
            }

            ResultadoIDA resultado = busquedaRecursiva(hijo, g + 1, umbral, tipoHeuristica); //llamada recursiva para el hijo actual

            if(resultado.encontrado){ //si se encontró la solución en esta rama, retornamos el resultado
                return resultado;
            }

            if(resultado.nuevoUmbral < min){ //actualizamos el nuevo umbral mínimo encontrado
                min = resultado.nuevoUmbral;
            }
        }

        return new ResultadoIDA(false, min, null); //retornamos el nuevo umbral mínimo encontrado para la siguiente iteración

    }

    //metodo para seleccionar la heuristica
    private int calcularHeuristica(String estadoActual, int tipoHeuristica) {

        if(tipoHeuristica == 1){
            return calcularManhattan(estadoActual);

        } else if(tipoHeuristica == 2){
            return calcularManhattan(estadoActual) + calcularConflictoLineal(estadoActual);

        } else {
            return 0; //si se ingresa un tipo de heuristica no válido, retornamos 0 para que no afecte el costo total
        }
    }

    //DISTANCIA DE MANHATTAN ADAPTADA AL 5x5
    private int calcularManhattan(String estadoActual) {
        int costoTotal = 0;

        for (int i = 0; i < 25; i++) {
            char pieza = estadoActual.charAt(i);
            
            if (pieza != ' ') {
                // Buscamos dónde debería estar esta pieza en la meta
                int indiceMeta = estadoF.indexOf(pieza);
                
                // Fórmulas matemáticas para sacar fila y columna en un grid 5x5
                int filaActual = i / 5;
                int colActual = i % 5;
                int filaMeta = indiceMeta / 5;
                int colMeta = indiceMeta % 5;
                
                // Sumamos la distancia vertical y la horizontal
                costoTotal += Math.abs(filaActual - filaMeta) + Math.abs(colActual - colMeta);
            }
        }
        return costoTotal;
    }

    //CONFLICTO LINEAL
    private int calcularConflictoLineal(String estadoActual){
        int conflictos = 0;

        //conflictos en filas
        for(int fila = 0; fila < 5; fila++){
            for(int col1 = 0; col1 < 4; col1++){
                for(int col2 = col1 + 1; col2 < 5; col2++){
                    char piezaA = estadoActual.charAt(fila * 5 + col1);
                    char piezaB = estadoActual.charAt(fila * 5 + col2);

                    if(piezaA != ' ' && piezaB != ' '){
                        int metaA = estadoF.indexOf(piezaA);
                        int metaB = estadoF.indexOf(piezaB);

                        if(metaA / 5 == fila && metaB / 5 == fila){
                            if(metaA> metaB){
                                conflictos += 2;
                            }
                        }
                    }
                }
            }
        }

        //conflictos en columnas
        for(int col = 0; col < 5; col++){
            for(int fila1 = 0; fila1 < 4; fila1++){
                for(int fila2 = fila1 + 1; fila2 < 5; fila2++){
                    char piezaA = estadoActual.charAt(fila1 * 5 + col);
                    char piezaB = estadoActual.charAt(fila2 * 5 + col); 

                    if(piezaA != ' ' && piezaB != ' '){
                        int metaA = estadoF.indexOf(piezaA);
                        int metaB = estadoF.indexOf(piezaB);

                        if(metaA % 5 == col && metaB % 5 == col){
                            if(metaA > metaB){
                                conflictos += 2;
                            }
                        }
                    }
                }
            }
        }

        return conflictos;
    }

    //metodo para generar los hijos de un nodo dado el estado actual del tablero
    private List<Nodo> generarHijos(Nodo nodoActual) {
        List<Nodo> hijos = new ArrayList<>();
        String estado = nodoActual.estado;
        int posVacia = estado.indexOf(" ");

        // Arriba (-5 posiciones)
        if (posVacia >= 5) {
            hijos.add(new Nodo(intercambiarPos(estado, posVacia, posVacia - 5), nodoActual, "Arriba", nodoActual.nivel + 1, 0));
        }
        // Abajo (+5 posiciones). El límite inferior es la fila que empieza en 20
        if (posVacia <= 19) {
            hijos.add(new Nodo(intercambiarPos(estado, posVacia, posVacia + 5), nodoActual, "Abajo", nodoActual.nivel + 1, 0));
        }
        // Izquierda (-1 posición). Múltiplos de 5 son la columna izquierda (0, 5, 10, 15, 20)
        if (posVacia % 5 != 0) {
            hijos.add(new Nodo(intercambiarPos(estado, posVacia, posVacia - 1), nodoActual, "Izquierda", nodoActual.nivel + 1, 0));
        }
        // Derecha (+1 posición). (4, 9, 14, 19, 24) son la columna derecha
        if (posVacia % 5 != 4) {
            hijos.add(new Nodo(intercambiarPos(estado, posVacia, posVacia + 1), nodoActual, "Derecha", nodoActual.nivel + 1, 0));
        }

        return hijos;
    }

    private String intercambiarPos(String estado, int pos1, int pos2) {
        char[] caracteres = estado.toCharArray();
        char temporal = caracteres[pos1];
        caracteres[pos1] = caracteres[pos2];
        caracteres[pos2] = temporal;
        return new String(caracteres);
    }

    private void imprimirSolucion(Nodo nodo) {
        List<Nodo> movimientos = new ArrayList<>();
        Nodo nodoAct = nodo;

        while (nodoAct != null) {
            movimientos.add(nodoAct);
            nodoAct = nodoAct.padre;
        }

        System.out.println("\nPasos para resolver el Puzzle 24:");
        for (int i = movimientos.size() - 1; i >= 0; i--) {
            Nodo paso = movimientos.get(i);
            System.out.println("Movimiento: " + paso.movimiento);
            imprimirTablero5x5(paso.estado);
            System.out.println("---------");
        }
    }

    private void imprimirTablero5x5(String estado) {
        for (int i = 0; i < 25; i++) {
            char c = estado.charAt(i);
            String valorAImprimir;

            if (c == ' ') {
                valorAImprimir = "  "; 
            } else if (c >= 'A' && c <= 'O') {
                int numeroReal = (c - 'A') + 10;
                valorAImprimir = String.valueOf(numeroReal);
            } else {
                valorAImprimir = " " + c; 
            }

            System.out.print("[" + valorAImprimir + "] ");
            if ((i + 1) % 5 == 0) {
                System.out.println();
            }
        }
    }
}
    
