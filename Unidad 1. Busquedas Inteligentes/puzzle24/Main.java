package puzzle24;

public class Main {

    public static void main(String[] args) {

        String estadoI = "12 456379ABD8EFGHCJKLMINO";; 
        String estadoF = "123456789ABCDEFGHIJKLMNO ";

        SolucionPuzzle24 solucion = new SolucionPuzzle24(estadoI, estadoF);

        System.out.println("RESOLVIENDO PUZZLE 24 CON ESTADO INICIAL: " + estadoI);

        //Distancia de Manhattan
        System.out.println("\nHeurística: Distancia de Manhattan");
        solucion.busquedaIDA(1); //usamos la heurística de Manhattan

        //Distancia de Manhattan + Conflicto lineal
        System.out.println("\nHeurística: Distancia de Manhattan + Conflicto lineal");
        solucion.busquedaIDA(2); //usamos la heurística de Manhattan + conflicto lineal
    }
}
