public class Arbol {
    
    Nodo raiz;

    public Arbol(){
        this.raiz = null;
    }

    public boolean vacio(){
        if (raiz == null) return true;
        return false;
    }

    public Nodo buscarNodo(String nombre){
        return auxiliar(raiz, nombre);
    }

    private Nodo auxiliar(Nodo actual, String nombre){
        if(actual == null) return null;

        if(actual.nombre.equals(nombre)) return actual;

        Nodo izquierdo = auxiliar(actual.izq, nombre);
        Nodo derecho = auxiliar(actual.der, nombre);

        if(izquierdo != null) return izquierdo;
        
        else if(derecho!= null) return derecho;

        return null;
    }

    public void insertar(String nombre){
        if(vacio()) raiz = new Nodo(nombre);
        
        else insertarAux(raiz, nombre);
    }

    private void insertarAux(Nodo actual, String nombre){
        if(nombre.compareTo(actual.nombre) < 0) {
            if(actual.izq == null) actual.izq = new Nodo(nombre);
            else insertarAux(actual.izq, nombre);
        } else if(nombre.compareTo(actual.nombre) > 0) {
            if(actual.der == null) actual.der = new Nodo(nombre);
            else insertarAux(actual.der, nombre);
        }
        else System.out.println("Ese nodo ya existe");
    }

    public void imprimirArbol(){
        if(vacio()) System.out.println("El árbol está vacío");
        else imprimirAux(raiz);
    }

    private void imprimirAux(Nodo actual){
        if(actual != null){
            imprimirAux(actual.izq);
            System.out.println(actual.nombre);
            imprimirAux(actual.der);
        }
    }
}
