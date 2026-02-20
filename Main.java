package puzzle8;

public class Main {
    public static void main(String[] args) {
        String estadoI = "853624 17";
        String estadoF = "12345678 ";

        SolucionPuzzle solucion = new SolucionPuzzle(estadoI, estadoF);

        System.out.println("INICIANDO BUSQUEDA EN ANCHURA (BFS)");
        solucion.busquedaAnchura();

        //System.out.println("INICIANDO BUSQUEDA EN PROFUNDIDAD (DFS)");
        //solucion.busquedaProfundidad();

        //System.out.println("INICIANDO BÚSQUEDA HEURÍSTICA");
        //solucion.busquedaHeuristica();

        //System.out.println("INICIANDO BÚSQUEDA COSTO UNIFORME");
        //solucion.busquedaCostoUniforme();
    }
}
