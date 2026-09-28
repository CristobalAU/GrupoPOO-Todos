package cl.alltogether;

public class Usuario {
    public int id;
    public String nombre, correo, claveHash, sal, rol, codigoVinculacion;
    public Integer cuidadorId;
    public Usuario() {} // Gson
    public Usuario(int id, String nombre, String correo, String hash, String sal, String rol, String codigo) {
        this.id=id; this.nombre=nombre; this.correo=correo; this.claveHash=hash;
        this.sal=sal; this.rol=rol; this.codigoVinculacion=codigo;
    }
    @Override public String toString() { return nombre+" ("+correo+")"; }
}
