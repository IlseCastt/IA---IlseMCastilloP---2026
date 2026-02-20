package puzzle8;

public class Nodo implements Comparable<Nodo> {
    public String estado;
    public Nodo padre;
    public String movimiento;
    int nivel;
    int costoF;

    public Nodo(String estado, Nodo padre, String movimiento, int nivel) {
        this.estado = estado;
        this.padre = padre;
        this.movimiento = movimiento;
        this.nivel = nivel;
        this.costoF = 0;
    }

    public Nodo(String estado, Nodo padre, String movimiento, int nivel, int costoF) {
        this.estado = estado;
        this.padre = padre;
        this.movimiento = movimiento;
        this.nivel = nivel;
        this.costoF = costoF;
    }

    //el menor costoF es el que va primero
    public int compareTo(Nodo otro) {
        return Integer.compare(this.costoF, otro.costoF);
    }
}
