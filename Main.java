package puzzle24;

public class Main {

    public static void main(String[] args) {

        String estadoI = "123456789ABC EFGHDJKLMINO"; 
        String estadoF = "123456789ABCDEFGHIJKLMNO ";

        SolucionPuzzle24 solucion = new SolucionPuzzle24(estadoI, estadoF);

        System.out.println("INICIANDO BUSQUEDA EN HEURÍSTICA");
        solucion.busquedaHeuristica();
    }
}
