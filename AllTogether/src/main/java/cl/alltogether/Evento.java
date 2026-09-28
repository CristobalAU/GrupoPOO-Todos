package cl.alltogether;

public class Evento {
    public int solicitudId, usuarioId;
    public String fecha, accion;
    public Evento() {} // Gson
    public Evento(int solicitudId,int usuarioId,String fecha,String accion) {
        this.solicitudId=solicitudId; this.usuarioId=usuarioId; this.fecha=fecha; this.accion=accion;
    }
}
