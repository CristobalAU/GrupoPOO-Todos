package cl.alltogether;

public class Solicitud {
    public int id, familiarId;
    public Integer cuidadorId;
    public String tipo, descripcion, prioridad, estado, creada, actualizada;
    public Solicitud() {} // Gson
    public Solicitud(int id, int familiarId, Integer cuidadorId, String tipo, String descripcion, String prioridad, String fecha) {
        this.id=id; this.familiarId=familiarId; this.cuidadorId=cuidadorId;
        this.tipo=tipo; this.descripcion=descripcion; this.prioridad=prioridad;
        this.estado="Pendiente"; this.creada=fecha; this.actualizada=fecha;
    }
}
